package com.beatbridge

import android.companion.CompanionDeviceService
import androidx.core.content.ContextCompat

class BeatBridgeCompanionService : CompanionDeviceService() {

    @Suppress("DEPRECATION")
    override fun onDeviceAppeared(address: String) {
        ContextCompat.startForegroundService(
            this,
            BluetoothMonitorService.companionConnectionIntent(this, address, connected = true),
        )
    }

    @Suppress("DEPRECATION")
    override fun onDeviceDisappeared(address: String) {
        ContextCompat.startForegroundService(
            this,
            BluetoothMonitorService.companionConnectionIntent(this, address, connected = false),
        )
    }
}
