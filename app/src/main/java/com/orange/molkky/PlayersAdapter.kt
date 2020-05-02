package com.orange.molkky

import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.orange.molkky.databinding.PlayerBinding

class PlayersAdapter(val computeGame: () -> Unit) :
    ListAdapter<PlayerInfo, PlayersAdapter.ViewHolder>(PlayersDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: PlayerBinding = PlayerBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)!!
        holder.bind(item)
    }

    inner class ViewHolder(private val binding: PlayerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(player: PlayerInfo) {
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
                        computeGame()
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
            val adapter = ScoresAdapter { position, newScore ->
                player.scores[position] = newScore
                computeGame()
            }
            binding.scores.adapter = adapter
            binding.total.text = player.total.toString() + " pts"
            adapter.submitList(player.scores)
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