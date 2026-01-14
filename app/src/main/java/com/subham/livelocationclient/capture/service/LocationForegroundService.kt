package com.subham.livelocationclient.capture.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.livelocationclient.capture.LocationCapture
import com.google.android.gms.location.LocationServices
import com.subham.livelocationclient.R
import com.subham.livelocationclient.debug.AppLogger
import com.subham.livelocationclient.permission.hasLocationPermission

private const val TAG = "LocationForegroundService"

class LocationForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "live_location_tracking"
        const val NOTIFICATION_ID = 1001
    }

    private lateinit var locationCapture: LocationCapture
    override fun onCreate() {
        super.onCreate()
        AppLogger.d(TAG, "Location capture foreground service created")
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        locationCapture = LocationCapture(fusedClient)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        AppLogger.d(TAG, "Starting location capture foreground service......")
        if (!applicationContext.hasLocationPermission()) {
            AppLogger.d(
                TAG,
                "Location permission not granted. Failed to start location capture foreground service"
            )
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        AppLogger.d(TAG, "Location Notification Built Success. Staring location capture now...")
        locationCapture.start(hasLocationPermission = true)
        return START_STICKY
    }

    override fun onDestroy() {
        AppLogger.d(TAG, "Location capture foreground service stopped...")
        locationCapture.stop()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        createNotificationChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Live location active")
            .setContentText("Accessing location in real time")
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .build();
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Location",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
            AppLogger.d(TAG, "Location Notification Channel created")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}