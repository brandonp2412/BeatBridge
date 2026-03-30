package com.beatbridge

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for MainActivity preference key constants.
 *
 * These verify that the SharedPreferences key names are stable and do not
 * accidentally change between builds — a regression that would silently lose
 * the user's saved device selection across an update.
 */
class MainActivityPrefsTest {

    @Test
    fun prefsName_isCorrect() {
        assertEquals("beatbridge_prefs", MainActivity.PREFS_NAME)
    }

    @Test
    fun prefSelectedDeviceKey_isCorrect() {
        assertEquals("selected_device_address", MainActivity.PREF_SELECTED_DEVICE)
    }

    @Test
    fun prefSelectedNameKey_isCorrect() {
        assertEquals("selected_device_name", MainActivity.PREF_SELECTED_NAME)
    }

    @Test
    fun prefKeys_areDistinct() {
        val keys = setOf(
            MainActivity.PREF_SELECTED_DEVICE,
            MainActivity.PREF_SELECTED_NAME
        )
        assertEquals(
            "Preference keys must be unique to avoid collisions in SharedPreferences",
            2, keys.size
        )
    }
}
