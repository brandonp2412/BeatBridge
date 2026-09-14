package com.beatbridge

import android.companion.CompanionDeviceService
import android.companion.DevicePresenceEvent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

@RequiresApi(Build.VERSION_CODES.S)
class BeatBridgeCompanionService : CompanionDeviceService() {

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onDeviceAppeared(address: String) {
        dispatchConnection(address, connected = true)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onDeviceDisappeared(address: String) {
        dispatchConnection(address, connected = false)
    }

    @RequiresApi(36)
    override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
        val address = CompanionDeviceSupport.addressForAssociationId(this, event.associationId)
        if (address == null) {
            Log.w(TAG, "Companion presence event has no matching association: ${event.associationId}")
            return
        }

        when (event.event) {
            DevicePresenceEvent.EVENT_BT_CONNECTED -> dispatchConnection(address, connected = true)
            DevicePresenceEvent.EVENT_BT_DISCONNECTED -> dispatchConnection(address, connected = false)
            else -> Log.d(TAG, "Ignoring non-connection companion presence event ${event.event}: $address")
        }
    }

    private fun dispatchConnection(address: String, connected: Boolean) {
        ContextCompat.startForegroundService(
            this,
            BluetoothMonitorService.companionConnectionIntent(this, address, connected),
        )
    }

    private companion object {
        const val TAG = "BeatBridge"
    }
}
