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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.orange.molkky.databinding.PlayerBinding

class PlayersAdapter(val changePlayer: (position: Int, newPlayer: PlayerInfo) -> Unit) :
    ListAdapter<PlayerInfo, PlayersAdapter.ViewHolder>(PlayersDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: PlayerBinding = PlayerBinding.inflate(LayoutInflater.from(parent.context))
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
        var animator: ObjectAnimator? = null

        fun bind(playerPosition: Int) {
            val player = getItem(playerPosition)
            binding.layout.setBackgroundColor(
                ResourcesCompat.getColor(
                    binding.root.context.resources,
                    when {
                        player.playingState == PlayerInfo.PlayingState.WON -> R.color.colorPrimary
                        player.playingState == PlayerInfo.PlayingState.LOST -> R.color.grey
                        player.active -> R.color.colorAccent
                        else -> R.color.white
                    },
                    null
                )
            )
            binding.name.text = player.name

            if (player.rank > 0) {
                binding.rank.text = player.rank.toString()
                binding.medal.visibility = View.VISIBLE
                animator = ObjectAnimator.ofFloat(binding.star, View.ROTATION, 360f)
                animator?.duration = 1000
                animator?.repeatCount = INFINITE
                animator?.interpolator = AccelerateDecelerateInterpolator()
                animator?.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        binding.star.rotation = 0f
                    }
                })
                animator?.start()
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
                        changePlayer(playerPosition, player)
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
            val adapter = ScoresAdapter { scorePosition, newScore ->
                player.scores[scorePosition] = newScore
                changePlayer(playerPosition, player)
            }
            binding.scores.adapter = adapter
            binding.total.text = player.total.toString() + " pts"
            adapter.submitList(player.scores)
        }

        fun unbind() {
            animator?.cancel()
        }
    }

    private class PlayersDiffCallback : DiffUtil.ItemCallback<PlayerInfo>() {
        override fun areItemsTheSame(oldItem: PlayerInfo, newItem: PlayerInfo): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: PlayerInfo, newItem: PlayerInfo): Boolean {
            return oldItem.name == newItem.name
                    && oldItem.active == newItem.active
                    && (oldItem.scores sameContentWith newItem.scores)!!
        }
    }
}