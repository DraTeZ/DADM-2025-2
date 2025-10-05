package com.example.androidtictactoe

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.UUID

class LobbyActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var myPlayerId: String
    private lateinit var gamesAdapter: GamesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        myPlayerId = getPlayerId()
        // Usa la URL de tu base de datos
        database = Firebase.database("https://androidtictactoedadmun-default-rtdb.firebaseio.com/").getReference("games")

        val createGameButton: Button = findViewById(R.id.createGameButton)
        val gamesRecyclerView: RecyclerView = findViewById(R.id.gamesRecyclerView)
        val btnBackToMenu: Button = findViewById(R.id.btnBackToMenuLobby)

        gamesRecyclerView.layoutManager = LinearLayoutManager(this)
        gamesAdapter = GamesAdapter { game -> joinGame(game) }
        gamesRecyclerView.adapter = gamesAdapter

        createGameButton.setOnClickListener {
            createNewGame()
        }
        btnBackToMenu.setOnClickListener {
            finish()
        }
        addGamesListener()
    }

    private fun addGamesListener() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val availableGames = snapshot.children.mapNotNull { it.getValue(Game::class.java) }
                    .filter { it.gameState == "WAITING" }
                gamesAdapter.updateGames(availableGames)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun createNewGame() {
        val gameId = database.push().key ?: return
        val newGame = Game(
            gameId = gameId,
            player1Id = myPlayerId,
            gameState = "WAITING",
            currentTurn = myPlayerId
        )
        database.child(gameId).setValue(newGame).addOnSuccessListener {
            navigateToGame(gameId)
        }
    }

    private fun joinGame(game: Game) {
        game.gameId?.let {
            database.child(it).child("player2Id").setValue(myPlayerId)
            database.child(it).child("gameState").setValue("IN_PROGRESS")
            navigateToGame(it)
        }
    }

    private fun navigateToGame(gameId: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("GAME_ID", gameId) // Marcador para modo online
        startActivity(intent)
    }

    private fun getPlayerId(): String {
        val prefs = getSharedPreferences("TicTacToePrefs", MODE_PRIVATE)
        var id = prefs.getString("playerId", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("playerId", id).apply()
        }
        return id
    }
}

// --- Adapter para el RecyclerView (puedes ponerlo en el mismo archivo para simplificar) ---
class GamesAdapter(private val onGameClicked: (Game) -> Unit) : RecyclerView.Adapter<GamesAdapter.GameViewHolder>() {
    private var games = listOf<Game>()

    fun updateGames(newGames: List<Game>) {
        games = newGames
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(games[position], onGameClicked)
    }

    override fun getItemCount() = games.size

    class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        fun bind(game: Game, onGameClicked: (Game) -> Unit) {
            textView.text = "Partida de Jugador #${game.player1Id?.take(6)}"
            itemView.setOnClickListener { onGameClicked(game) }
        }
    }
}