package im.angry.openeuicc.ui

import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class MainActivity : AppCompatActivity(), OpenEuiccContextMarker {
    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private lateinit var spinnerItem: MenuItem
    private lateinit var spinner: Spinner

    private val fragments = arrayListOf<EuiccManagementFragment>()

    protected lateinit var tm: TelephonyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(requireViewById(R.id.toolbar))

        supportFragmentManager.beginTransaction().replace(
            R.id.fragment_root,
            appContainer.uiComponentFactory.createNoEuiccPlaceholderFragment()
        ).commit()

        tm = telephonyManager

        spinnerAdapter = ArrayAdapter<String>(this, R.layout.spinner_item)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launch {
            init()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)

        if (!this::spinner.isInitialized) {
            spinnerItem = menu.findItem(R.id.spinner)
            spinner = spinnerItem.actionView as Spinner
            if (spinnerAdapter.isEmpty) {
                spinnerItem.isVisible = false
            }
            spinner.adapter = spinnerAdapter
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_root, fragments[position]).commit()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

            }
        } else {
            // Fragments may cause this menu to be inflated multiple times.
            // Simply reuse the action view in that case
            menu.findItem(R.id.spinner).actionView = spinner
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java));
                true
            }
        android.R.id.home -> {
            onBackPressed();
            true
        }
            else -> super.onOptionsItemSelected(item)
        }

    private suspend fun init() {
        withContext(Dispatchers.IO) {
            euiccChannelManager.enumerateEuiccChannels()
            euiccChannelManager.knownChannels.forEach {
                Log.d(TAG, "slot ${it.slotId} port ${it.portId}")
                Log.d(TAG, it.lpa.eID)
                // Request the system to refresh the list of profiles every time we start
                // Note that this is currently supposed to be no-op when unprivileged,
                // but it could change in the future
                euiccChannelManager.notifyEuiccProfilesChanged(it.logicalSlotId)
            }
        }

        withContext(Dispatchers.Main) {
            euiccChannelManager.knownChannels.sortedBy { it.logicalSlotId }.forEach { channel ->
                spinnerAdapter.add(getString(R.string.channel_name_format, channel.logicalSlotId))
                fragments.add(appContainer.uiComponentFactory.createEuiccManagementFragment(channel))
            }

            if (fragments.isNotEmpty()) {
                if (this@MainActivity::spinner.isInitialized) {
                    spinnerItem.isVisible = true
                }
                supportFragmentManager.beginTransaction().replace(R.id.fragment_root, fragments.first()).commit()
            }
        }
    }
}