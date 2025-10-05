package com.example.androidtictactoe

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*

class MainActivity : AppCompatActivity() {

    // --- MODO LOCAL ---
    private val gameViewModel: GameViewModel by viewModels()
    private var localGameMode: String? = null

    // --- MODO ONLINE ---
    private var onlineGameId: String? = null
    private lateinit var myPlayerId: String
    private lateinit var gameRef: DatabaseReference
    private var mySymbol = "X"
    private var gameEventListener: ValueEventListener? = null


    // --- VISTAS Y RECURSOS COMUNES ---
    private lateinit var boardView: BoardView
    private lateinit var textViewStatus: TextView
    private lateinit var textViewPlayerX: TextView
    private lateinit var textViewPlayerO: TextView
    private lateinit var btnReset: Button
    private lateinit var btnBackToMenu: Button
    private var playerXSoundPlayer: MediaPlayer? = null
    private var playerOSoundPlayer: MediaPlayer? = null
    private var cpuSoundPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var cpuMoveRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas comunes
        boardView = findViewById(R.id.boardView)
        textViewStatus = findViewById(R.id.textViewStatus)
        textViewPlayerX = findViewById(R.id.textViewPlayerX)
        textViewPlayerO = findViewById(R.id.textViewPlayerO)
        btnReset = findViewById(R.id.btnReset)
        btnBackToMenu = findViewById(R.id.btnBackToMenu)

        // Inicializar sonidos comunes
        playerXSoundPlayer = MediaPlayer.create(this, R.raw.sound_player1)
        playerOSoundPlayer = MediaPlayer.create(this, R.raw.sound_player2)
        cpuSoundPlayer = MediaPlayer.create(this, R.raw.sound_cpu)


        // --- DECIDIR EL MODO DE JUEGO ---
        onlineGameId = intent.getStringExtra("GAME_ID")

        if (onlineGameId != null) {
            setupOnlineGame() // Iniciar en modo ONLINE
        } else {
            setupLocalGame(savedInstanceState) // Iniciar en modo LOCAL
        }

