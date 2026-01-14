package com.subham.livelocationclient

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.livelocationclient.capture.LocationCapture
import com.google.android.gms.location.LocationServices
import com.subham.livelocationclient.capture.service.LocationForegroundService
import com.subham.livelocationclient.debug.AppLogger
import com.subham.livelocationclient.debug.DebugLogActivity
import com.subham.livelocationclient.permission.hasLocationPermission

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var locationCapture: LocationCapture? = null

    // Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                AppLogger.d(TAG, "Location permission granted")
            } else {
                AppLogger.d(TAG, "Location permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Long-press to open debug log viewer
        findViewById<View>(android.R.id.content).setOnLongClickListener {
            startActivity(Intent(this, DebugLogActivity::class.java))
            true
        }

        AppLogger.d(TAG, "onCreate")

        // Initialize capture object (but do NOT start yet)
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        locationCapture = LocationCapture(fusedClient)

        // Start permission flow
        checkPermissionAndStart()
    }

    override fun onStart() {
        super.onStart()
        AppLogger.d(TAG, "onStart")
    }

    override fun onStop() {
        AppLogger.d(TAG, "onStop")
        super.onStop()
    }

    private fun checkPermissionAndStart() {
        if (hasLocationPermission()) {
            startLocationService()
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
}
