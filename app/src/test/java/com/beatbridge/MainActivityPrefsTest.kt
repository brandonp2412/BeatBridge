package com.beatbridge

import android.Manifest
import android.os.Build
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
    fun android13BluetoothRequirements_doNotIncludeNotificationPermission() {
        val permissions = MainActivity.requiredBluetoothPermissions(Build.VERSION_CODES.TIRAMISU)

        assertEquals(listOf(Manifest.permission.BLUETOOTH_CONNECT), permissions)
    }

    @Test
    fun preAndroid12BluetoothRequirements_includeLegacyPermissions() {
        val permissions = MainActivity.requiredBluetoothPermissions(Build.VERSION_CODES.R)

        assertEquals(
            listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN),
            permissions
        )
    }

    @Test
    fun monitoringDoesNotStartWithoutBluetoothPermission() {
        assertEquals(
            false,
            MainActivity.shouldMonitor(
                hasBluetoothPermissions = false,
                anyDevice = true,
                selectedAddresses = setOf("AA:BB:CC:DD:EE:FF"),
            )
        )
    }

    @Test
    fun monitoringStartsForConfiguredDeviceWithPermission() {
        assertEquals(
            true,
            MainActivity.shouldMonitor(
                hasBluetoothPermissions = true,
                anyDevice = false,
                selectedAddresses = setOf("AA:BB:CC:DD:EE:FF"),
            )
        )
    }

    @Test
    fun monitoringStaysStoppedWhenNothingIsConfigured() {
        assertEquals(
            false,
            MainActivity.shouldMonitor(
                hasBluetoothPermissions = true,
                anyDevice = false,
                selectedAddresses = emptySet(),
            )
        )
    }

    @Test
    fun deviceAppSelectionInheritsGlobalAppsWithoutOverride() {
        assertEquals(
            setOf("com.spotify.music"),
            MainActivity.effectiveAppSelection(
                deviceApps = null,
                globalApps = setOf("com.spotify.music"),
            )
        )
    }

    @Test
    fun explicitEmptyDeviceAppSelectionDoesNotFallBackToGlobalApps() {
        assertEquals(
            emptySet<String>(),
            MainActivity.effectiveAppSelection(
                deviceApps = emptySet(),
                globalApps = setOf("com.spotify.music"),
            )
        )
    }

    @Test
    fun appPickerDeduplicatesPackagesAndSortsNames() {
        val apps = listOf(
            MusicApp("com.example.two", "Zulu"),
            MusicApp("com.example.one", "alpha"),
            MusicApp("com.example.two", "Duplicate launcher entry"),
        )

        assertEquals(
            listOf(
                MusicApp("com.example.one", "alpha"),
                MusicApp("com.example.two", "Zulu"),
            ),
            normalizeMusicApps(apps),
        )
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