        btnBackToMenu.setOnClickListener { finish() }
    }


    // ===================================================================
    // LÓGICA Y CONFIGURACIÓN PARA EL MODO LOCAL
    // ===================================================================
    private fun setupLocalGame(savedInstanceState: Bundle?) {
        localGameMode = intent.getStringExtra("GAME_MODE")
        val selectedDifficulty = intent.getStringExtra("DIFFICULTY")
        if (selectedDifficulty != null && selectedDifficulty != "NONE") {
            gameViewModel.difficulty = selectedDifficulty
        }

        boardView.onBoardTouchListener = { row, col ->
            val isHumanTurn = (localGameMode == "PVP") || (localGameMode == "PVC" && gameViewModel.playerXTurn)
            if (gameViewModel.isGameActive && gameViewModel.board[row][col].isEmpty() && isHumanTurn) {
                handleLocalMove(row, col)
            }
        }

        btnReset.setOnClickListener {
            gameViewModel.resetBoard()
            updateUILocal()
        }

        updateUILocal()

        if (savedInstanceState != null && localGameMode == "PVC" && !gameViewModel.playerXTurn && gameViewModel.isGameActive) {
            updateStatusTextLocal()
            updateUILocal()
            cpuMoveRunnable = Runnable { cpuMove() }
            handler.postDelayed(cpuMoveRunnable!!, 700)
        }
    }

    private fun updateUILocal() {
        boardView.setBoard(gameViewModel.board)
        textViewStatus.text = gameViewModel.statusText
        textViewPlayerX.text = "Jugador X: ${gameViewModel.playerXPoints}"
        val opponentName = if (localGameMode == "PVC") "CPU" else "Jugador O"
        textViewPlayerO.text = "$opponentName: ${gameViewModel.playerOPoints}"
    }

    private fun handleLocalMove(row: Int, col: Int) {
        if (!gameViewModel.isGameActive || gameViewModel.board[row][col].isNotEmpty()) return

        val playerSymbol = if (gameViewModel.playerXTurn) "X" else "O"
        gameViewModel.board[row][col] = playerSymbol
        gameViewModel.roundCount++

        playSoundForMove(playerSymbol)

        if (checkForWinLocal()) {
            playerWinsLocal(playerSymbol)
        } else if (gameViewModel.roundCount == 9) {
            drawLocal()
        } else {
            gameViewModel.playerXTurn = !gameViewModel.playerXTurn
            updateStatusTextLocal()
            if (localGameMode == "PVC" && !gameViewModel.playerXTurn && gameViewModel.isGameActive) {
                cpuMoveRunnable = Runnable { cpuMove() }
                handler.postDelayed(cpuMoveRunnable!!, 700)
            }
        }
        updateUILocal()
    }

    private fun cpuMove() {
        if (!gameViewModel.isGameActive) return
        val move = when (gameViewModel.difficulty) {
            "EASY" -> findEasyMove()
            "MEDIUM" -> findMediumMove()
            "HARD" -> findHardMove()
            else -> findEasyMove()
        }
        move?.let { (row, col) -> handleLocalMove(row, col) }
    }

    private fun findEasyMove(): Pair<Int, Int>? {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0..2) {
            for (j in 0..2) {
                if (gameViewModel.board[i][j].isEmpty()) emptyCells.add(Pair(i, j))
            }
        }
        return emptyCells.randomOrNull()
    }

    private fun findWinningMove(player: String): Pair<Int, Int>? {
        for (i in 0..2) {
            for (j in 0..2) {
                if (gameViewModel.board[i][j].isEmpty()) {
                    gameViewModel.board[i][j] = player
                    if (checkForWinLocal()) {
                        gameViewModel.board[i][j] = ""
                        return Pair(i, j)
                    }
                    gameViewModel.board[i][j] = ""
                }
            }
        }
        return null
    }

    private fun findMediumMove(): Pair<Int, Int>? {
        return findWinningMove("O") ?: findWinningMove("X") ?: findEasyMove()
    }

    private fun findHardMove(): Pair<Int, Int>? {
        return findWinningMove("O") ?: findWinningMove("X") ?: run {
            if (gameViewModel.board[1][1].isEmpty()) return Pair(1, 1)
            val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
            val emptyCorners = corners.filter { gameViewModel.board[it.first][it.second].isEmpty() }
            emptyCorners.randomOrNull() ?: findEasyMove()
        }
    }

    private fun updateStatusTextLocal() {
        gameViewModel.statusText = when {
            !gameViewModel.isGameActive -> gameViewModel.statusText
            localGameMode == "PVC" && !gameViewModel.playerXTurn -> "Turno de la CPU"
            gameViewModel.playerXTurn -> "Turno del Jugador X"
            else -> "Turno del Jugador O"
        }
    }

    private fun checkForWinLocal(): Boolean {
        val board = gameViewModel.board
        for (i in 0..2) {
            if (board[i][0].isNotEmpty() && board[i][0] == board[i][1] && board[i][0] == board[i][2]) return true
            if (board[0][i].isNotEmpty() && board[0][i] == board[1][i] && board[0][i] == board[2][i]) return true
        }
        if (board[1][1].isNotEmpty() && (board[0][0] == board[1][1] && board[0][0] == board[2][2] || board[0][2] == board[1][1] && board[0][2] == board[2][0])) return true
        return false
    }

    private fun playerWinsLocal(player: String) {
        gameViewModel.isGameActive = false
        gameViewModel.statusText = if (player == "X") "¡El jugador X ha ganado!" else if (localGameMode == "PVC") "¡La CPU ha ganado!" else "¡El jugador O ha ganado!"
        if (player == "X") gameViewModel.playerXPoints++ else gameViewModel.playerOPoints++
    }

    private fun drawLocal() {
        gameViewModel.isGameActive = false
        gameViewModel.statusText = "¡Empate!"
    }


    // ===================================================================
    // LÓGICA Y CONFIGURACIÓN PARA EL MODO ONLINE
    // ===================================================================
    private fun setupOnlineGame() {
        myPlayerId = getPlayerId()
        gameRef = Firebase.database("https://androidtictactoedadmun-default-rtdb.firebaseio.com/").getReference("games").child(onlineGameId!!)

        boardView.onBoardTouchListener = { row, col ->
            makeOnlineMove(row, col)
        }

        btnReset.setOnClickListener {
            gameRef.get().addOnSuccessListener { snapshot ->
                val game = snapshot.getValue(Game::class.java)
                // Solo el creador de la partida (Jugador 1) puede reiniciar el tablero
                if (game != null && game.player1Id == myPlayerId) {
                    val updates = mutableMapOf<String, Any?>()
                    updates["board"] = "---------"
                    updates["gameState"] = "IN_PROGRESS"
                    updates["winner"] = null
                    updates["currentTurn"] = game.player1Id
                    gameRef.updateChildren(updates)
                }
            }
        }
        addGameEventListener()
    }

    private fun addGameEventListener() {
        gameEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val game = snapshot.getValue(Game::class.java)
                if (game == null) {
                    finish(); return
                }
                mySymbol = if (game.player1Id == myPlayerId) "X" else "O"
                updateUIOnline(game)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        gameRef.addValueEventListener(gameEventListener!!)
    }

    // --- FUNCIÓN DE ACTUALIZACIÓN DE UI MODIFICADA ---
    private fun updateUIOnline(game: Game) {
        val boardArray = Array(3) { Array(3) { "" } }
        game.board?.let {
            for (i in it.indices) {
                boardArray[i / 3][i % 3] = if (it[i] == '-') "" else it[i].toString()
            }
        }
        boardView.setBoard(boardArray)

        // Muestra los puntajes
        val player1Tag = if (game.player1Id == myPlayerId) " (Tú)" else ""
        val player2Tag = if (game.player2Id == myPlayerId) " (Tú)" else ""
        textViewPlayerX.text = "Jugador X${player1Tag}: ${game.player1Score}"
        textViewPlayerO.text = "Jugador O${player2Tag}: ${game.player2Score}"

        when (game.gameState) {
            "WAITING" -> textViewStatus.text = "Esperando oponente..."
            "FINISHED" -> {
                textViewStatus.text = when {
                    game.winner == "TIE" -> "¡Empate!"
                    (game.winner == game.player1Id && mySymbol == "X") || (game.winner == game.player2Id && mySymbol == "O") -> "¡Has ganado!"
                    else -> "Has perdido"
                }
            }
            "IN_PROGRESS" -> {
                textViewStatus.text = if (game.currentTurn == myPlayerId) "¡Es tu turno!" else "Turno del oponente"
            }
        }
    }

    // --- FUNCIÓN DE MOVIMIENTO MODIFICADA ---
    private fun makeOnlineMove(row: Int, col: Int) {
        gameRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val game = snapshot.getValue(Game::class.java)
                if (game != null && game.gameState == "IN_PROGRESS" && game.currentTurn == myPlayerId) {
                    game.board?.let { currentBoard ->
                        val index = row * 3 + col
                        if (currentBoard.getOrNull(index) == '-') {
                            val newBoardList = currentBoard.toMutableList()
                            newBoardList[index] = mySymbol[0]
                            val newBoardString = newBoardList.joinToString("")

                            val updates = mutableMapOf<String, Any?>()
                            updates["board"] = newBoardString

                            // Comprobar si el juego ha terminado
                            val result = checkForWinOnline(newBoardString)
                            if (result != null) {
                                updates["gameState"] = "FINISHED"
                                if (result == "X") {
                                    updates["winner"] = game.player1Id
                                    updates["player1Score"] = game.player1Score + 1
                                } else if (result == "O") {
                                    updates["winner"] = game.player2Id
                                    updates["player2Score"] = game.player2Score + 1
                                } else { // Empate
                                    updates["winner"] = "TIE"
                                }
                            } else {
                                // El juego continúa, pasa el turno
                                updates["currentTurn"] = if (myPlayerId == game.player1Id) game.player2Id else game.player1Id
                            }

                            gameRef.updateChildren(updates)
                            playSoundForMove(mySymbol)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- NUEVA FUNCIÓN PARA DETECTAR VICTORIA ONLINE ---
    private fun checkForWinOnline(board: String): String? {
        val lines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // Filas
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // Columnas
            listOf(0, 4, 8), listOf(2, 4, 6)  // Diagonales
        )
        for (line in lines) {
            val (a, b, c) = line
            if (board[a] != '-' && board[a] == board[b] && board[a] == board[c]) {
                return board[a].toString() // Retorna "X" o "O"
            }
        }
        if (!board.contains('-')) {
            return "TIE" // Empate
        }
        return null // El juego continúa
    }


    // ===================================================================
    // FUNCIONES COMUNES Y CICLO DE VIDA
    // ===================================================================
    private fun getPlayerId(): String {
        val prefs = getSharedPreferences("TicTacToePrefs", MODE_PRIVATE)
        return prefs.getString("playerId", "") ?: ""
    }

    private fun playSoundForMove(playerSymbol: String) {
        val isCpuTurn = localGameMode == "PVC" && !gameViewModel.playerXTurn
        val soundToPlay = when {
            playerSymbol == "X" -> playerXSoundPlayer
            playerSymbol == "O" && isCpuTurn -> cpuSoundPlayer
            else -> playerOSoundPlayer
        }
        handler.postDelayed({ soundToPlay?.start() }, 100)
    }

    override fun onStop() {
        super.onStop()
        if (onlineGameId == null) { // Solo guarda estado si es un juego local
            gameViewModel.saveState()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar listener de Firebase si estábamos en modo online
        gameEventListener?.let { gameRef.removeEventListener(it) }

        // Limpiar handler y sonidos
        cpuMoveRunnable?.let { handler.removeCallbacks(it) }
        playerXSoundPlayer?.release()
        playerOSoundPlayer?.release()
        cpuSoundPlayer?.release()
        playerXSoundPlayer = null
        playerOSoundPlayer = null
        cpuSoundPlayer = null
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Acerca de Tic-Tac-Toe")
            .setMessage("Programado por: David Rátiva\nVersión: 1.0")
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}