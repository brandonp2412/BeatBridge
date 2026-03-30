package com.beatbridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.IBinder
import android.view.KeyEvent
import androidx.core.app.NotificationCompat

/**
 * Foreground service that listens for Bluetooth ACL_CONNECTED events.
 * When the user's selected device connects, it dispatches a media play key event
 * to resume playback in whatever media app was last active.
 *
 * A foreground service is used so that the dynamic BroadcastReceiver keeps
 * working after Android 8.0 restricted implicit manifest receivers.
 */
class BluetoothMonitorService : Service() {

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return

            val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            device?.let { handleDeviceConnected(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Monitoring for your device…"))
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Update notification to reflect the currently watched device name
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val name = prefs.getString(MainActivity.PREF_SELECTED_NAME, null)
        val text = if (name != null) "Waiting for $name to connect..." else "No device selected yet"
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
        return START_STICKY
    }

    private fun handleDeviceConnected(device: BluetoothDevice) {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val selectedAddress = prefs.getString(MainActivity.PREF_SELECTED_DEVICE, null) ?: return
        if (device.address == selectedAddress) {
            triggerMediaPlay()
        }
    }

    /**
     * Sends MEDIA_PLAY key events via AudioManager so the active media session
     * resumes playback (Spotify, YouTube Music, Podcast apps, etc.).
     */
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

    private fun buildNotification(contentText: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BeatBridge")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
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
        unregisterReceiver(bluetoothReceiver)
    }

    companion object {
        private const val CHANNEL_ID = "beatbridge_monitor"
        private const val NOTIFICATION_ID = 1
    }
}
