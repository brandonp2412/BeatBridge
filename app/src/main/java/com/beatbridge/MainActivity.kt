package com.beatbridge

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.beatbridge.databinding.ActivityMainBinding
import androidx.core.content.edit
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val deviceList = mutableListOf<BtDevice>()
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
        setupAnyDeviceToggle()
        setupDelaySlider()
        checkPermissionsAndLoad()
        updateStatusLabel()
        binding.tvWhatsNew.setOnClickListener {
            startActivity(Intent(this, WhatsNewActivity::class.java))
        }
        binding.tvSourceCode.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/brandonp2412/BeatBridge".toUri()))
        }
        binding.tvDonate.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/sponsors/brandonp2412".toUri()))
        }

        syncMonitorService()
    }

    private fun loadMusicApps() {
        musicAppList.clear()

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
            selectedAddresses = prefs.getStringSet(PREF_SELECTED_DEVICES, emptySet()) ?: emptySet(),
            onSelect = { device -> onDeviceSelected(device) },
            onConfigure = { device -> openDeviceAppsScreen(device) }
        )
        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
            addItemDecoration(createListDivider())
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
            selectedPackages = prefs.getStringSet(PREF_SELECTED_APPS, emptySet()) ?: emptySet(),
            onSelect = { app -> onAppSelected(app) }
        )
        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appAdapter
            addItemDecoration(createListDivider())
        }
    }

    private fun createListDivider() = SelectionAwareDividerDecoration(
        color = ContextCompat.getColor(this, R.color.bb_divider),
        density = resources.displayMetrics.density,
    )

    private fun setupAnyDeviceToggle() {
        val anyDevice = prefs.getBoolean(PREF_ANY_DEVICE, false)
        binding.switchAnyDevice.isChecked = anyDevice
        updateDeviceSectionEnabled(!anyDevice)
        binding.switchAnyDevice.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(PREF_ANY_DEVICE, isChecked) }
            updateDeviceSectionEnabled(!isChecked)
            updateStatusLabel()
            syncMonitorService()
        }
    }

    private fun setupDelaySlider() {
        val saved = prefs.getInt(PREF_LAUNCH_DELAY, 1)
        binding.sliderDelay.progress = saved
        updateDelayLabel(saved)
        binding.sliderDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    prefs.edit { putInt(PREF_LAUNCH_DELAY, progress) }
                    updateDelayLabel(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun updateDelayLabel(seconds: Int) {
        binding.tvDelayLabel.text = "Launch delay: ${seconds}s"
    }

    private fun updateDeviceSectionEnabled(enabled: Boolean) {
        binding.layoutDeviceSection.alpha = if (enabled) 1f else 0.38f
        binding.tilDeviceSearch.isEnabled = enabled
        binding.etDeviceSearch.isEnabled = enabled
    }

    private fun onDeviceSelected(device: BtDevice) {
        if (prefs.getBoolean(PREF_ANY_DEVICE, false)) return
        val current = LinkedHashSet(prefs.getStringSet(PREF_SELECTED_DEVICES, emptySet()) ?: emptySet())
        val displayName = device.name.ifEmpty { device.address }
        if (device.address in current) {
            current.remove(device.address)
            Toast.makeText(this, "Removed: $displayName", Toast.LENGTH_SHORT).show()
        } else {
            current.add(device.address)
            Toast.makeText(this, "Added: $displayName", Toast.LENGTH_SHORT).show()
        }
        prefs.edit { putStringSet(PREF_SELECTED_DEVICES, current) }
        deviceAdapter.updateSelections(current)
        updateStatusLabel()
        syncMonitorService()
    }

    private fun onAppSelected(app: MusicApp) {
        val current = LinkedHashSet(prefs.getStringSet(PREF_SELECTED_APPS, emptySet()) ?: emptySet())
        if (app.packageName in current) {
            current.remove(app.packageName)
            prefs.edit { putStringSet(PREF_SELECTED_APPS, current) }
            appAdapter.updateSelections(current)
            Toast.makeText(this, "Removed ${app.appName}", Toast.LENGTH_SHORT).show()
        } else {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${packageName}".toUri()
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            current.add(app.packageName)
            prefs.edit { putStringSet(PREF_SELECTED_APPS, current) }
            appAdapter.updateSelections(current)
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
            (bluetoothAdapter.bondedDevices ?: emptySet())
                .map { BtDevice(address = it.address, name = it.name ?: "") }
                .sortedBy { it.name.ifEmpty { it.address } }
        )
        deviceAdapter.filter(binding.etDeviceSearch.text.toString())

        val isEmpty = deviceList.isEmpty()
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvDevices.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateStatusLabel() {
        val selectedAddresses = prefs.getStringSet(PREF_SELECTED_DEVICES, emptySet()) ?: emptySet()
        binding.tvStatus.text = when {
            prefs.getBoolean(PREF_ANY_DEVICE, false) -> "Auto-playing on any Bluetooth connection"
            selectedAddresses.isNotEmpty() -> {
                val names = selectedAddresses.map { addr ->
                    deviceList.find { it.address == addr }?.name?.ifEmpty { addr } ?: addr
                }
                "Watching: ${names.joinToString(", ")}"
            }
            else -> "Tap a device below to activate auto-play"
        }
    }

    private fun openDeviceAppsScreen(device: BtDevice) {
        val intent = Intent(this, DeviceAppsActivity::class.java).apply {
            putExtra(DeviceAppsActivity.EXTRA_DEVICE_ADDRESS, device.address)
            putExtra(DeviceAppsActivity.EXTRA_DEVICE_NAME, device.name.ifEmpty { device.address })
        }
        startActivity(intent)
    }

    private fun syncMonitorService() {
        val selectedAddresses = prefs.getStringSet(PREF_SELECTED_DEVICES, emptySet()) ?: emptySet()
        val shouldMonitor = prefs.getBoolean(PREF_ANY_DEVICE, false) || selectedAddresses.isNotEmpty()
        val serviceIntent = Intent(this, BluetoothMonitorService::class.java)
        if (shouldMonitor) {
            startForegroundService(serviceIntent)
        } else {
            stopService(serviceIntent)
        }
    }

    companion object {
        const val PREFS_NAME = "beatbridge_prefs"
        const val PREF_SELECTED_DEVICES = "selected_device_addresses"
        const val PREF_SELECTED_APPS = "selected_app_packages"
        const val PREF_ANY_DEVICE = "any_device"
        const val PREF_LAUNCH_DELAY = "launch_delay_seconds"
        const val PREF_DEVICE_APPS_PREFIX = "device_apps_"
        const val PREF_DEVICE_ASK_PREFIX = "device_ask_"
        const val PREF_DEVICE_EQ_PREFIX = "device_eq_"
    }
}
