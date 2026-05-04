package com.beatbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import com.beatbridge.MainActivity.Companion.PREFS_NAME
import com.beatbridge.MainActivity.Companion.PREF_SELECTED_DEVICES
import com.beatbridge.MainActivity.Companion.PREF_ANY_DEVICE

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.BOOT_COMPLETED") return

        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val hasDevices = prefs.getStringSet(PREF_SELECTED_DEVICES, null)?.isNotEmpty() == true
        val anyDevice = prefs.getBoolean(PREF_ANY_DEVICE, false)
        if (!hasDevices && !anyDevice) return

        context.startForegroundService(Intent(context, BluetoothMonitorService::class.java))
    }
}
