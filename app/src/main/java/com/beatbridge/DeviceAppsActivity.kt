package com.beatbridge

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.beatbridge.databinding.ActivityDeviceAppsBinding

class DeviceAppsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceAppsBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var appAdapter: AppAdapter
    private lateinit var prefKey: String
    private val appList = mutableListOf<MusicApp>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS) ?: run { finish(); return }
        val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: deviceAddress
        prefKey = "${MainActivity.PREF_DEVICE_APPS_PREFIX}$deviceAddress"

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = deviceName
        supportActionBar?.subtitle = "Per-device settings"

        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)

        val askKey = "${MainActivity.PREF_DEVICE_ASK_PREFIX}$deviceAddress"
        binding.switchAsk.isChecked = prefs.getBoolean(askKey, false)
        binding.switchAsk.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(askKey, isChecked) }
        }

        binding.rowEqualizer.setOnClickListener {
            startActivity(Intent(this, DeviceEqualizerActivity::class.java).apply {
                putExtra(DeviceEqualizerActivity.EXTRA_DEVICE_ADDRESS, deviceAddress)
                putExtra(DeviceEqualizerActivity.EXTRA_DEVICE_NAME, deviceName)
            })
        }

        loadApps()
        setupRecyclerView()
        setupSearch()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadApps() {
        val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val found = packageManager.queryIntentActivities(launchIntent, 0)
            .mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(packageManager).toString()
                if (pkg == packageName) null else MusicApp(pkg, label)
            }
            .sortedBy { it.appName }
        appList.addAll(found)
    }

    private fun setupRecyclerView() {
        val savedApps = prefs.getStringSet(prefKey, emptySet()) ?: emptySet()
        appAdapter = AppAdapter(
            apps = appList,
            selectedPackages = savedApps,
            onSelect = { app -> toggleApp(app) }
        )
        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(this@DeviceAppsActivity)
            adapter = appAdapter
            addItemDecoration(DividerItemDecoration(this@DeviceAppsActivity, DividerItemDecoration.VERTICAL))
        }
    }

    private fun toggleApp(app: MusicApp) {
        val current = LinkedHashSet(prefs.getStringSet(prefKey, emptySet()) ?: emptySet())
        if (app.packageName in current) {
            current.remove(app.packageName)
        } else {
            current.add(app.packageName)
        }
        if (current.isEmpty()) {
            prefs.edit { remove(prefKey) }
        } else {
            prefs.edit { putStringSet(prefKey, current) }
        }
        appAdapter.updateSelections(current)
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                appAdapter.filter(s?.toString() ?: "")
            }
        })
    }

    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_DEVICE_NAME = "device_name"
    }
}
