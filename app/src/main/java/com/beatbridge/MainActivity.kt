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
import android.text.Editable
import android.text.TextWatcher
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
    private val musicAppList = mutableListOf<MusicApp>()
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var appAdapter: AppAdapter

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

        loadMusicApps()
        setupDeviceRecyclerView()
        setupAppRecyclerView()
        setupSearch()
        checkPermissionsAndLoad()
        updateStatusLabel()

        if (prefs.getString(PREF_SELECTED_DEVICE, null) != null) {
            startMonitorService()
        }
    }

    private fun loadMusicApps() {
        musicAppList.clear()

        // Show all launchable apps so the user can pick any media player
        val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val found = packageManager.queryIntentActivities(launchIntent, 0)
            .mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(packageManager).toString()
                if (pkg == packageName) null else MusicApp(pkg, label)
            }
            .sortedBy { it.appName }

        musicAppList.addAll(found)

        val isEmpty = musicAppList.isEmpty()
        binding.tvAppsEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvApps.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun setupDeviceRecyclerView() {
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

    private fun setupSearch() {
        binding.etDeviceSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                deviceAdapter.filter(s?.toString() ?: "")
            }
        })
        binding.etAppSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                appAdapter.filter(s?.toString() ?: "")
            }
        })
    }

    private fun setupAppRecyclerView() {
        appAdapter = AppAdapter(
            apps = musicAppList,
            selectedPackage = prefs.getString(PREF_SELECTED_APP, null),
            onSelect = { app -> onAppSelected(app) }
        )
        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appAdapter
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

    private fun onAppSelected(app: MusicApp) {
        val current = prefs.getString(PREF_SELECTED_APP, null)
        if (current == app.packageName) {
            // Tap again to deselect
            prefs.edit().remove(PREF_SELECTED_APP).apply()
            appAdapter.updateSelection(null)
            Toast.makeText(this, "No app — will resume whatever was last playing", Toast.LENGTH_SHORT).show()
        } else {
            prefs.edit().putString(PREF_SELECTED_APP, app.packageName).apply()
            appAdapter.updateSelection(app.packageName)
            Toast.makeText(this, "Will open ${app.appName} on connect", Toast.LENGTH_SHORT).show()
        }
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
        // Re-run the current filter so `filtered` syncs with the newly loaded list
        deviceAdapter.filter(binding.etDeviceSearch.text.toString())

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

    fun startMonitorService() {
        startForegroundService(Intent(this, BluetoothMonitorService::class.java))
    }

    companion object {
        const val PREFS_NAME = "beatbridge_prefs"
        const val PREF_SELECTED_DEVICE = "selected_device_address"
        const val PREF_SELECTED_NAME = "selected_device_name"
        const val PREF_SELECTED_APP = "selected_app_package"
    }
}
