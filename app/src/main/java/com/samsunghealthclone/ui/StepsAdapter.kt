package com.samsunghealthclone.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samsunghealthclone.data.StepEntry
import com.samsunghealthclone.databinding.ItemStepEntryBinding

class StepsAdapter(
    private val onClick: (StepEntry, Int) -> Unit
) : ListAdapter<StepEntry, StepsAdapter.StepViewHolder>(DiffCallback()) {

    class StepViewHolder(private val binding: ItemStepEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: StepEntry, position: Int, onClick: (StepEntry, Int) -> Unit, formatDate: (Long) -> String) {
            binding.tvDate.text = formatDate(entry.date)
            binding.tvSteps.text = formatNumber(entry.steps)
            binding.tvHours.text = String.format("%.1f ч", entry.steps / 6000f)
            itemView.setOnClickListener { onClick(entry, position) }
        }

        private fun formatNumber(number: Int): String {
            return if (number >= 1000) {
                String.format("%,d", number).replace(",", " ")
            } else {
                number.toString()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val binding = ItemStepEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry, position, onClick) { timestamp ->
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = timestamp
            return@bind android.text.format.DateFormat.format("dd MMM", calendar).toString()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<StepEntry>() {
        override fun areItemsTheSame(oldItem: StepEntry, newItem: StepEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StepEntry, newItem: StepEntry): Boolean {
            return oldItem == newItem
        }
    }
}