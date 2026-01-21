package com.subham.livelocationclient.capture

import android.annotation.SuppressLint
import com.google.android.gms.location.*
import com.subham.livelocationclient.data.RawLocationFix
import com.subham.livelocationclient.debug.AppLogger

private const val TAG = "LocationCapture"

class LocationCapture(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val onLocationFix: (RawLocationFix) -> Unit
) {

    private var started = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val lastLocation = result.lastLocation ?: return
            val rawFix = RawLocationFix.fromLocation(lastLocation)
            onLocationFix(rawFix)

            AppLogger.d(
                TAG,
                "RawLocationFix received → " +
                        "lat=${rawFix.latitude}, " +
                        "lon=${rawFix.longitude}, " +
                        "accuracy=${rawFix.accuracyMeters}m, " +
                        "deviceTime=${rawFix.deviceTimeMs}"
            )
        }
    }

    /**
     * @param hasLocationPermission
     * true ONLY if runtime permission is already granted
     */
    @SuppressLint("MissingPermission")
    fun start(hasLocationPermission: Boolean) {
        if (!hasLocationPermission) {
            AppLogger.d(TAG, "start() ignored — location permission not granted")
            return
        }

        if (started) {
            AppLogger.d(TAG, "start() called but already ACTIVE — no-op")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            8_000L
        )
            .setMinUpdateIntervalMillis(8_000L)
            .setMaxUpdateDelayMillis(10_000L)
            .build()

        AppLogger.d(TAG, "Requesting location updates")

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null // use binder thread
        )

        started = true
        AppLogger.d(TAG, "LocationCapture state → ACTIVE")
    }

    fun stop() {
        if (!started) {
            AppLogger.d(TAG, "stop() called while IDLE — no-op")
            return
        }

        AppLogger.d(TAG, "Stopping location updates")

        fusedLocationClient.removeLocationUpdates(locationCallback)
        started = false

        AppLogger.d(TAG, "LocationCapture state → STOPPED")
    }
}
