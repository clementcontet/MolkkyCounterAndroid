package com.orange.molkky

import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.orange.molkky.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var players: MutableList<PlayerInfo> = mutableListOf()
    private val playersAdapter: PlayersAdapter = PlayersAdapter { position, newPlayer ->
        players[position] = newPlayer
        computeGame()
    }
    private var threeMissYouLose = false
    private var goal = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.players.adapter = playersAdapter
        val moveHelper = ItemTouchHelper(MoveHelper())
        moveHelper.attachToRecyclerView(binding.players)
        computeGame()

        binding.button0.setOnClickListener { addDigit(0) }
        binding.button1.setOnClickListener { addDigit(1) }
        binding.button2.setOnClickListener { addDigit(2) }
        binding.button3.setOnClickListener { addDigit(3) }
        binding.button4.setOnClickListener { addDigit(4) }
        binding.button5.setOnClickListener { addDigit(5) }
        binding.button6.setOnClickListener { addDigit(6) }
        binding.button7.setOnClickListener { addDigit(7) }
        binding.button8.setOnClickListener { addDigit(8) }
        binding.button9.setOnClickListener { addDigit(9) }
        binding.backspace.setOnClickListener {
            val length = binding.newScore.text.length
            if (length > 0) {
                binding.newScore.text = binding.newScore.text.toString().substring(0, length - 1)
                checkValidateButton()
            }
        }
        binding.check.setOnClickListener {
            val score = binding.newScore.text.toString().toInt()
            binding.newScore.text = ""
            getActivePlayer()!!.scores.add(score)
            computeGame()
        }
    }

    private fun addDigit(digit: Int) {
        binding.newScore.text = binding.newScore.text.toString() + digit.toString()
        checkValidateButton()
    }

    private fun checkValidateButton() {
        binding.check.isEnabled = getActivePlayer() != null &&
                binding.newScore.text.toString().toIntOrNull() ?: Int.MAX_VALUE <= 12
    }

    private fun computeGame() {
        for (player in players) {
            player.active = false
            player.playingState = PlayerInfo.PlayingState.PLAYING
            computeTotal(player)
        }
        getActivePlayer()?.active = true
        playersAdapter.submitList(players)
        playersAdapter.notifyDataSetChanged()
        checkValidateButton()
    }

    private fun getActivePlayer(): PlayerInfo? {
        val round = getRound()
        for (player in players.filter { it.playingState == PlayerInfo.PlayingState.PLAYING }) {
            if (player.scores.size < round) {
                return player
            }
        }
        return null
    }

    private fun getRound(): Int {
        val currentPlayers = players.filter { it.playingState == PlayerInfo.PlayingState.PLAYING }
        val maxRound = currentPlayers.map { it.scores.size }.max() ?: 0
        val minRound = currentPlayers.map { it.scores.size }.min() ?: 0
        return if (maxRound == minRound) maxRound + 1 else maxRound
    }

    private fun computeTotal(player: PlayerInfo) {
        var total = 0
        var numOfZero = 0
        for (score in player.scores) {
            if (score == null) continue
            if (score == 0) {
                numOfZero++
                if (numOfZero == 3) {
                    total = 0
                    if (threeMissYouLose) {
                        player.playingState = PlayerInfo.PlayingState.LOST
                        break
                    }
                }
            } else {
                numOfZero = 0
                total += score
                if (total > goal) {
                    total = 25
                } else if (total == goal) {
                    player.playingState = PlayerInfo.PlayingState.WON
                    break
                }
            }
        }
        player.total = total
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                val editText = TextInputEditText(this)
                val inputLayout = TextInputLayout(this)
                inputLayout.addView(editText)
                editText.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
                AlertDialog.Builder(this)
                    .setTitle("Nom du joueur ?")
                    .setView(inputLayout)
                    .setPositiveButton("Ok") { _, _ ->
                        val newPlayer = PlayerInfo(editText.text.toString())
                        for (i in 0..getRound() - 2) {
                            newPlayer.scores.add(null)
                        }
                        players.add(newPlayer)
                        computeGame()
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
                return true
            }
            R.id.action_total -> {
                AlertDialog.Builder(this)
                    .setTitle("Objectif ?")
                    .setItems(arrayOf("50 points", "40 points")) { _, which ->
                        goal = if (which == 0) 50 else 40
                        computeGame()
                    }
                    .create()
                    .show()
                return true
            }
            R.id.action_death -> {
                AlertDialog.Builder(this)
                    .setTitle("Après 3 manqués...")
                    .setItems(arrayOf("Retour à 0 points", "C'est perdu !")) { _, which ->
                        threeMissYouLose = which == 1
                        computeGame()
                    }
                    .create()
                    .show()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private inner class MoveHelper : ItemTouchHelper.SimpleCallback(START or END, UP or DOWN) {
        private var isSwiping: Boolean = false
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            isSwiping = false
            val fromPos = viewHolder.adapterPosition
            val toPos = target.adapterPosition
            val pivotPlayer = players[toPos].copy()
            players[toPos] = players[fromPos]
            players[fromPos] = pivotPlayer
            playersAdapter.notifyItemMoved(fromPos, toPos)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            isSwiping = true
            AlertDialog.Builder(binding.root.context)
                .setTitle("Supprimer joueur ?")
                .setPositiveButton("Ok") { _, _ ->
                    players.removeAt(viewHolder.adapterPosition)
                    computeGame()
                }
                .setNegativeButton("Annuler") { _, _ ->
                    computeGame()
                }
                .show()
        }

        override fun clearView(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ) {
            super.clearView(recyclerView, viewHolder)
            if (!isSwiping) computeGame()
        }
    }
}
