package com.example.androidtictactoe

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.content.Intent
import android.widget.Button

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val btnPlayerVsPlayer: Button = findViewById(R.id.btnPlayerVsPlayer)
        val btnCpuEasy: Button = findViewById(R.id.btnCpuEasy)
        val btnCpuMedium: Button = findViewById(R.id.btnCpuMedium)
        val btnCpuHard: Button = findViewById(R.id.btnCpuHard)

        btnPlayerVsPlayer.setOnClickListener {
            startGame("PVP", "NONE")
        }

        btnCpuEasy.setOnClickListener {
            startGame("PVC", "EASY")
        }

        btnCpuMedium.setOnClickListener {
            startGame("PVC", "MEDIUM")
        }

        btnCpuHard.setOnClickListener {
            startGame("PVC", "HARD")
        }
    }

    private fun startGame(gameMode: String, difficulty: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("GAME_MODE", gameMode)
        intent.putExtra("DIFFICULTY", difficulty)
        startActivity(intent)
    }
}