package com.beatbridge

import android.app.NotificationManager
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity

class NotificationActionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent.action) {
            ACTION_PLAY -> dispatchMediaPlay()
            ACTION_OPEN_APPS -> launchConfiguredApps()
            ACTION_AUDIO_SETTINGS -> startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
        }

        getSystemService(NotificationManager::class.java)
            .cancel(BluetoothMonitorService.ACTIONS_NOTIFICATION_ID)
        finish()
    }

    private fun dispatchMediaPlay() {
        val audioManager = getSystemService(AudioManager::class.java)
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
    }

    private fun launchConfiguredApps() {
        val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val deviceApps = address?.let {
            prefs.getStringSet("${MainActivity.PREF_DEVICE_APPS_PREFIX}$it", null)
        }
        val packages = deviceApps ?: prefs.getStringSet(MainActivity.PREF_SELECTED_APPS, emptySet()) ?: emptySet()
        for (pkg in packages) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg) ?: continue
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        }
    }

    companion object {
        const val ACTION_PLAY = "com.beatbridge.action.PLAY"
        const val ACTION_OPEN_APPS = "com.beatbridge.action.OPEN_APPS"
        const val ACTION_AUDIO_SETTINGS = "com.beatbridge.action.AUDIO_SETTINGS"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }
}
