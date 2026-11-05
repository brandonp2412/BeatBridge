package com.beatbridge

import java.util.Locale

/** Tracks a physical connection until a disconnect, regardless of repeated callback sources. */
internal class ConnectionSessionTracker {
    private val activeSessions = mutableMapOf<String, Long>()
    private var nextSessionId = 0L

    /** Returns a new session ID only for the first connection event. */
    fun connected(address: String): Long? {
        val key = normalize(address)
        if (key in activeSessions) return null

        val sessionId = ++nextSessionId
        activeSessions[key] = sessionId
        return sessionId
    }

    /** Disconnect invalidates delayed work for this device, even if it reconnects immediately. */
    fun disconnected(address: String) {
        activeSessions.remove(normalize(address))
    }

    fun isCurrent(address: String, sessionId: Long): Boolean =
        activeSessions[normalize(address)] == sessionId

    companion object {
        private fun normalize(address: String): String = address.uppercase(Locale.ROOT)

        /** Addresses are identities: never match a name, prefix, or another nearby device. */
        fun matchesSelectedAddress(selectedAddresses: Set<String>, address: String): Boolean =
            selectedAddresses.any { it.equals(address, ignoreCase = true) }
    }
}
