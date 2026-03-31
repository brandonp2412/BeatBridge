package com.beatbridge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.beatbridge.databinding.ItemAppBinding

data class MusicApp(val packageName: String, val appName: String)

class AppAdapter(
    private val apps: List<MusicApp>,
    private var selectedPackage: String?,
    private val onSelect: (MusicApp) -> Unit
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    private var filtered = apps.toMutableList()

    inner class ViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = filtered[position]
        val isSelected = app.packageName == selectedPackage

        holder.binding.tvAppName.text = app.appName
        holder.binding.ivCheck.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        holder.itemView.isSelected = isSelected

        try {
            val icon = holder.itemView.context.packageManager.getApplicationIcon(app.packageName)
            holder.binding.ivAppIcon.setImageDrawable(icon)
        } catch (_: Exception) {
            holder.binding.ivAppIcon.setImageDrawable(null)
        }

        holder.itemView.setOnClickListener { onSelect(app) }
    }

    override fun getItemCount(): Int = filtered.size

    fun updateSelection(packageName: String?) {
        selectedPackage = packageName
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filtered = if (query.isBlank()) {
            apps.toMutableList()
        } else {
            apps.filter { it.appName.contains(query, ignoreCase = true) }.toMutableList()
        }
        notifyDataSetChanged()
    }
}
