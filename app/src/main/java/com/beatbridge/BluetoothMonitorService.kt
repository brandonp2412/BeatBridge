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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.KeyEvent
import androidx.core.app.NotificationCompat

/**
 * Foreground service that listens for Bluetooth ACL_CONNECTED events.
 * When the user's selected device connects, it optionally launches the user's
 * chosen music app and then dispatches a media play key event.
 */
class BluetoothMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())

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
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        createLaunchNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED).apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun handleDeviceConnected(device: BluetoothDevice) {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val selectedAddress = prefs.getString(MainActivity.PREF_SELECTED_DEVICE, null) ?: return
        if (device.address != selectedAddress) return

        val appPackage = prefs.getString(MainActivity.PREF_SELECTED_APP, null)
        if (appPackage != null) {
            launchAppThenPlay(appPackage)
        } else {
            triggerMediaPlay()
        }
    }

    private fun launchAppThenPlay(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: run {
            triggerMediaPlay()
            return
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launchIntent)

        handler.postDelayed({ triggerMediaPlay() }, LAUNCH_DELAY_MS)
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
        unregisterReceiver(bluetoothReceiver)
    }

    companion object {
        private const val CHANNEL_ID = "beatbridge_monitor"
        private const val LAUNCH_CHANNEL_ID = "beatbridge_launch"
        private const val NOTIFICATION_ID = 1
        private const val LAUNCH_NOTIFICATION_ID = 2
        private const val REQUEST_CODE_LAUNCH = 1
        private const val LAUNCH_DELAY_MS = 1000L
    }
}
