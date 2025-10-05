package com.example.androidtictactoe

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.Menu
import android.view.MenuItem


import android.content.Intent
import android.widget.Button

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val btnPlayerVsPlayer: Button = findViewById(R.id.btnPlayerVsPlayer)
        val btnPlayerVsCpu: Button = findViewById(R.id.btnPlayerVsCpu)
        val btnOnlineGame: Button = findViewById(R.id.btnOnlineGame)
        val btnQuitApp: Button = findViewById(R.id.btnQuitApp)

        btnPlayerVsPlayer.setOnClickListener {
            startGame("PVP", "NONE")
        }
        btnPlayerVsCpu.setOnClickListener {
            showDifficultyDialog()
        }
        btnOnlineGame.setOnClickListener {
            val intent = Intent(this, LobbyActivity::class.java)
            startActivity(intent)
        }
        btnQuitApp.setOnClickListener {

            showQuitConfirmationDialog()
        }
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

    private fun showDifficultyDialog() {
        // Opciones que se mostrarán en el diálogo
        val difficultyOptions = arrayOf("Fácil", "Intermedio", "Difícil")

        // Crear el constructor del AlertDialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Elige la dificultad")
            .setItems(difficultyOptions) { dialog, which ->
                // 'which' es el índice del elemento seleccionado (0, 1, o 2)
                val difficulty = when (which) {
                    0 -> "EASY"
                    1 -> "MEDIUM"
                    2 -> "HARD"
                    else -> "EASY" // Opción por defecto
                }
                startGame("PVC", difficulty)
            }

        // Crear y mostrar el diálogo
        builder.create().show()
    }

    private fun showQuitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar salida")
            .setMessage("¿Estás seguro de que deseas salir?")
            .setCancelable(false)
            .setPositiveButton("Sí") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun startGame(gameMode: String, difficulty: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("GAME_MODE", gameMode)
        intent.putExtra("DIFFICULTY", difficulty)
        startActivity(intent)
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Acerca de Tic-Tac-Toe DADM")
            .setMessage("Creado por: David Alexander Rátiva Gutiérez\nVersión: 1.0")
            .setPositiveButton("Cerrar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

}