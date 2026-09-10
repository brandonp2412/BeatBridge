package com.beatbridge

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.beatbridge.MainActivity.Companion.PREFS_NAME
import com.beatbridge.MainActivity.Companion.PREF_SELECTED_DEVICES
import com.beatbridge.MainActivity.Companion.PREF_ANY_DEVICE

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.BOOT_COMPLETED") return

        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val selectedDevices = prefs.getStringSet(PREF_SELECTED_DEVICES, emptySet()) ?: emptySet()
        val anyDevice = prefs.getBoolean(PREF_ANY_DEVICE, false)
        val hasBluetoothPermissions = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
        if (!MainActivity.shouldMonitor(hasBluetoothPermissions, anyDevice, selectedDevices)) return

        context.startForegroundService(Intent(context, BluetoothMonitorService::class.java))
    }
}
