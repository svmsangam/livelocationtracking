package com.subham.livelocationclient

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.subham.livelocationclient.capture.service.LocationForegroundService
import com.subham.livelocationclient.debug.AppLogger
import com.subham.livelocationclient.debug.DebugLogActivity
import com.subham.livelocationclient.permission.hasLocationPermission
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var locationService: LocationForegroundService? = null
    private var bound = false

    private lateinit var locationTextView: TextView

    // Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                AppLogger.d(TAG, "Location permission granted")
                startLocationService()
            } else {
                AppLogger.d(TAG, "Location permission denied")
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationTextView = findViewById(R.id.locationTextView)

        // Long-press to open debug log viewer
        findViewById<View>(android.R.id.content).setOnLongClickListener {
            startActivity(Intent(this, DebugLogActivity::class.java))
            true
        }

        AppLogger.d(TAG, "onCreate")
        // Start permission flow
        checkPermissionAndStart()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as LocationForegroundService.LocalBinder
            locationService = localBinder.getService()
            bound = true
            collectLocationFlow()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            locationService = null
        }
    }

    override fun onStart() {
        super.onStart()
        AppLogger.d(TAG, "onStart")
        if (hasLocationPermission()) {
            bindLocationService()
        }
    }

    override fun onStop() {
        AppLogger.d(TAG, "onStop")
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
        super.onStop()
    }

    private fun checkPermissionAndStart() {
        if (hasLocationPermission()) {
            startLocationService()
            bindLocationService()
        } else {
            AppLogger.d(TAG, "Requesting location permission")
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startLocationService() {
        AppLogger.d(TAG, "Starting location foreground service.....")
        val intent = Intent(this, LocationForegroundService::class.java)
        startForegroundService(intent)
    }

    private fun stopLocationService() {
        val intent = Intent(this, LocationForegroundService::class.java)
        stopService(intent)
    }

    private fun bindLocationService() {
        val intent = Intent(this, LocationForegroundService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun collectLocationFlow() {
        try {
            lifecycleScope.launch {
                locationService?.locationFlow?.collect { fix ->
                    val text =
                        "Lat: ${fix.latitude}, Lon: ${fix.longitude}, Time: ${fix.deviceTimeMs}"
                    AppLogger.d(TAG, "Received location fix: $text")
                    locationTextView.text = text
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error collecting locationFlow" + e.message)
        }
    }
}
