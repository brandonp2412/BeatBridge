package com.beatbridge

import android.app.Activity
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.companion.ObservingDevicePresenceRequest
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

    fun isAssociated(context: Context, address: String): Boolean {
        if (!isSupported(context)) return false
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return manager.myAssociations.any {
                it.deviceMacAddress?.toString()?.equals(address, ignoreCase = true) == true
            }
        }
        @Suppress("DEPRECATION")
        return manager.associations.any { it.equals(address, ignoreCase = true) }
    }

    internal fun addressForAssociationId(context: Context, associationId: Int): String? {
        if (!isSupported(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        return manager.myAssociations
            .firstOrNull { it.id == associationId }
            ?.deviceMacAddress
            ?.toString()
    }

    private fun associationIdForAddress(manager: CompanionDeviceManager, address: String): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return manager.myAssociations
            .firstOrNull {
                it.deviceMacAddress?.toString()?.equals(address, ignoreCase = true) == true
            }
            ?.id
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

    fun startObserving(context: Context, address: String): Boolean {
        if (!isSupported(context) || !isAssociated(context, address)) return false
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        return try {
            if (Build.VERSION.SDK_INT >= 36) {
                val associationId = associationIdForAddress(manager, address) ?: return false
                val request = ObservingDevicePresenceRequest.Builder()
                    .setAssociationId(associationId)
                    .build()
                manager.startObservingDevicePresence(request)
            } else {
                @Suppress("DEPRECATION")
                manager.startObservingDevicePresence(address)
            }
            true
        } catch (_: IllegalStateException) {
            // Already observing this association.
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    fun removeAssociation(context: Context, address: String) {
        if (!isSupported(context)) return
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        try {
            if (Build.VERSION.SDK_INT >= 36) {
                val associationId = associationIdForAddress(manager, address) ?: return
                val request = ObservingDevicePresenceRequest.Builder()
                    .setAssociationId(associationId)
                    .build()
                try {
                    manager.stopObservingDevicePresence(request)
                } catch (_: RuntimeException) {
                    // Observation may already be stopped.
                }
                manager.disassociate(associationId)
            } else {
                @Suppress("DEPRECATION")
                try {
                    manager.stopObservingDevicePresence(address)
                } catch (_: RuntimeException) {
                    // Observation may already be stopped.
                }
                @Suppress("DEPRECATION")
                manager.disassociate(address)
            }
        } catch (_: RuntimeException) {
            // The legacy monitor remains usable even if the OEM rejects CDM cleanup.
        }
    }
}
