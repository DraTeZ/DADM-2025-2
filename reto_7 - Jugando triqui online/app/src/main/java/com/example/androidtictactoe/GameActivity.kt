package com.example.androidtictactoe
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.media.MediaPlayer
import android.os.Bundle


class GameActivity : AppCompatActivity() {

    // El ViewModel que guarda y gestiona todo el estado del juego
    private val gameViewModel: GameViewModel by viewModels()

    // Referencias a las vistas de la UI
    private lateinit var boardView: BoardView
    private lateinit var textViewStatus: TextView
    private lateinit var textViewPlayerX: TextView
    private lateinit var textViewPlayerO: TextView
    private lateinit var btnReset: Button
    private lateinit var btnBackToMenu: Button

    // Reproductores de sonido
    private var playerXSoundPlayer: MediaPlayer? = null
    private var playerOSoundPlayer: MediaPlayer? = null
    private var cpuSoundPlayer: MediaPlayer? = null

    // Variables de configuración del juego
    private var gameMode: String? = null
    private var difficulty: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtener configuración del Intent
        gameMode = intent.getStringExtra("GAME_MODE")
        difficulty = intent.getStringExtra("DIFFICULTY")

        // Inicializar vistas
        boardView = findViewById(R.id.boardView)
        textViewStatus = findViewById(R.id.textViewStatus)
        textViewPlayerX = findViewById(R.id.textViewPlayerX)
        textViewPlayerO = findViewById(R.id.textViewPlayerO)
        btnReset = findViewById(R.id.btnReset)
        btnBackToMenu = findViewById(R.id.btnBackToMenu)

        val selectedDifficulty = intent.getStringExtra("DIFFICULTY")
        if (selectedDifficulty != null && selectedDifficulty != "NONE") {
            gameViewModel.difficulty = selectedDifficulty
        }

        // Inicializar sonidos
        playerXSoundPlayer = MediaPlayer.create(this, R.raw.sound_player1)
        playerOSoundPlayer = MediaPlayer.create(this, R.raw.sound_player2)
        cpuSoundPlayer = MediaPlayer.create(this, R.raw.sound_cpu)

        // Configurar listeners
        boardView.onBoardTouchListener = { row, col ->
            if (gameViewModel.isGameActive && gameViewModel.board[row][col].isEmpty()) {
                handlePlayerMove(row, col)
            }
        }

        btnReset.setOnClickListener {
            gameViewModel.resetBoard()
            updateUIFromViewModel()
        }

        btnBackToMenu.setOnClickListener {
            finish()
        }

