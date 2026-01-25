package com.subham.livelocationclient

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
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
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private var isTracking = false

    // Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                AppLogger.d(TAG, "Location permission granted")
                startAndBindService()
            } else {
                AppLogger.d(TAG, "Location permission denied")
                updateButtonStates(false)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        AppLogger.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationTextView = findViewById(R.id.locationTextView)
        startButton = findViewById(R.id.startTrackingButton)
        stopButton = findViewById(R.id.stopTrackingButton)

        // Long-press to open debug log viewer
        findViewById<View>(android.R.id.content).setOnLongClickListener {
            startActivity(Intent(this, DebugLogActivity::class.java))
            true
        }

        startButton.setOnClickListener {
            checkPermissionAndStart()
        }

        stopButton.setOnClickListener {
            stopTracking()
        }

        // Disable stop button initially (not tracking yet)
        updateButtonStates(false)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as LocationForegroundService.LocalBinder
            locationService = localBinder.getService()
            bound = true
            collectLocationFlow()

            if (isTracking) {
                locationService?.startTracking()
            }
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
            startAndBindService()
        } else {
            AppLogger.d(TAG, "Requesting location permission")
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startAndBindService() {
        if (!bound) {
            startLocationService()
            bindLocationService()
        }
        isTracking = true
        updateButtonStates(true)
    }

    private fun startLocationService() {
        AppLogger.d(TAG, "Starting location foreground service.....")
        val intent = Intent(this, LocationForegroundService::class.java)
        startForegroundService(intent)
    }

    private fun stopTracking() {
        if (bound) {
            locationService?.stopTracking()
        }
        isTracking = false
        updateButtonStates(false)
    }

    private fun bindLocationService() {
        val intent = Intent(this, LocationForegroundService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun collectLocationFlow() {
        try {
            lifecycleScope.launch {
                locationService?.state?.collect { state ->
                    state.derivedLocation?.let { derived ->
                        val text =
                            "Lat: ${derived.latitude}, Lon: ${derived.longitude}, Accuracy: ${derived.accuracyMeters}m, Speed: ${derived.speedMps ?: "N/A"} m/s"
                        AppLogger.d(TAG, "Received derived location: $text")
                        locationTextView.text = text
                    } ?: run {
                        locationTextView.text = "No valid location fix"
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error collecting locationFlow" + e.message)
        }
    }

    private fun updateButtonStates(isTracking: Boolean) {
        startButton.isEnabled = !isTracking
        stopButton.isEnabled = isTracking
    }
}
