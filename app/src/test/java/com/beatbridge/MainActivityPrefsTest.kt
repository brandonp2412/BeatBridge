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
    fun prefDeviceAskPrefix_isCorrect() {
        assertEquals("device_ask_", MainActivity.PREF_DEVICE_ASK_PREFIX)
    }

    @Test
    fun prefDeviceEqPrefix_isCorrect() {
        assertEquals("device_eq_", MainActivity.PREF_DEVICE_EQ_PREFIX)
    }

    @Test
    fun prefKeys_areDistinct() {
        val keys = setOf(
            MainActivity.PREF_SELECTED_DEVICES,
            MainActivity.PREF_SELECTED_APPS,
            MainActivity.PREF_ANY_DEVICE,
            MainActivity.PREF_LAUNCH_DELAY,
            MainActivity.PREF_DEVICE_APPS_PREFIX,
            MainActivity.PREF_DEVICE_ASK_PREFIX,
            MainActivity.PREF_DEVICE_EQ_PREFIX
        )
        assertEquals(
            "Preference keys must be unique to avoid collisions in SharedPreferences",
            7, keys.size
        )
    }
}
