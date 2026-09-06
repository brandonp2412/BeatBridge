package com.beatbridge

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for BeatBridge UI.
 *
 * These run on a real device or emulator. Tests that require Bluetooth hardware
 * are skipped automatically when running on emulators that lack it.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    @Test
    fun appContext_hasCorrectPackageName() {
        assertEquals("com.beatbridge", context.packageName)
    }

    @Test
    fun mainActivity_launchesWithoutCrash() {
        // Skip on devices/emulators that have no Bluetooth adapter — MainActivity
        // calls finish() immediately in that case, which is correct behaviour but
        // makes UI assertions meaningless.
        val hasBluetooth = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        assumeTrue("Skipped: device has no Bluetooth hardware", hasBluetooth)

        ActivityScenario.launch(MainActivity::class.java).use { }
    }

    @Test
    fun defaultStatus_isDisplayed() {
        val hasBluetooth = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        assumeTrue("Skipped: device has no Bluetooth hardware", hasBluetooth)
        context.getSharedPreferences(MainActivity.PREFS_NAME, 0).edit().clear().commit()

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Tap a device below to activate auto-play"))
                .check(matches(isDisplayed()))
        }
    }
}
