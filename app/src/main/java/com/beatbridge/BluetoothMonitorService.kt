package com.beatbridge

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.KeyEvent
import androidx.core.app.NotificationCompat

class BluetoothMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var equalizer: Equalizer? = null

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> device?.let { handleDeviceConnected(it) }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> releaseEqualizer()
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
    }

    private fun isAudioDevice(device: BluetoothDevice): Boolean {
        val major = device.bluetoothClass?.majorDeviceClass ?: return false
        return major == BluetoothClass.Device.Major.AUDIO_VIDEO
    }

    private fun handleDeviceConnected(device: BluetoothDevice) {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val anyDevice = prefs.getBoolean(MainActivity.PREF_ANY_DEVICE, false)
        if (anyDevice) {
            if (!isAudioDevice(device)) return
        } else {
            val selectedAddresses = prefs.getStringSet(MainActivity.PREF_SELECTED_DEVICES, emptySet()) ?: emptySet()
            if (selectedAddresses.isEmpty() || device.address !in selectedAddresses) return
        }

        applyEqualizer(prefs, device.address)

        val deviceKey = "${MainActivity.PREF_DEVICE_APPS_PREFIX}${device.address}"
        val deviceApps = prefs.getStringSet(deviceKey, null)
        val appPackages = (deviceApps ?: prefs.getStringSet(MainActivity.PREF_SELECTED_APPS, emptySet()) ?: emptySet()).toList()
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
        val raw = prefs.getString("${MainActivity.PREF_DEVICE_EQ_PREFIX}$address", null) ?: return
        val levels = raw.split(",").mapNotNull { it.toShortOrNull() }
        if (levels.isEmpty()) return
        try {
            equalizer?.release()
            val eq = Equalizer(0, 0)
            eq.enabled = true
            for (i in 0 until minOf(levels.size, eq.numberOfBands.toInt())) {
                eq.setBandLevel(i.toShort(), levels[i])
            }
            equalizer = eq
        } catch (_: Exception) {
            equalizer = null
        }
    }

    private fun releaseEqualizer() {
        equalizer?.release()
        equalizer = null
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceChoices(device: BluetoothDevice, appPackages: List<String>) {
        val deviceName = device.name ?: device.address
        val builder = NotificationCompat.Builder(this, ACTIONS_CHANNEL_ID)
            .setContentTitle("$deviceName connected")
            .setContentText("What do you want to do?")
            .setSmallIcon(R.drawable.ic_music_note)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

        builder.addAction(0, "Play", choicePendingIntent(NotificationActionActivity.ACTION_PLAY, device.address, 1))

        if (appPackages.isNotEmpty()) {
            val label = if (appPackages.size == 1) {
                "Open ${appLabel(appPackages[0])}"
            } else {
                "Open ${appPackages.size} apps"
            }
            builder.addAction(0, label, choicePendingIntent(NotificationActionActivity.ACTION_OPEN_APPS, device.address, 2))
        }

        builder.addAction(0, "Audio settings", choicePendingIntent(NotificationActionActivity.ACTION_AUDIO_SETTINGS, device.address, 3))

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
            val launchIntent = packageManager.getLaunchIntentForPackage(packages[index])
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            }
            handler.postDelayed({ step(index + 1) }, delayMs)
        }
        step(0)
    }

    private fun triggerMediaPlay() {
        val audioManager = getSystemService(AudioManager::class.java)
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BeatBridge Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Running in the background to detect your paired device"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createLaunchNotificationChannel() {
        val channel = NotificationChannel(
            LAUNCH_CHANNEL_ID,
            "BeatBridge App Launch",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Briefly shown when launching your music app"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createActionsNotificationChannel() {
        val channel = NotificationChannel(
            ACTIONS_CHANNEL_ID,
            "BeatBridge Device Choices",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shown when a device connects so you can pick what happens"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BeatBridge")
            .setContentText("Persistent notification")
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        releaseEqualizer()
        unregisterReceiver(bluetoothReceiver)
    }

    companion object {
        private const val CHANNEL_ID = "beatbridge_monitor"
        private const val LAUNCH_CHANNEL_ID = "beatbridge_launch"
        private const val ACTIONS_CHANNEL_ID = "beatbridge_device_actions"
        private const val NOTIFICATION_ID = 1
        const val ACTIONS_NOTIFICATION_ID = 2
    }
}
