package com.orange.molkky

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
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
    private val playersViewModel: PlayersViewModel by viewModels()
    private val playersAdapter: PlayersAdapter = PlayersAdapter { position, newPlayer ->
        playersViewModel.players[position] = newPlayer
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

        binding.keyboard.button0.setOnClickListener { addDigit(0) }
        binding.keyboard.button1.setOnClickListener { addDigit(1) }
        binding.keyboard.button2.setOnClickListener { addDigit(2) }
        binding.keyboard.button3.setOnClickListener { addDigit(3) }
        binding.keyboard.button4.setOnClickListener { addDigit(4) }
        binding.keyboard.button5.setOnClickListener { addDigit(5) }
        binding.keyboard.button6.setOnClickListener { addDigit(6) }
        binding.keyboard.button7.setOnClickListener { addDigit(7) }
        binding.keyboard.button8.setOnClickListener { addDigit(8) }
        binding.keyboard.button9.setOnClickListener { addDigit(9) }
        binding.keyboard.backspace.setOnClickListener {
            val length = binding.keyboard.newScore.text.length
            if (length > 0) {
                vibrate()
                binding.keyboard.newScore.text =
                    binding.keyboard.newScore.text.toString().substring(0, length - 1)
                checkValidateButton()
            }
        }
        binding.keyboard.check.setOnClickListener {
            vibrate()
            val score = binding.keyboard.newScore.text.toString().toInt()
            binding.keyboard.newScore.text = ""
            getActivePlayer()!!.scores.add(score)
            computeGame()
        }
    }

    private fun addDigit(digit: Int) {
        vibrate()
        binding.keyboard.newScore.text =
            binding.keyboard.newScore.text.toString() + digit.toString()
        checkValidateButton()
    }

    private fun vibrate() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(50)
        }
    }

    private fun checkValidateButton() {
        binding.keyboard.check.isEnabled = getActivePlayer() != null &&
                binding.keyboard.newScore.text.toString().toIntOrNull() ?: Int.MAX_VALUE <= 12
    }

    private fun computeGame() {
        for (player in playersViewModel.players) {
            player.active = false
            player.playingState = PlayerInfo.PlayingState.PLAYING
            player.rank = 0
            computeTotal(player)
        }
        var rank = 1
        for (player in playersViewModel.players
            .filter { it.playingState == PlayerInfo.PlayingState.WON }
            .sortedBy { it.roundWon }) {
            player.rank = rank
            rank++
        }
        getActivePlayer()?.active = true
        playersAdapter.submitList(playersViewModel.players)
        playersAdapter.notifyDataSetChanged()
        checkValidateButton()
    }

    private fun getActivePlayer(): PlayerInfo? {
        val round = getRound()
        for (player in playersViewModel.players.filter { it.playingState == PlayerInfo.PlayingState.PLAYING }) {
            if (player.scores.size < round) {
                return player
            }
        }
        return null
    }

    private fun getRound(): Int {
        val currentPlayers =
            playersViewModel.players.filter { it.playingState == PlayerInfo.PlayingState.PLAYING }
        val maxRound = currentPlayers.map { it.scores.size }.max() ?: 0
        val minRound = currentPlayers.map { it.scores.size }.min() ?: 0
        return if (maxRound == minRound) maxRound + 1 else maxRound
    }

    private fun computeTotal(player: PlayerInfo) {
        var total = 0
        var numOfConsecutiveZeros = 0
        for ((index, score) in player.scores.withIndex()) {
            if (score == null) continue
            if (score == 0) {
                numOfConsecutiveZeros++
                if (numOfConsecutiveZeros == 3) {
                    total = 0
                    if (threeMissYouLose) {
                        player.playingState = PlayerInfo.PlayingState.LOST
                        break
                    }
                }
            } else {
                numOfConsecutiveZeros = 0
                total += score
                if (total > goal) {
                    total = 25
                } else if (total == goal) {
                    player.playingState = PlayerInfo.PlayingState.WON
                    player.roundWon = index
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
                        playersViewModel.players.add(newPlayer)
                        computeGame()
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
                return true
            }
            R.id.action_restart -> {
                AlertDialog.Builder(this)
                    .setTitle("Recommencer la partie ?")
                    .setPositiveButton("Ok") { _, _ ->
                        for (player in playersViewModel.players) {
                            player.scores = mutableListOf()
                        }
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
            val pivotPlayer = playersViewModel.players[toPos].copy()
            playersViewModel.players[toPos] = playersViewModel.players[fromPos]
            playersViewModel.players[fromPos] = pivotPlayer
            playersAdapter.notifyItemMoved(fromPos, toPos)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            isSwiping = true
            AlertDialog.Builder(binding.root.context)
                .setTitle("Supprimer joueur ?")
                .setPositiveButton("Ok") { _, _ ->
                    playersViewModel.players.removeAt(viewHolder.adapterPosition)
                    computeGame()
                }
                .setNegativeButton("Annuler") { _, _ ->
                    computeGame()
                }
                .setOnCancelListener { computeGame() }
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
