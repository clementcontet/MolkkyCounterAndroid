package com.orange.molkky

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator.INFINITE
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.orange.molkky.databinding.PlayerBinding
import com.orange.molkky.db.PlayerTable

class PlayersAdapter(val changePlayer: (newPlayer: PlayerTable) -> Unit) :
    RecyclerView.Adapter<PlayersAdapter.ViewHolder>() {
    private var players: List<PlayerTable> = listOf()
    fun submitList(newPlayers: List<PlayerTable>) {
        players = newPlayers
    }

    override fun getItemCount(): Int {
        return players.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: PlayerBinding =
            PlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    inner class ViewHolder(private val binding: PlayerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var animator: ObjectAnimator =
            ObjectAnimator.ofFloat(binding.star, View.ROTATION, 360f)
        private val adapter = ScoresAdapter()

        init {
            animator.duration = 1000
            animator.repeatCount = INFINITE
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    binding.star.rotation = 0f
                }
            })
        }

        fun bind(playerPosition: Int) {
            val player = players[playerPosition]
            binding.layout.setBackgroundColor(
                ResourcesCompat.getColor(
                    binding.root.context.resources,
                    when {
                        player.playingState == PlayerTable.PlayingState.WON -> R.color.colorPrimary
                        player.playingState == PlayerTable.PlayingState.LOST -> R.color.grey
                        player.active -> R.color.colorAccent
                        else -> R.color.white
                    },
                    null
                )
            )
            binding.name.text = player.name

            if (player.playingState == PlayerTable.PlayingState.WON) {
                binding.rank.text = player.rank.toString()
                binding.medal.visibility = View.VISIBLE
                animator.start()
            } else {
                binding.medal.visibility = View.GONE
            }

            binding.name.setOnClickListener {
                val editText = TextInputEditText(binding.root.context)
                val inputLayout = TextInputLayout(binding.root.context)
                inputLayout.addView(editText)
                editText.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
                editText.setText(player.name)
                AlertDialog.Builder(binding.root.context)
                    .setTitle("Renommer joueur ?")
                    .setView(inputLayout)
                    .setPositiveButton("Ok") { _, _ ->
                        player.name = editText.text.toString()
                        changePlayer(player)
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }

            adapter.changeScores = { scorePosition, newScore ->
                player.scores[player.scores.size - 1 - scorePosition] = newScore
                changePlayer(player)
            }
            binding.scores.adapter = adapter
            binding.total.text = player.total.toString() + " pts"
            adapter.submitList(player.scores.reversed())
        }

        fun unbind() {
            animator.cancel()
        }
    }
}