package com.beatbridge

import android.app.Activity
import android.companion.AssociationInfo
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.myAssociations.any { associationAddress(it).equals(address, ignoreCase = true) }
        } else {
            @Suppress("DEPRECATION")
            manager.associations.any { it.equals(address, ignoreCase = true) }
        }
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
            override fun onAssociationPending(intentSender: IntentSender) {
                onAssociationPending(intentSender)
            }

            @Suppress("DEPRECATION")
            override fun onDeviceFound(chooserLauncher: IntentSender) {
                onAssociationPending(chooserLauncher)
            }

            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                startObserving(activity, address)
                onAssociationReady()
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
                val association = manager.myAssociations.firstOrNull {
                    associationAddress(it).equals(address, ignoreCase = true)
                } ?: return false
                val request = ObservingDevicePresenceRequest.Builder()
                    .setAssociationId(association.id)
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
                manager.myAssociations.firstOrNull {
                    associationAddress(it).equals(address, ignoreCase = true)
                }?.let { association ->
                    val request = ObservingDevicePresenceRequest.Builder()
                        .setAssociationId(association.id)
                        .build()
                    try {
                        manager.stopObservingDevicePresence(request)
                    } catch (_: RuntimeException) {
                        // Observation may already be stopped.
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                @Suppress("DEPRECATION")
                try {
                    manager.stopObservingDevicePresence(address)
                } catch (_: RuntimeException) {
                    // Observation may already be stopped.
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                manager.myAssociations.firstOrNull {
                    associationAddress(it).equals(address, ignoreCase = true)
                }?.let { manager.disassociate(it.id) }
            } else {
                @Suppress("DEPRECATION")
                manager.disassociate(address)
            }
        } catch (_: RuntimeException) {
            // The legacy monitor remains usable even if the OEM rejects CDM cleanup.
        }
    }

    fun addressForAssociationId(context: Context, associationId: Int): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        return manager.myAssociations.firstOrNull { it.id == associationId }
            ?.let(::associationAddress)
    }

    private fun associationAddress(info: AssociationInfo): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            info.deviceMacAddress?.toString()
        } else {
            null
        }
}
