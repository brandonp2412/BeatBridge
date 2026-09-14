package com.beatbridge

import android.app.Activity
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper

object CompanionDeviceSupport {

    fun isSupported(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP)

    @Suppress("DEPRECATION")
    fun isAssociated(context: Context, address: String): Boolean {
        if (!isSupported(context)) return false
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        return manager.associations.any { it.equals(address, ignoreCase = true) }
    }

    fun requestAssociation(
        activity: Activity,
        address: String,
        onAssociationPending: (IntentSender) -> Unit,
        onAssociationReady: () -> Unit,
        onFailure: (CharSequence?) -> Unit,
    ) {
        if (!isSupported(activity)) {
            onFailure("Companion device support is unavailable on this phone")
            return
        }

        if (isAssociated(activity, address)) {
            startObserving(activity, address)
            onAssociationReady()
            return
        }

        val request = AssociationRequest.Builder()
            .addDeviceFilter(
                BluetoothDeviceFilter.Builder()
                    .setAddress(address)
                    .build()
            )
            .setSingleDevice(true)
            .build()

        val callback = object : CompanionDeviceManager.Callback() {
            @Suppress("DEPRECATION")
            override fun onDeviceFound(chooserLauncher: IntentSender) {
                onAssociationPending(chooserLauncher)
            }

            override fun onFailure(error: CharSequence?) {
                onFailure(error)
            }
        }

        val manager = activity.getSystemService(CompanionDeviceManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.associate(request, activity.mainExecutor, callback)
        } else {
            @Suppress("DEPRECATION")
            manager.associate(request, callback, Handler(Looper.getMainLooper()))
        }
    }

    fun finishAssociation(context: Context, address: String): Boolean {
        if (!isAssociated(context, address)) return false
        startObserving(context, address)
        return true
    }

    @Suppress("DEPRECATION")
    fun startObserving(context: Context, address: String): Boolean {
        if (!isSupported(context) || !isAssociated(context, address)) return false
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        return try {
            manager.startObservingDevicePresence(address)
            true
        } catch (_: IllegalStateException) {
            // Already observing this association.
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    @Suppress("DEPRECATION")
    fun removeAssociation(context: Context, address: String) {
        if (!isSupported(context)) return
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        try {
            try {
                manager.stopObservingDevicePresence(address)
            } catch (_: RuntimeException) {
                // Observation may already be stopped.
            }
            manager.disassociate(address)
        } catch (_: RuntimeException) {
            // The legacy monitor remains usable even if the OEM rejects CDM cleanup.
        }
    }
}
