package com.subham.livelocationclient.capture.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import com.subham.livelocationclient.capture.LocationCapture
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.subham.livelocationclient.R
import com.subham.livelocationclient.capture.events.LocationEvent
import com.subham.livelocationclient.capture.`interface`.LocationPublisher
import com.subham.livelocationclient.data.LocationState
import com.subham.livelocationclient.debug.AppLogger
import com.subham.livelocationclient.permission.hasLocationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "LocationForegroundService"

@OptIn(ExperimentalCoroutinesApi::class)
class LocationForegroundService(
    @VisibleForTesting
    internal var serviceScope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default.limitedParallelism(
            1
        )
    ),
    @VisibleForTesting
    internal var clock: () -> Long = { System.currentTimeMillis() }
) : Service(), LocationPublisher {
    companion object {
        const val CHANNEL_ID = "live_location_tracking"
        const val NOTIFICATION_ID = 1001
    }

    private lateinit var trackingEngine: LocationTrackingEngine

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        trackingEngine = LocationTrackingEngine(clock = clock)
    }

    override val state: StateFlow<LocationState>
        get() = trackingEngine.state

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (!applicationContext.hasLocationPermission()) {
            AppLogger.d(
                TAG,
                "Location permission not granted. Failed to start location capture foreground service"
            )
            stopSelf()
            return START_NOT_STICKY
        }
        AppLogger.d(TAG, "Location foreground service started......")
        return START_STICKY
    }

    private val locationCapture by lazy {
        LocationCapture(fusedLocationClient) { fix ->
            serviceScope.launch {
                trackingEngine.dispatch(LocationEvent.FixReceived(fix))
            }
        }
    }

    override fun onDestroy() {
        AppLogger.d(TAG, "Location foreground service destroyed...")
        stopTracking()
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

        fun stopTracking() {
            this@LocationForegroundService.stopTracking()
        }

        fun startTracking() {
            this@LocationForegroundService.startTracking()
        }
    }

    //TODO: DO this on explicit user call
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopTracking()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? = binder

    override fun startTracking() {
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
            AppLogger.d(TAG, "Location notification created....")
            trackingEngine.dispatch(LocationEvent.StartTracking)
            locationCapture.start(hasLocationPermission = true)
            AppLogger.d(TAG, "Location Capture started.........")
        } catch (ex: Exception) {
            trackingEngine.dispatch(
                LocationEvent.ProviderError(
                    ex.message ?: "Unknown error during startTracking()"
                )
            )
        }
    }

    override fun stopTracking() {
        locationCapture.stop()
        trackingEngine.dispatch(LocationEvent.StopTracking)
        AppLogger.d(TAG, "Location Capture stopped.........")
    }
}