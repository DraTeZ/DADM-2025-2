package com.example.androidtictactoe

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel

class GameViewModel(application: Application) : AndroidViewModel(application) {

    // Variables de estado del juego
    var board = Array(3) { Array(3) { "" } }
    var playerXTurn = true
    var roundCount = 0
    var isGameActive = true
    var statusText = "Turno del Jugador X"
    var playerXPoints = 0
    var playerOPoints = 0

    // Variable para la dificultad
    var difficulty = "HARD" // Valor por defecto

    private val prefs: SharedPreferences

    init {
        prefs = application.getSharedPreferences("TicTacToePrefs", Context.MODE_PRIVATE)

        // Cargar puntajes
        playerXPoints = prefs.getInt("playerXPoints", 0)
        playerOPoints = prefs.getInt("playerOPoints", 0)

        // Cargar la dificultad guardada (0=Fácil, 1=Intermedio, 2=Difícil)
        val difficultyInt = prefs.getInt("difficulty", 2) // Por defecto es 2 (Difícil)
        difficulty = when (difficultyInt) {
            0 -> "EASY"
            1 -> "MEDIUM"
            else -> "HARD"
        }
    }

    fun saveState() {
        val editor = prefs.edit()
        editor.putInt("playerXPoints", playerXPoints)
        editor.putInt("playerOPoints", playerOPoints)

        // Convertir la dificultad de String a Int para guardarla
        val difficultyInt = when (difficulty) {
            "EASY" -> 0
            "MEDIUM" -> 1
            else -> 2
        }
        editor.putInt("difficulty", difficultyInt)

        editor.apply()
    }

    fun resetBoard() {
        board = Array(3) { Array(3) { "" } }
        roundCount = 0
        playerXTurn = true
        isGameActive = true
        statusText = "Turno del Jugador X"
    }
}