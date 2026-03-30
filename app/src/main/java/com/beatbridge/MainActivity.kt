package com.beatbridge

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.beatbridge.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var deviceAdapter: DeviceAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            loadPairedDevices()
        } else {
            Toast.makeText(
                this,
                "Bluetooth permission is required to list paired devices",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter ?: run {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupRecyclerView()
        checkPermissionsAndLoad()
        updateStatusLabel()

        // Resume monitoring service if a device was previously selected
        if (prefs.getString(PREF_SELECTED_DEVICE, null) != null) {
            startMonitorService()
        }
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter(
            devices = deviceList,
            selectedAddress = prefs.getString(PREF_SELECTED_DEVICE, null),
            onSelect = { device -> onDeviceSelected(device) }
        )
        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
            addItemDecoration(DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL))
        }
    }

    @SuppressLint("MissingPermission")
    private fun onDeviceSelected(device: BluetoothDevice) {
        val displayName = device.name ?: device.address
        prefs.edit()
            .putString(PREF_SELECTED_DEVICE, device.address)
            .putString(PREF_SELECTED_NAME, displayName)
            .apply()
        deviceAdapter.updateSelection(device.address)
        updateStatusLabel()
        startMonitorService()
        Toast.makeText(this, "Now watching: $displayName", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissionsAndLoad() {
        val required = buildRequiredPermissions()
        val allGranted = required.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) loadPairedDevices() else permissionLauncher.launch(required)
    }

    private fun buildRequiredPermissions(): Array<String> {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            perms.add(Manifest.permission.BLUETOOTH)
            perms.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms.toTypedArray()
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        deviceList.clear()
        deviceList.addAll(
            (bluetoothAdapter.bondedDevices ?: emptySet()).sortedBy { it.name ?: it.address }
        )
        deviceAdapter.notifyDataSetChanged()

        val isEmpty = deviceList.isEmpty()
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvDevices.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateStatusLabel() {
        val selectedName = prefs.getString(PREF_SELECTED_NAME, null)
        binding.tvStatus.text = if (selectedName != null) {
            "Watching: $selectedName"
        } else {
            "Tap a device below to activate auto-play"
        }
    }

    private fun startMonitorService() {
        startForegroundService(Intent(this, BluetoothMonitorService::class.java))
    }

    companion object {
        const val PREFS_NAME = "beatbridge_prefs"
        const val PREF_SELECTED_DEVICE = "selected_device_address"
        const val PREF_SELECTED_NAME = "selected_device_name"
    }
}
