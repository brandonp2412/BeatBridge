package com.beatbridge

import android.app.ForegroundServiceStartNotAllowedException
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class ForegroundStartInstrumentedTest {
    private val target = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun companionCallbacksUseAndroidBluetoothAddressCasing() {
        for (connected in listOf(true, false)) {
            val intent = BluetoothMonitorService.companionConnectionIntent(
                target,
                "aa:bb:cc:dd:ee:ff",
                connected,
            )
            assertEquals("AA:BB:CC:DD:EE:FF", intent.getStringExtra("device_address"))
        }
    }

    @Test
    fun allowedStartForwardsTheIntent() {
        val intent = Intent(target, BluetoothMonitorService::class.java)
        var forwarded: Intent? = null
        val context = object : ContextWrapper(target) {
            override fun startForegroundService(service: Intent): ComponentName {
                forwarded = service
                return ComponentName(target, BluetoothMonitorService::class.java)
            }
        }

        assertTrue(BluetoothMonitorService.startIfAllowed(context, intent))
        assertSame(intent, forwarded)
    }

    @Test
    fun androidBackgroundRejectionDoesNotCrashTheCaller() {
        val context = object : ContextWrapper(target) {
            override fun startForegroundService(service: Intent): ComponentName {
                throw ForegroundServiceStartNotAllowedException("Background start denied")
            }
        }

        assertFalse(
            BluetoothMonitorService.startIfAllowed(
                context,
                Intent(target, BluetoothMonitorService::class.java),
            ),
        )
    }

    @Test(expected = SecurityException::class)
    fun missingPermissionsAreNotHidden() {
        val context = object : ContextWrapper(target) {
            override fun startForegroundService(service: Intent): ComponentName {
                throw SecurityException("Missing permission")
            }
        }

        BluetoothMonitorService.startIfAllowed(
            context,
            Intent(target, BluetoothMonitorService::class.java),
        )
    }
}
