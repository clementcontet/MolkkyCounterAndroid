package com.orange.molkky

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.orange.molkky.databinding.ActivityMainBinding
import com.orange.molkky.db.AppDatabase
import com.orange.molkky.db.PlayerTable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var disposable: CompositeDisposable
    lateinit var db: AppDatabase
    lateinit var players: MutableList<PlayerTable>
    lateinit var playersAdapter: PlayersAdapter
    private var threeMissYouLose = false
    private var goal = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disposable = CompositeDisposable()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "molkky-db"
        ).build()
        playersAdapter = PlayersAdapter {
            disposable.add(db.playerDao.updatePlayer(it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ -> Log.i("Mölkky", "Player updated") }
            )
        }
        binding.players.adapter = playersAdapter
        val moveHelper = ItemTouchHelper(MoveHelper())
        moveHelper.attachToRecyclerView(binding.players)
        binding.newPlayer.setOnClickListener { createNewPlayer() }
    }

    override fun onResume() {
        super.onResume()
        threeMissYouLose = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(getString(R.string.pref_3_miss), false)
        goal = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(getString(R.string.pref_goal), "50")!!.toInt()
        disposable.add(db.playerDao.getPlayers()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { dbPlayers ->
                players = dbPlayers.sortedBy { it.order }.toMutableList()
                computeGame()
                handleKeys()
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
    }

    private fun handleKeys() {
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
            val activePlayer = getActivePlayer()!!
            activePlayer.scores.add(score)
            disposable.add(db.playerDao.updatePlayer(activePlayer)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ -> Log.i("Mölkky", "Score added") }
            )
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
            @Suppress("DEPRECATION")
            v.vibrate(50)
        }
    }

    private fun checkValidateButton() {
        binding.keyboard.check.isEnabled = getActivePlayer() != null &&
                binding.keyboard.newScore.text.toString().toIntOrNull() ?: Int.MAX_VALUE <= 12
    }

    private fun computeGame() {
        for (player in players) {
            player.active = false
            player.playingState = PlayerTable.PlayingState.PLAYING
            player.rank = 0
            computeTotal(player)
        }
        var rank = 1
        for (player in players
            .filter { it.playingState == PlayerTable.PlayingState.WON }
            .sortedBy { it.roundWon }) {
            player.rank = rank
            rank++
        }
        getActivePlayer()?.active = true

        playersAdapter.submitList(players)
        if (players.isEmpty()) {
            binding.players.visibility = View.GONE
            binding.newPlayer.visibility = View.VISIBLE
        } else {
            binding.players.visibility = View.VISIBLE
            binding.newPlayer.visibility = View.GONE
        }

        playersAdapter.notifyDataSetChanged()
        Log.i("Mölkky", "submit $players")
        checkValidateButton()
        getActivePlayer()?.let {
            binding.players.post { binding.players.smoothScrollToPosition(players.indexOf(it)) }
        }
    }

    private fun getActivePlayer(): PlayerTable? {
        val round = getRound()
        for (player in players.filter { it.playingState == PlayerTable.PlayingState.PLAYING }) {
            if (player.scores.size < round) {
                return player
            }
        }
        return null
    }

    private fun getRound(): Int {
        val currentPlayers =
            players.filter { it.playingState == PlayerTable.PlayingState.PLAYING }
        val maxRound = currentPlayers.map { it.scores.size }.max() ?: 0
        val minRound = currentPlayers.map { it.scores.size }.min() ?: 0
        return if (maxRound == minRound) maxRound + 1 else maxRound
    }

    private fun computeTotal(player: PlayerTable) {
        var total = 0
        var numOfConsecutiveZeros = 0
        for ((index, score) in player.scores.withIndex()) {
            if (score == null) continue
            if (score == 0) {
                numOfConsecutiveZeros++
                if (numOfConsecutiveZeros == 3) {
                    total = 0
                    if (threeMissYouLose) {
                        player.playingState = PlayerTable.PlayingState.LOST
                        break
                    }
                }
            } else {
                numOfConsecutiveZeros = 0
                total += score
                if (total > goal) {
                    total = 25
                } else if (total == goal) {
                    player.playingState = PlayerTable.PlayingState.WON
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
                createNewPlayer()
                return true
            }
            R.id.action_restart -> {
                AlertDialog.Builder(this)
                    .setTitle("Recommencer la partie ?")
                    .setPositiveButton("Ok") { _, _ ->
                        for (player in players) {
                            player.scores = mutableListOf()
                        }
                        disposable.add(db.playerDao.updatePlayers(players)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { _ -> Log.i("Mölkky", "Scores reseted") })
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
                return true
            }
            R.id.action_delete_players -> {
                val checked = players.map { false }.toMutableList()
                AlertDialog.Builder(this)
                    .setTitle("Joueurs à supprimer ?")
                    .setMultiChoiceItems(
                        players.map { it.name }.toTypedArray(),
                        players.map { false }.toBooleanArray()
                    ) { _, which, isChecked -> checked[which] = isChecked }
                    .setPositiveButton("Ok")
                    { _, _ ->
                        disposable.add(db.playerDao.deletePlayers(players.filterIndexed { key, _ -> checked[key] })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { _ -> Log.i("Mölkky", "Players deleted") })
                    }
                    .setNegativeButton("Annuler", null)
                    .create()
                    .show()
                return true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createNewPlayer() {
        val editText = TextInputEditText(this)
        val inputLayout = TextInputLayout(this)
        inputLayout.addView(editText)
        editText.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
        AlertDialog.Builder(this)
            .setTitle("Nom du joueur ?")
            .setView(inputLayout)
            .setPositiveButton("Ok") { _, _ ->
                val newPlayer =
                    PlayerTable(
                        name = editText.text.toString(),
                        order = (players.map { it.order }.max() ?: 0) + 1
                    )
                for (i in 0..getRound() - 2) {
                    newPlayer.scores.add(null)
                }
                disposable.add(db.playerDao.insertPlayer(newPlayer)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { _ -> Log.i("Mölkky", "Player added") }
                )
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private inner class MoveHelper : ItemTouchHelper.SimpleCallback(START or END, 0) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPos = viewHolder.adapterPosition
            val toPos = target.adapterPosition

            val pivotOrder = players[toPos].order
            players[toPos].order = players[fromPos].order
            players[fromPos].order = pivotOrder

            val pivotPlayer = players[toPos].copy()
            players[toPos] = players[fromPos]
            players[fromPos] = pivotPlayer

            playersAdapter.notifyItemMoved(fromPos, toPos)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            disposable.add(db.playerDao.updatePlayers(players)
                .subscribeOn(Schedulers.io())
                .subscribe { _ -> Log.i("Mölkky", "Players swiped") }
            )
        }
    }
}
