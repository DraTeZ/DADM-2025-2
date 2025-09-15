package com.example.androidtictactoe

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.example.androidtictactoe.databinding.ActivityMainBinding
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var buttons: Array<Button>
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameMode = intent.getStringExtra("GAME_MODE")
        difficulty = intent.getStringExtra("DIFFICULTY")
        textViewStatus = findViewById(R.id.textViewStatus)
        textViewPlayerX = findViewById(R.id.textViewPlayerX)
        textViewPlayerO = findViewById(R.id.textViewPlayerO)
        btnReset = findViewById(R.id.btnReset)
        btnBackToMenu = findViewById(R.id.btnBackToMenu)

        buttons = Array(9) { i ->
            val buttonID = "btn${i + 1}"
            val resID = resources.getIdentifier(buttonID, "id", packageName)
            findViewById(resID)
        }
        for (button in buttons) {
            button.setOnClickListener(this)
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
    }

    override fun onClick(v: View?) {
        if (!isGameActive || (v as Button).text.toString() != "") {
            return
        }

        val buttonIndex = buttons.indexOf(v)
        val row = buttonIndex / 3
        val col = buttonIndex % 3

        val playerSymbol = if (playerXTurn) "X" else "O"
        v.text = playerSymbol
        v.setTextColor(ContextCompat.getColor(this, if (playerXTurn) android.R.color.holo_blue_light else android.R.color.holo_red_light))
        board[row][col] = playerSymbol
        textViewStatus.text = if (playerXTurn) "Turno del Jugador O" else "Turno del Jugador X"

        roundCount++

        if (checkForWin()) {
            playerWins(playerSymbol)
        } else if (roundCount == 9) {
            draw()
        } else {
            playerXTurn = !playerXTurn
            if (gameMode == "PVC" && !playerXTurn && isGameActive) {
                textViewStatus.text = "Turno de la CPU"
                Handler(Looper.getMainLooper()).postDelayed({ cpuMove() }, 500)
            }
        }
    }

    private fun cpuMove() {
        if (!isGameActive) return

        val move = when (difficulty) {
            "EASY" -> findRandomMove()
            "MEDIUM" -> findMediumMove()
            "HARD" -> findHardMove()
            else -> findRandomMove()
        }

        move?.performClick()
    }

    // Nivel Fácil: Movimiento aleatorio
    private fun findRandomMove(): Button? {
        return buttons.filter { it.text.isEmpty() }.randomOrNull()
    }

    // Nivel Intermedio: Bloquea o Gana si es posible
    private fun findMediumMove(): Button? {
        // 1. ¿Puede ganar la CPU?
        var move = findWinningMove("O")
        if (move != null) return move

        // 2. ¿Necesita bloquear al jugador?
        move = findWinningMove("X")
        if (move != null) return move

        // 3. Si no, movimiento aleatorio
        return findRandomMove()
    }

    // Nivel Difícil: Estrategia de 5 pasos
    private fun findHardMove(): Button? {
        // 1. ¿Puede ganar la CPU?
        var move = findWinningMove("O")
        if (move != null) return move

        // 2. ¿Necesita bloquear al jugador?
        move = findWinningMove("X")
        if (move != null) return move

        // 3. Tomar el centro si está libre
        val centerButton = buttons[4]
        if (centerButton.text.isEmpty()) return centerButton

        // 4. Tomar una esquina si está libre
        val corners = listOf(buttons[0], buttons[2], buttons[6], buttons[8])
        val emptyCorner = corners.filter { it.text.isEmpty() }.randomOrNull()
        if (emptyCorner != null) return emptyCorner

        // 5. Tomar cualquier lado
        return findRandomMove()
    }

    // Función auxiliar para encontrar un movimiento ganador o de bloqueo
    private fun findWinningMove(player: String): Button? {
        for (i in buttons.indices) {
            if (buttons[i].text.isEmpty()) {
                val row = i / 3
                val col = i % 3
                board[row][col] = player // Simular movimiento
                if (checkForWin()) {
                    board[row][col] = "" // Deshacer simulación
                    return buttons[i]
                }
                board[row][col] = "" // Deshacer simulación
            }
        }
        return null
    }

    private fun checkForWin(): Boolean {
        for (i in 0..2) {
            if (board[i][0] == board[i][1] && board[i][0] == board[i][2] && board[i][0] != "") return true
        }
        for (i in 0..2) {
            if (board[0][i] == board[1][i] && board[0][i] == board[2][i] && board[0][i] != "") return true
        }
        if (board[0][0] == board[1][1] && board[0][0] == board[2][2] && board[0][0] != "") return true
        if (board[0][2] == board[1][1] && board[0][2] == board[2][0] && board[0][2] != "") return true
        return false
    }

    private fun playerWins(player: String) {
        val winnerText = if (player == "X") "¡El jugador X ha ganado!" else if (gameMode == "PVC") "¡La CPU ha ganado!" else "¡El jugador O ha ganado!"
        textViewStatus.text = winnerText
        if (player == "X") playerXPoints++ else playerOPoints++
        updateScore()
        isGameActive = false
    }

    private fun draw() {
        textViewStatus.text = "¡Empate!"
        isGameActive = false
    }

    private fun updateScore() {
        textViewPlayerX.text = "Jugador X: $playerXPoints"
        val opponentName = if (gameMode == "PVC") "CPU (O)" else "Jugador O"
        textViewPlayerO.text = "$opponentName: $playerOPoints"
    }

    private fun resetBoard() {
        for (i in 0..2) {
            for (j in 0..2) {
                board[i][j] = ""
            }
        }
        for (button in buttons) {
            button.text = ""
        }
        roundCount = 0
        playerXTurn = true
        textViewStatus.text = "Turno del Jugador X"
        isGameActive = true
    }
}