        // Actualizar la UI con el estado actual del ViewModel
        updateUIFromViewModel()
    }

    private fun updateUIFromViewModel() {
        boardView.setBoard(gameViewModel.board)
        textViewStatus.text = gameViewModel.statusText
        textViewPlayerX.text = "Jugador X: ${gameViewModel.playerXPoints}"
        val opponentName = if (gameMode == "PVC") "CPU" else "Jugador O"
        textViewPlayerO.text = "$opponentName: ${gameViewModel.playerOPoints}"
    }

    private fun handlePlayerMove(row: Int, col: Int) {
        // La validación del turno del jugador ya se hizo en el listener.
        // Esta función ahora solo procesa la jugada.
        if (!gameViewModel.isGameActive || gameViewModel.board[row][col].isNotEmpty()) return

        val playerSymbol = if (gameViewModel.playerXTurn) "X" else "O"
        gameViewModel.board[row][col] = playerSymbol
        gameViewModel.roundCount++

        playSoundForMove(playerSymbol)

        if (checkForWin()) {
            playerWins(playerSymbol)
        } else if (gameViewModel.roundCount == 9) {
            draw()
        } else {
            gameViewModel.playerXTurn = !gameViewModel.playerXTurn
            updateStatusText()
            if (gameMode == "PVC" && !gameViewModel.playerXTurn && gameViewModel.isGameActive) {
                Handler(Looper.getMainLooper()).postDelayed({ cpuMove() }, 700)
            }
        }
        updateUIFromViewModel()
    }

    private fun cpuMove() {
        if (!gameViewModel.isGameActive) return

        // Lee la dificultad directamente del ViewModel
        val move = when (gameViewModel.difficulty) {
            "EASY" -> findEasyMove()
            "MEDIUM" -> findMediumMove()
            "HARD" -> findHardMove()
            else -> findEasyMove()
        }

        move?.let { (row, col) ->
            handlePlayerMove(row, col)
        }
    }


    private fun findEasyMove(): Pair<Int, Int>? {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0..2) {
            for (j in 0..2) {
                if (gameViewModel.board[i][j].isEmpty()) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }
        return emptyCells.randomOrNull()
    }

    private fun findWinningMove(player: String): Pair<Int, Int>? {
        for (i in 0..2) {
            for (j in 0..2) {
                if (gameViewModel.board[i][j].isEmpty()) {
                    gameViewModel.board[i][j] = player // Simular
                    if (checkForWin()) {
                        gameViewModel.board[i][j] = "" // Deshacer
                        return Pair(i, j)
                    }
                    gameViewModel.board[i][j] = "" // Deshacer
                }
            }
        }
        return null
    }

    private fun findMediumMove(): Pair<Int, Int>? {
        var move = findWinningMove("O") // Ganar
        if (move != null) return move
        move = findWinningMove("X") // Bloquear
        if (move != null) return move
        return findEasyMove() // Aleatorio
    }

    private fun findHardMove(): Pair<Int, Int>? {
        var move = findWinningMove("O") // Ganar
        if (move != null) return move
        move = findWinningMove("X") // Bloquear
        if (move != null) return move
        if (gameViewModel.board[1][1].isEmpty()) return Pair(1, 1) // Centro
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        val emptyCorners = corners.filter { gameViewModel.board[it.first][it.second].isEmpty() }
        if (emptyCorners.isNotEmpty()) return emptyCorners.random() // Esquina
        return findEasyMove() // Lado
    }


    private fun updateStatusText() {
        gameViewModel.statusText = when {
            !gameViewModel.isGameActive -> gameViewModel.statusText // Mantiene mensaje de victoria/empate
            gameMode == "PVC" && !gameViewModel.playerXTurn -> "Turno de la CPU"
            gameViewModel.playerXTurn -> "Turno del Jugador X"
            else -> "Turno del Jugador O"
        }
    }

    private fun checkForWin(): Boolean {
        val board = gameViewModel.board
        for (i in 0..2) { // Filas
            if (board[i][0] == board[i][1] && board[i][0] == board[i][2] && board[i][0].isNotEmpty()) return true
        }
        for (i in 0..2) { // Columnas
            if (board[0][i] == board[1][i] && board[0][i] == board[2][i] && board[0][i].isNotEmpty()) return true
        }
        // Diagonales
        if (board[0][0] == board[1][1] && board[0][0] == board[2][2] && board[0][0].isNotEmpty()) return true
        if (board[0][2] == board[1][1] && board[0][2] == board[2][0] && board[0][2].isNotEmpty()) return true
        return false
    }

    private fun playerWins(player: String) {
        gameViewModel.isGameActive = false
        gameViewModel.statusText = if (player == "X") "¡El jugador X ha ganado!" else if (gameMode == "PVC") "¡La CPU ha ganado!" else "¡El jugador O ha ganado!"
        if (player == "X") gameViewModel.playerXPoints++ else gameViewModel.playerOPoints++
    }

    private fun draw() {
        gameViewModel.isGameActive = false
        gameViewModel.statusText = "¡Empate!"
    }

    // --- Sonido y Ciclo de Vida ---

    private fun playSoundForMove(playerSymbol: String) {
        val isCpuTurn = gameMode == "PVC" && !gameViewModel.playerXTurn
        val soundToPlay = when {
            playerSymbol == "X" -> playerXSoundPlayer
            playerSymbol == "O" && isCpuTurn -> cpuSoundPlayer
            playerSymbol == "O" && !isCpuTurn -> playerOSoundPlayer
            else -> null
        }
        Handler(Looper.getMainLooper()).postDelayed({ soundToPlay?.start() }, 100)
    }

    override fun onDestroy() {
        super.onDestroy()
        playerXSoundPlayer?.release()
        playerOSoundPlayer?.release()
        cpuSoundPlayer?.release()
        playerXSoundPlayer = null
        playerOSoundPlayer = null
        cpuSoundPlayer = null
    }

    override fun onStop() {
        super.onStop()
        gameViewModel.saveState()
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
            .setMessage("Programado por: David\nVersión: 1.0")
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}