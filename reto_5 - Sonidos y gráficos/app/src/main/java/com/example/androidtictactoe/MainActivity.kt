package com.example.androidtictactoe

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var boardView: BoardView
    private lateinit var textViewStatus: TextView
    private lateinit var textViewPlayerX: TextView
    private lateinit var textViewPlayerO: TextView
    private lateinit var btnReset: Button
    private lateinit var btnBackToMenu: Button

    private var playerXTurn = true
    private var roundCount = 0
    private var playerXPoints = 0
    private var playerOPoints = 0

    private val board = Array(3) { Array(3) { "" } }
    private var gameMode: String? = null
    private var difficulty: String? = null
    private var isGameActive = true

    private var playerXSoundPlayer: MediaPlayer? = null
    private var playerOSoundPlayer: MediaPlayer? = null
    private var cpuSoundPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        boardView = findViewById(R.id.boardView)
        textViewStatus = findViewById(R.id.textViewStatus)
        textViewPlayerX = findViewById(R.id.textViewPlayerX)
        textViewPlayerO = findViewById(R.id.textViewPlayerO)
        btnReset = findViewById(R.id.btnReset)
        btnBackToMenu = findViewById(R.id.btnBackToMenu)

        gameMode = intent.getStringExtra("GAME_MODE")
        difficulty = intent.getStringExtra("DIFFICULTY")

        playerXSoundPlayer = MediaPlayer.create(this, R.raw.sound_player1)
        playerOSoundPlayer = MediaPlayer.create(this, R.raw.sound_player2)
        cpuSoundPlayer = MediaPlayer.create(this, R.raw.sound_cpu)

        boardView.onBoardTouchListener = { row, col ->
            if (isGameActive && board[row][col].isEmpty()) {
                handlePlayerMove(row, col)
            }
        }

        btnReset.setOnClickListener {
            resetBoard()
        }

        btnBackToMenu.setOnClickListener {
            finish()
        }

        if (gameMode == "PVC") {
            textViewPlayerO.text = "CPU: 0"
        }

        updateScore()
        resetBoard()
    }

    private fun handlePlayerMove(row: Int, col: Int) {
        if (!isGameActive || board[row][col].isNotEmpty()) return

        val playerSymbol = if (playerXTurn) "X" else "O"
        board[row][col] = playerSymbol
        boardView.setBoard(board)
        playSoundForMove(playerSymbol)

        roundCount++

        if (checkForWin()) {
            playerWins(playerSymbol)
        } else if (roundCount == 9) {
            draw()
        } else {
            playerXTurn = !playerXTurn
            updateStatusText()
            if (gameMode == "PVC" && !playerXTurn && isGameActive) {
                Handler(Looper.getMainLooper()).postDelayed({ cpuMove() }, 700)
            }
        }
    }
    // --- 4. NUEVA FUNCIÓN INTELIGENTE PARA REPRODUCIR SONIDOS ---
    private fun playSoundForMove(playerSymbol: String) {
        val soundToPlay = when (playerSymbol) {
            "X" -> playerXSoundPlayer
            "O" -> {
                if (gameMode == "PVC" && !playerXTurn) { // Es el turno de la CPU
                    cpuSoundPlayer
                } else { // Es el Jugador O humano
                    playerOSoundPlayer
                }
            }
            else -> null
        }

        // Pequeño delay para evitar problemas de sonido
        Handler(Looper.getMainLooper()).postDelayed({
            soundToPlay?.start()
        }, 100)
    }

    override fun onDestroy() {
        super.onDestroy()
        // --- 5. LIBERAMOS LOS TRES REPRODUCTORES ---
        playerXSoundPlayer?.release()
        playerOSoundPlayer?.release()
        cpuSoundPlayer?.release()
        playerXSoundPlayer = null
        playerOSoundPlayer = null
        cpuSoundPlayer = null
    }

    private fun cpuMove() {
        if (!isGameActive) return

        val move = when (difficulty) {
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
                if (board[i][j].isEmpty()) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }
        return emptyCells.randomOrNull()
    }

    private fun findWinningMove(player: String): Pair<Int, Int>? {
        for (i in 0..2) {
            for (j in 0..2) {
                if (board[i][j].isEmpty()) {
                    board[i][j] = player // Simular
                    if (checkForWin()) {
                        board[i][j] = "" // Deshacer
                        return Pair(i, j)
                    }
                    board[i][j] = "" // Deshacer
                }
            }
        }
        return null
    }

    private fun findMediumMove(): Pair<Int, Int>? {
        // 1. Ganar si es posible
        var move = findWinningMove("O")
        if (move != null) return move
        // 2. Bloquear si es necesario
        move = findWinningMove("X")
        if (move != null) return move
        // 3. Movimiento aleatorio
        return findEasyMove()
    }

    private fun findHardMove(): Pair<Int, Int>? {
        // 1. Ganar si es posible
        var move = findWinningMove("O")
        if (move != null) return move
        // 2. Bloquear si es necesario
        move = findWinningMove("X")
        if (move != null) return move
        // 3. Tomar el centro
        if (board[1][1].isEmpty()) return Pair(1, 1)
        // 4. Tomar una esquina vacía
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        val emptyCorners = corners.filter { board[it.first][it.second].isEmpty() }
        if (emptyCorners.isNotEmpty()) return emptyCorners.random()
        // 5. Tomar cualquier lado
        return findEasyMove()
    }


    private fun updateStatusText() {
        textViewStatus.text = when {
            !isGameActive -> textViewStatus.text
            gameMode == "PVC" && !playerXTurn -> "Turno de la CPU"
            playerXTurn -> "Turno del Jugador X"
            else -> "Turno del Jugador O"
        }
    }

    private fun checkForWin(): Boolean {
        // Filas
        for (i in 0..2) {
            if (board[i][0] == board[i][1] && board[i][0] == board[i][2] && board[i][0].isNotEmpty()) return true
        }
        // Columnas
        for (i in 0..2) {
            if (board[0][i] == board[1][i] && board[0][i] == board[2][i] && board[0][i].isNotEmpty()) return true
        }
        // Diagonales
        if (board[0][0] == board[1][1] && board[0][0] == board[2][2] && board[0][0].isNotEmpty()) return true
        if (board[0][2] == board[1][1] && board[0][2] == board[2][0] && board[0][2].isNotEmpty()) return true

        return false
    }

    private fun playerWins(player: String) {
        isGameActive = false
        val winnerText = if (player == "X") "¡El jugador X ha ganado!" else if (gameMode == "PVC") "¡La CPU ha ganado!" else "¡El jugador O ha ganado!"
        textViewStatus.text = winnerText
        if (player == "X") playerXPoints++ else playerOPoints++
        updateScore()
    }

    private fun draw() {
        isGameActive = false
        textViewStatus.text = "¡Empate!"
    }

    private fun updateScore() {
        textViewPlayerX.text = "Jugador X: $playerXPoints"
        val opponentName = if (gameMode == "PVC") "CPU" else "Jugador O"
        textViewPlayerO.text = "$opponentName: $playerOPoints"
    }

    private fun resetBoard() {
        for (i in 0..2) {
            for (j in 0..2) {
                board[i][j] = ""
            }
        }
        boardView.setBoard(board) // Actualiza la vista
        roundCount = 0
        playerXTurn = true
        isGameActive = true
        updateStatusText()
    }

}