package com.beatbridge

import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.beatbridge.databinding.ActivityDeviceEqualizerBinding

class DeviceEqualizerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceEqualizerBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var prefKey: String
    private var equalizer: Equalizer? = null
    private val bandSeekBars = mutableListOf<SeekBar>()
    private var minLevel: Short = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceEqualizerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS) ?: run { finish(); return }
        val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: deviceAddress
        prefKey = "${MainActivity.PREF_DEVICE_EQ_PREFIX}$deviceAddress"

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = deviceName
        supportActionBar?.subtitle = "Equalizer"

        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)

        val eq = try { Equalizer(0, 0) } catch (_: Exception) { null }
        if (eq == null) {
            binding.tvUnsupported.visibility = View.VISIBLE
            return
        }
        equalizer = eq
        eq.enabled = true

        val range = eq.getBandLevelRange()
        minLevel = range[0]
        val maxLevel = range[1]
        val saved = prefs.getString(prefKey, null)
            ?.split(",")?.mapNotNull { it.toShortOrNull() }

        binding.btnReset.setOnClickListener {
            bandSeekBars.forEach { it.progress = (0 - minLevel).toInt() }
        }

        for (band in 0 until eq.numberOfBands) {
            addBandRow(eq, band.toShort(), minLevel, maxLevel, saved?.getOrNull(band))
        }
    }

    private fun addBandRow(eq: Equalizer, band: Short, min: Short, max: Short, savedLevel: Short?) {
        val level = savedLevel ?: eq.getBandLevel(band)
        val freqHz = eq.getCenterFreq(band) / 1000
        val freqLabel = if (freqHz >= 1000) "${freqHz / 1000} kHz" else "$freqHz Hz"

        val title = TextView(this).apply {
            text = freqLabel
            setTextColor(getColor(R.color.bb_on_surface))
            textSize = 14f
        }
        val value = TextView(this).apply {
            setTextColor(getColor(R.color.bb_on_surface_dim))
            textSize = 12f
        }
        val seek = SeekBar(this).apply {
            this.max = (max - min).toInt()
            progress = (level - min).toInt()
        }

        fun updateValue() {
            val db = (seek.progress + min) / 100f
            value.text = "%+.1f dB".format(db)
        }
        updateValue()

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val lvl = (progress + min).toShort()
                try { eq.setBandLevel(band, lvl) } catch (_: Exception) {}
                updateValue()
                saveLevels()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)
            addView(title)
            addView(value)
            addView(seek)
        }
        bandSeekBars.add(seek)
        binding.layoutBands.addView(row)
    }

    private fun saveLevels() {
        val levels = bandSeekBars.joinToString(",") { (it.progress + minLevel).toString() }
        prefs.edit { putString(prefKey, levels) }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        equalizer?.release()
    }

    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_DEVICE_NAME = "device_name"
    }
}
