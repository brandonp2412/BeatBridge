package com.beatbridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothMonitorServiceTest {
    private val selectedAddress = "AA:BB:CC:DD:EE:FF"

    private enum class AndroidEvent {
        ACL_CONNECTED,
        ACL_DISCONNECTED,
        COMPANION_CONNECTED,
        COMPANION_DISCONNECTED,
    }

    private data class Broadcast(val type: AndroidEvent, val address: String)

    /** Exercise both Android callback paths against the same connection state. */
    private fun acceptedConnections(events: List<Broadcast>): Int {
        val tracker = ConnectionSessionTracker()
        var accepted = 0
        for (event in events) {
            when (event.type) {
                AndroidEvent.ACL_CONNECTED, AndroidEvent.COMPANION_CONNECTED ->
                    if (tracker.connected(event.address) != null) accepted++
                AndroidEvent.ACL_DISCONNECTED, AndroidEvent.COMPANION_DISCONNECTED ->
                    tracker.disconnected(event.address)
            }
        }
        return accepted
    }

    @Test
    fun repeatedAclAndCompanionConnectionBroadcastsDispatchOnlyOnce() {
        assertEquals(
            1,
            acceptedConnections(
                listOf(
                    Broadcast(AndroidEvent.ACL_CONNECTED, selectedAddress),
                    Broadcast(AndroidEvent.COMPANION_CONNECTED, selectedAddress.lowercase()),
                    Broadcast(AndroidEvent.ACL_CONNECTED, selectedAddress),
                    // A duplicate sent long after the old three-second timeout stays ignored.
                    Broadcast(AndroidEvent.COMPANION_CONNECTED, selectedAddress),
                )
            )
        )
    }

    @Test
    fun quickDisconnectReconnectCreatesExactlyOneNewSession() {
        assertEquals(
            2,
            acceptedConnections(
                listOf(
                    Broadcast(AndroidEvent.ACL_CONNECTED, selectedAddress),
                    Broadcast(AndroidEvent.COMPANION_DISCONNECTED, selectedAddress),
                    Broadcast(AndroidEvent.COMPANION_CONNECTED, selectedAddress),
                    Broadcast(AndroidEvent.ACL_CONNECTED, selectedAddress),
                )
            )
        )
    }

    @Test
    fun delayedPlaybackFromOldConnectionCannotSurviveReconnect() {
        val tracker = ConnectionSessionTracker()
        val oldSession = tracker.connected(selectedAddress)
        assertNotNull(oldSession)
        assertTrue(tracker.isCurrent(selectedAddress, oldSession!!))

        tracker.disconnected(selectedAddress)
        assertFalse(tracker.isCurrent(selectedAddress, oldSession))
        val newSession = tracker.connected(selectedAddress)
        assertNotNull(newSession)
        assertNotEquals(oldSession, newSession)
        assertFalse(tracker.isCurrent(selectedAddress, oldSession))
        assertTrue(tracker.isCurrent(selectedAddress, newSession!!))
    }

    @Test
    fun unrelatedDeviceDisconnectDoesNotCancelActivePlaybackSequence() {
        val tracker = ConnectionSessionTracker()
        val selectedSession = tracker.connected(selectedAddress)!!
        tracker.connected("11:22:33:44:55:66")
        tracker.disconnected("11:22:33:44:55:66")
        assertTrue(tracker.isCurrent(selectedAddress, selectedSession))
        assertNull(tracker.connected(selectedAddress))
    }

    @Test
    fun duplicateDisconnectEventsDoNotCreateExtraSessions() {
        val tracker = ConnectionSessionTracker()
        tracker.connected(selectedAddress)
        tracker.disconnected(selectedAddress)
        tracker.disconnected(selectedAddress)
        assertNotNull(tracker.connected(selectedAddress))
        assertNull(tracker.connected(selectedAddress))
    }

    @Test
    fun matchingUsesCompleteMacAddressAndIgnoresCase() {
        val selected = setOf(selectedAddress.lowercase())
        assertTrue(ConnectionSessionTracker.matchesSelectedAddress(selected, selectedAddress))
        assertFalse(ConnectionSessionTracker.matchesSelectedAddress(selected, "AA:BB:CC:DD:EE:00"))
        assertFalse(ConnectionSessionTracker.matchesSelectedAddress(selected, "AA:BB:CC:DD:EE"))
        assertFalse(ConnectionSessionTracker.matchesSelectedAddress(emptySet(), selectedAddress))
    }
}
