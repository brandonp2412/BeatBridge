package com.beatbridge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothMonitorServiceTest {

    @Test
    fun firstConnectionIsNeverDuplicate() {
        assertFalse(BluetoothMonitorService.isDuplicateConnection(null, now = 10_000L))
    }

    @Test
    fun duplicateConnectionInsideWindowIsIgnored() {
        assertTrue(
            BluetoothMonitorService.isDuplicateConnection(
                lastHandledAt = 10_000L,
                now = 10_000L + BluetoothMonitorService.CONNECTION_DEDUPE_MS - 1,
            )
        )
    }

    @Test
    fun connectionAtEndOfWindowIsHandledAgain() {
        assertFalse(
            BluetoothMonitorService.isDuplicateConnection(
                lastHandledAt = 10_000L,
                now = 10_000L + BluetoothMonitorService.CONNECTION_DEDUPE_MS,
            )
        )
    }
}
