package com.example.androidtictactoe

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Game(
    val gameId: String? = null,
    val player1Id: String? = null,
    var player2Id: String? = null,
    var board: String? = "---------",
    var gameState: String? = "WAITING",
    var currentTurn: String? = null,
    var winner: String? = null,
    var player1Score: Int = 0,
    var player2Score: Int = 0
)