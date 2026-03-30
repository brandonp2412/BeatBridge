package com.beatbridge

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Automated screenshot capture for the F-Droid / Play Store listing.
 *
 * Four screenshots cover every meaningful UI state:
 *   01_empty_state      – no paired devices found
 *   02_devices_found    – a few devices, none selected
 *   03_device_selected  – one device selected (checkmark + "Watching" header)
 *   04_full_list        – full realistic device list with selection
 *
 * A [FakeDeviceAdapter] swaps into the RecyclerView so screenshots look
 * realistic without needing Mockito or specific paired hardware.
 *
 * Run:  fastlane screenshots
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val uiDevice = UiDevice.getInstance(instrumentation)
    private val outputDir by lazy {
        File(context.getExternalFilesDir(null), "screenshots").also { it.mkdirs() }
    }

    // ── Scenarios ─────────────────────────────────────────────────────────────

    @Test
    fun screenshot_01_empty_state() {
        capture(
            filename          = "01_empty_state",
            devices           = emptyList(),
            selectedAddress   = null,
        )
    }

    @Test
    fun screenshot_02_devices_found() {
        capture(
            filename        = "02_devices_found",
            devices         = listOf(
                "Car Audio BT"  to "00:1A:7D:DA:71:13",
                "JBL Charge 5"  to "E8:EE:CC:86:01:30",
                "UGREEN-40759"  to "00:02:5B:01:BE:2C",
            ),
            selectedAddress = null,
        )
    }

    @Test
    fun screenshot_03_device_selected() {
        capture(
            filename        = "03_device_selected",
            devices         = listOf(
                "Car Audio BT"  to "00:1A:7D:DA:71:13",
                "JBL Charge 5"  to "E8:EE:CC:86:01:30",
                "UGREEN-40759"  to "00:02:5B:01:BE:2C",
            ),
            selectedAddress = "00:1A:7D:DA:71:13",
            selectedName    = "Car Audio BT",
        )
    }

    @Test
    fun screenshot_04_full_list() {
        capture(
            filename        = "04_full_list",
            devices         = listOf(
                "AirPods Pro"       to "B8:D7:AF:69:CC:10",
                "Car Audio BT"      to "00:1A:7D:DA:71:13",
                "Galaxy Buds2 Pro"  to "F0:6D:8F:88:DE:20",
                "JBL Charge 5"      to "E8:EE:CC:86:01:30",
                "Sony WH-1000XM5"   to "A4:77:33:11:BB:10",
                "UGREEN-40759"      to "00:02:5B:01:BE:2C",
            ),
            selectedAddress = "A4:77:33:11:BB:10",
            selectedName    = "Sony WH-1000XM5",
        )
    }

    // ── Engine ────────────────────────────────────────────────────────────────

    private fun capture(
        filename: String,
        devices: List<Pair<String, String>>,
        selectedAddress: String?,
        selectedName: String? = null,
    ) {
        assumeTrue(
            "Skipped: no Bluetooth adapter (activity finishes immediately)",
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        )

        // Pre-seed prefs so the header renders the right text on first draw
        context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(MainActivity.PREF_SELECTED_DEVICE, selectedAddress)
            .putString(MainActivity.PREF_SELECTED_NAME, selectedName)
            .commit()

        uiDevice.wakeUp()
        Thread.sleep(300)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Show activity over the lock screen without needing to unlock
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    activity.setShowWhenLocked(true)
                    activity.setTurnScreenOn(true)
                } else {
                    @Suppress("DEPRECATION")
                    activity.window.addFlags(
                        android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    )
                }

                val rvDevices = activity.findViewById<RecyclerView>(R.id.rv_devices)
                val tvEmpty   = activity.findViewById<View>(R.id.tv_empty)
                val tvStatus  = activity.findViewById<TextView>(R.id.tv_status)

                if (devices.isEmpty()) {
                    rvDevices.visibility = View.GONE
                    tvEmpty.visibility   = View.VISIBLE
                    tvStatus.text        = "Tap a device below to activate auto-play"
                } else {
                    tvEmpty.visibility   = View.GONE
                    rvDevices.visibility = View.VISIBLE
                    // Swap in the fake adapter so we display our custom device list
                    rvDevices.adapter    = FakeDeviceAdapter(devices, selectedAddress)
                    tvStatus.text = if (selectedName != null) "Watching: $selectedName"
                                    else "Tap a device below to activate auto-play"
                }
            }

            Thread.sleep(700) // let layout settle
            uiDevice.takeScreenshot(File(outputDir, "$filename.png"), 1.0f, 100)
        }
    }
}
