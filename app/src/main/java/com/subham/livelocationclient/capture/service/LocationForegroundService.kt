package com.subham.livelocationclient.capture.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.livelocationclient.capture.LocationCapture
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.subham.livelocationclient.R
import com.subham.livelocationclient.capture.RawLocationFix
import com.subham.livelocationclient.debug.AppLogger
import com.subham.livelocationclient.permission.hasLocationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

private const val TAG = "LocationForegroundService"

class LocationForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "live_location_tracking"
        const val NOTIFICATION_ID = 1001
    }

    private val _locationFlow = MutableSharedFlow<RawLocationFix>(
        replay = 1, // Keeps last emitted item for new subscribers
        extraBufferCapacity = 5, // small buffer for bursts
        onBufferOverflow = BufferOverflow.DROP_OLDEST // drop oldest if overflow
    )

    val locationFlow: SharedFlow<RawLocationFix> = _locationFlow

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        AppLogger.d(TAG, "Location capture foreground service created")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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
        startLocationUpdates()
        return START_STICKY
    }

    private val locationCapture by lazy {
        LocationCapture(fusedLocationClient) { fix ->
            serviceScope.launch {
                _locationFlow.emit(fix)
            }
        }
    }

    fun startLocationUpdates() {
        locationCapture.start(hasLocationPermission = true)
    }

    fun stopLocationUpdates() {
        locationCapture.stop()
    }


    override fun onDestroy() {
        AppLogger.d(TAG, "Location capture foreground service stopped...")
        serviceScope.cancel()
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
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Live Location",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
        AppLogger.d(TAG, "Location Notification Channel created")
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationForegroundService = this@LocationForegroundService
    }

    //TODO: DO this on explicit user call
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? = binder
}