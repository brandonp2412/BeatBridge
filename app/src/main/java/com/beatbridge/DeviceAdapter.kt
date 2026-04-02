package com.beatbridge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.beatbridge.databinding.ItemDeviceBinding

data class BtDevice(val address: String, val name: String)

class DeviceAdapter(
    private val devices: List<BtDevice>,
    private var selectedAddress: String?,
    private val onSelect: (BtDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    private var filtered = devices.toMutableList()

    inner class ViewHolder(val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = filtered[position]
        val isSelected = device.address == selectedAddress

        holder.binding.tvDeviceName.text = device.name.ifEmpty { "Unknown Device" }
        holder.binding.tvDeviceAddress.text = device.address
        holder.binding.ivCheck.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        holder.itemView.isSelected = isSelected

        holder.itemView.setOnClickListener { onSelect(device) }
    }

    override fun getItemCount(): Int = filtered.size

    fun updateSelection(address: String) {
        val oldPos = filtered.indexOfFirst { it.address == selectedAddress }
        val newPos = filtered.indexOfFirst { it.address == address }
        selectedAddress = address
        if (oldPos >= 0) notifyItemChanged(oldPos)
        if (newPos >= 0) notifyItemChanged(newPos)
    }

    fun filter(query: String) {
        val newList = if (query.isBlank()) {
            devices.toMutableList()
        } else {
            devices.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.address.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = filtered.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                filtered[oldPos].address == newList[newPos].address
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                filtered[oldPos] == newList[newPos]
        })
        filtered = newList
        diff.dispatchUpdatesTo(this)
    }
}
