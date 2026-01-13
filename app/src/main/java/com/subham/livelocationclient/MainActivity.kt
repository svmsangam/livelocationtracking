package com.subham.livelocationclient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.livelocationclient.capture.LocationCapture
import com.google.android.gms.location.LocationServices
import com.subham.livelocationclient.debug.AppLogger
import com.subham.livelocationclient.debug.DebugLogActivity

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var locationCapture: LocationCapture? = null

    // Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                AppLogger.d(TAG, "Location permission granted")
                startLocationCapture()
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

        // App may return from background with permission already granted
        if (hasLocationPermission()) {
            startLocationCapture()
        }
    }

    override fun onStop() {
        AppLogger.d(TAG, "onStop")
        locationCapture?.stop()   // SAFE
        super.onStop()
    }

    private fun checkPermissionAndStart() {
        if (hasLocationPermission()) {
            AppLogger.d(TAG, "Permission already granted")
            startLocationCapture()
        } else {
            AppLogger.d(TAG, "Requesting location permission")
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startLocationCapture() {
        AppLogger.d(TAG, "Starting LocationCapture")
        locationCapture?.start(hasLocationPermission())
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
