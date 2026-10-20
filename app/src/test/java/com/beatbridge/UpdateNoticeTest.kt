package com.beatbridge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateNoticeTest {
    @Test fun onlyUpgradesNotify() {
        assertFalse(UpdateNotice.shouldShow(null, 2, null, 100))
        assertFalse(UpdateNotice.shouldShow(2, 2, null, 100))
        assertFalse(UpdateNotice.shouldShow(3, 2, null, 100))
        assertTrue(UpdateNotice.shouldShow(1, 2, null, 100))
    }

    @Test fun notificationsNeverRepeatBeforeFourteenDays() {
        val start = 1_000_000_000_000
        assertFalse(UpdateNotice.shouldShow(1, 2, start, start + UpdateNotice.COOLDOWN_MS - 1))
        assertTrue(UpdateNotice.shouldShow(2, 3, start, start + UpdateNotice.COOLDOWN_MS))
        assertFalse(UpdateNotice.shouldShow(2, 3, start, start - 1))
    }
}
