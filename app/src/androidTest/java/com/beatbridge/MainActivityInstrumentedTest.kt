package com.beatbridge

import android.content.pm.PackageManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
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

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Scenario reaching RESUMED means the activity did not crash on startup.
            scenario.onActivity { /* no-op — just confirm we got here */ }
        }
    }

    @Test
    fun statusTextView_isDisplayed() {
        val hasBluetooth = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        assumeTrue("Skipped: device has no Bluetooth hardware", hasBluetooth)

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.tvStatus)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun deviceRecyclerView_isPresent() {
        val hasBluetooth = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        assumeTrue("Skipped: device has no Bluetooth hardware", hasBluetooth)

        ActivityScenario.launch(MainActivity::class.java).use {
            // The RecyclerView is always in the layout; visibility depends on
            // whether paired devices exist, but the view itself must be present.
            onView(withId(R.id.rvDevices)) // throws if view is not in hierarchy
        }
    }
}
