package com.beatbridge

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.beatbridge.databinding.ItemDeviceBinding

class DeviceAdapter(
    private val devices: List<BluetoothDevice>,
    private var selectedAddress: String?,
    private val onSelect: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    private var filtered = devices.toMutableList()

    inner class ViewHolder(val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = filtered[position]
        val isSelected = device.address == selectedAddress

        holder.binding.tvDeviceName.text = device.name ?: "Unknown Device"
        holder.binding.tvDeviceAddress.text = device.address
        holder.binding.ivCheck.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        holder.itemView.isSelected = isSelected

        holder.itemView.setOnClickListener { onSelect(device) }
    }

    override fun getItemCount(): Int = filtered.size

    fun updateSelection(address: String) {
        selectedAddress = address
        notifyDataSetChanged()
    }

    @SuppressLint("MissingPermission")
    fun filter(query: String) {
        filtered = if (query.isBlank()) {
            devices.toMutableList()
        } else {
            devices.filter {
                (it.name ?: "").contains(query, ignoreCase = true) ||
                        it.address.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }
}
