package com.beatbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import com.beatbridge.MainActivity.Companion.PREFS_NAME
import com.beatbridge.MainActivity.Companion.PREF_SELECTED_DEVICE

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.BOOT_COMPLETED") return

        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.getString(PREF_SELECTED_DEVICE, null) ?: return

        context.startForegroundService(Intent(context, BluetoothMonitorService::class.java))
    }
}
