package com.orange.molkky

import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.orange.molkky.databinding.ScoreBinding
import java.text.DecimalFormat

class ScoresAdapter : ListAdapter<Int?, ScoresAdapter.ViewHolder>(DatabaseSolutionDiffCallback()) {
    lateinit var changeScores: (position: Int, newScore: Int) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ScoreBinding =
            ScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(private val binding: ScoreBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val score = getItem(position)
            binding.score.text = if (score == null) "-" else DecimalFormat("#00").format(score)
            binding.score.setOnClickListener {
                val currentPosition = adapterPosition
                val editText = TextInputEditText(binding.root.context)
                val inputLayout = TextInputLayout(binding.root.context)
                inputLayout.addView(editText)
                editText.inputType = InputType.TYPE_CLASS_PHONE
                editText.setText(score?.toString() ?: "")
                AlertDialog.Builder(binding.root.context)
                    .setTitle("Modifier score ?")
                    .setView(inputLayout)
                    .setPositiveButton("Ok") { _, _ ->
                        changeScores(
                            currentPosition,
                            editText.text.toString().toIntOrNull() ?: 0
                        )
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
        }
    }

    private class DatabaseSolutionDiffCallback : DiffUtil.ItemCallback<Int?>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }
    }
}