package com.beatbridge

import android.annotation.SuppressLint
import android.Manifest
import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat

class BluetoothMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var equalizer: Equalizer? = null
    private var equalizerDeviceAddress: String? = null
    private val lastHandledConnections = mutableMapOf<String, Long>()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> device?.let {
                    Log.i(TAG, "ACL connected: ${it.address}")
                    maybeHandleDeviceConnected(it)
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    device?.let {
                        Log.i(TAG, "ACL disconnected: ${it.address}")
                        lastHandledConnections.remove(it.address)
                    }
                    if (device == null || device.address == equalizerDeviceAddress) {
                        releaseEqualizer()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        createLaunchNotificationChannel()
        createActionsNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED).apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)
        Log.i(TAG, "Monitor service created")
    }

    private fun isAudioDevice(device: BluetoothDevice): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val major = device.bluetoothClass?.majorDeviceClass ?: return false
        return major == BluetoothClass.Device.Major.AUDIO_VIDEO
    }

    private fun maybeHandleDeviceConnected(device: BluetoothDevice) {
        val now = SystemClock.elapsedRealtime()
        val previous = lastHandledConnections[device.address]
        if (isDuplicateConnection(previous, now)) {
            Log.i(TAG, "Ignoring duplicate connection callback for ${device.address}")
            return
        }
        lastHandledConnections[device.address] = now
        handleDeviceConnected(device)
    }

    private fun handleDeviceConnected(device: BluetoothDevice) {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val anyDevice = prefs.getBoolean(MainActivity.PREF_ANY_DEVICE, false)
        if (anyDevice && !isAudioDevice(device)) return
        if (!anyDevice) {
            val selectedAddresses = prefs.getStringSet(MainActivity.PREF_SELECTED_DEVICES, emptySet()) ?: emptySet()
            if (selectedAddresses.isEmpty() || device.address !in selectedAddresses) return
        }

        Log.i(TAG, "Handling configured device connection: ${device.address}")
        applyEqualizer(prefs, device.address)

        val deviceKey = "${MainActivity.PREF_DEVICE_APPS_PREFIX}${device.address}"
        val deviceApps = prefs.getStringSet(deviceKey, null)
        val globalApps = prefs.getStringSet(MainActivity.PREF_SELECTED_APPS, emptySet()) ?: emptySet()
        val appPackages = MainActivity.effectiveAppSelection(deviceApps, globalApps).toList()
        val delayMs = prefs.getInt(MainActivity.PREF_LAUNCH_DELAY, 1) * 1000L

        val askKey = "${MainActivity.PREF_DEVICE_ASK_PREFIX}${device.address}"
        if (prefs.getBoolean(askKey, false)) {
            showDeviceChoices(device, appPackages)
            return
        }

        if (appPackages.isNotEmpty()) {
            launchAppsSequentially(appPackages, delayMs)
        } else {
            triggerMediaPlay()
        }
    }

    private fun applyEqualizer(prefs: SharedPreferences, address: String) {
        releaseEqualizer()

        val raw = prefs.getString("${MainActivity.PREF_DEVICE_EQ_PREFIX}$address", null) ?: return
        val levels = raw.split(",").mapNotNull { it.toShortOrNull() }
        if (levels.isEmpty()) return
        try {
            val eq = Equalizer(0, 0)
            eq.enabled = true
            for (i in 0 until minOf(levels.size, eq.numberOfBands.toInt())) {
                eq.setBandLevel(i.toShort(), levels[i])
            }
            equalizer = eq
            equalizerDeviceAddress = address
        } catch (_: Exception) {
            releaseEqualizer()
        }
    }

    private fun releaseEqualizer() {
        equalizer?.release()
        equalizer = null
        equalizerDeviceAddress = null
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceChoices(device: BluetoothDevice, appPackages: List<String>) {
        val deviceName = device.name ?: device.address
        val builder = NotificationCompat.Builder(this, ACTIONS_CHANNEL_ID)
            .setContentTitle(getString(R.string.device_connected, deviceName))
            .setContentText(getString(R.string.what_do_you_want))
            .setSmallIcon(R.drawable.ic_music_note)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

        builder.addAction(0, getString(R.string.play), choicePendingIntent(NotificationActionActivity.ACTION_PLAY, device.address, 1))

        if (appPackages.isNotEmpty()) {
            val label = if (appPackages.size == 1) {
                getString(R.string.open_app, appLabel(appPackages[0]))
            } else {
                getString(R.string.open_apps, appPackages.size)
            }
            builder.addAction(0, label, choicePendingIntent(NotificationActionActivity.ACTION_OPEN_APPS, device.address, 2))
        }

        builder.addAction(0, getString(R.string.audio_settings), choicePendingIntent(NotificationActionActivity.ACTION_AUDIO_SETTINGS, device.address, 3))

        getSystemService(NotificationManager::class.java).notify(ACTIONS_NOTIFICATION_ID, builder.build())
    }

    private fun choicePendingIntent(action: String, address: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, NotificationActionActivity::class.java)
            .setAction(action)
            .putExtra(NotificationActionActivity.EXTRA_DEVICE_ADDRESS, address)
        return PendingIntent.getActivity(
            this, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun appLabel(packageName: String): String = try {
        @Suppress("DEPRECATION")
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (_: Exception) {
        packageName
    }

    private fun launchAppsSequentially(packages: List<String>, delayMs: Long) {
        fun step(index: Int) {
            if (index >= packages.size) {
                triggerMediaPlay()
                return
            }
            launchApp(packages[index])
            handler.postDelayed({ step(index + 1) }, delayMs)
        }
        step(0)
    }

    private fun launchApp(packageName: String): Boolean {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: run {
            Log.w(TAG, "No launch intent for $packageName")
            return false
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val backgroundStartMode = if (android.os.Build.VERSION.SDK_INT >= 36) {
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                } else {
                    @Suppress("DEPRECATION")
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }
                val creatorOptions = ActivityOptions.makeBasic().apply {
                    pendingIntentCreatorBackgroundActivityStartMode = backgroundStartMode
                }
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    packageName.hashCode(),
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    creatorOptions.toBundle()
                )
                val senderOptions = ActivityOptions.makeBasic().apply {
                    pendingIntentBackgroundActivityStartMode = backgroundStartMode
                }
                pendingIntent.send(senderOptions.toBundle())
            } else {
                startActivity(launchIntent)
            }
            Log.i(TAG, "Launch requested for $packageName")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Launch failed for $packageName", e)
            false
        }
    }

    private fun triggerMediaPlay() {
        val audioManager = getSystemService(AudioManager::class.java)
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
        Log.i(TAG, "MEDIA_PLAY dispatched")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.monitor_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.monitor_channel_desc)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createLaunchNotificationChannel() {
        val channel = NotificationChannel(
            LAUNCH_CHANNEL_ID,
            getString(R.string.launch_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.launch_channel_desc)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createActionsNotificationChannel() {
        val channel = NotificationChannel(
            ACTIONS_CHANNEL_ID,
            getString(R.string.choices_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.choices_channel_desc)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.persistent_notification))
            .setSmallIcon(R.drawable.ic_music_note)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_COMPANION_CONNECTED -> {
                val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (!address.isNullOrBlank()) {
                    Log.i(TAG, "Companion service reported connected: $address")
                    handleCompanionConnection(address)
                }
            }
            ACTION_COMPANION_DISCONNECTED -> {
                val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (!address.isNullOrBlank()) {
                    Log.i(TAG, "Companion service reported disconnected: $address")
                    lastHandledConnections.remove(address)
                    if (address == equalizerDeviceAddress) releaseEqualizer()
                }
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun handleCompanionConnection(address: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Ignoring companion callback without BLUETOOTH_CONNECT")
            return
        }
        try {
            val bluetoothManager = getSystemService(BluetoothManager::class.java)
            val device = bluetoothManager.adapter?.getRemoteDevice(address) ?: return
            maybeHandleDeviceConnected(device)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Unable to resolve companion device $address", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        releaseEqualizer()
        unregisterReceiver(bluetoothReceiver)
        Log.i(TAG, "Monitor service destroyed")
    }

    companion object {
        private const val TAG = "BeatBridge"
        private const val CHANNEL_ID = "beatbridge_monitor"
        private const val LAUNCH_CHANNEL_ID = "beatbridge_launch"
        private const val ACTIONS_CHANNEL_ID = "beatbridge_device_actions"
        private const val NOTIFICATION_ID = 1
        const val ACTIONS_NOTIFICATION_ID = 2

        private const val ACTION_COMPANION_CONNECTED = "com.beatbridge.action.COMPANION_CONNECTED"
        private const val ACTION_COMPANION_DISCONNECTED = "com.beatbridge.action.COMPANION_DISCONNECTED"
        private const val EXTRA_DEVICE_ADDRESS = "device_address"
        internal const val CONNECTION_DEDUPE_MS = 3_000L

        fun companionConnectionIntent(context: Context, address: String, connected: Boolean): Intent =
            Intent(context, BluetoothMonitorService::class.java)
                .setAction(if (connected) ACTION_COMPANION_CONNECTED else ACTION_COMPANION_DISCONNECTED)
                .putExtra(EXTRA_DEVICE_ADDRESS, address)

        internal fun isDuplicateConnection(
            lastHandledAt: Long?,
            now: Long,
            windowMs: Long = CONNECTION_DEDUPE_MS,
        ): Boolean = lastHandledAt != null && now - lastHandledAt in 0 until windowMs
    }
}
