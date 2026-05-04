package com.beatbridge

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityPrefsTest {

    @Test
    fun prefsName_isCorrect() {
        assertEquals("beatbridge_prefs", MainActivity.PREFS_NAME)
    }

    @Test
    fun prefSelectedDevicesKey_isCorrect() {
        assertEquals("selected_device_addresses", MainActivity.PREF_SELECTED_DEVICES)
    }

    @Test
    fun prefSelectedAppsKey_isCorrect() {
        assertEquals("selected_app_packages", MainActivity.PREF_SELECTED_APPS)
    }

    @Test
    fun prefLaunchDelayKey_isCorrect() {
        assertEquals("launch_delay_seconds", MainActivity.PREF_LAUNCH_DELAY)
    }

    @Test
    fun prefKeys_areDistinct() {
        val keys = setOf(
            MainActivity.PREF_SELECTED_DEVICES,
            MainActivity.PREF_SELECTED_APPS,
            MainActivity.PREF_ANY_DEVICE,
            MainActivity.PREF_LAUNCH_DELAY
        )
        assertEquals(
            "Preference keys must be unique to avoid collisions in SharedPreferences",
            4, keys.size
        )
    }
}
