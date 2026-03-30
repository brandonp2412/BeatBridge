package com.beatbridge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.beatbridge.databinding.ItemDeviceBinding

/**
 * Test-only adapter that renders the same item_device.xml layout as DeviceAdapter,
 * but accepts plain (name, address) pairs instead of BluetoothDevice objects.
 *
 * This lets screenshot tests inject any device list without needing Mockito
 * or real paired Bluetooth hardware.
 */
class FakeDeviceAdapter(
    private val items: List<Pair<String, String>>,
    private val selectedAddress: String? = null,
) : RecyclerView.Adapter<FakeDeviceAdapter.VH>() {

    inner class VH(val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (name, address) = items[position]
        val selected = address == selectedAddress
        holder.binding.tvDeviceName.text = name
        holder.binding.tvDeviceAddress.text = address
        holder.binding.ivCheck.visibility = if (selected) View.VISIBLE else View.INVISIBLE
        holder.itemView.isSelected = selected
    }

    override fun getItemCount() = items.size
}
