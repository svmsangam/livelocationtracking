package com.subham.livelocationclient.capture.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.subham.livelocationclient.capture.LocationCapture
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.subham.livelocationclient.R
import com.subham.livelocationclient.capture.enums.TrackingStatus
import com.subham.livelocationclient.capture.events.LocationEvent
import com.subham.livelocationclient.capture.`interface`.LocationPublisher
import com.subham.livelocationclient.capture.reducer.LocationStateReducer
import com.subham.livelocationclient.data.LocationState
import com.subham.livelocationclient.debug.AppLogger
import com.subham.livelocationclient.permission.hasLocationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "LocationForegroundService"

class LocationForegroundService : Service(), LocationPublisher {

    companion object {
        const val CHANNEL_ID = "live_location_tracking"
        const val NOTIFICATION_ID = 1001
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val reducerDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val serviceScope = CoroutineScope(SupervisorJob() + reducerDispatcher)

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val reducer = LocationStateReducer({ System.currentTimeMillis() })

    private val _state =
        MutableStateFlow(LocationState.initial())
    override val state: StateFlow<LocationState> = _state

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
        return START_STICKY
    }

    private val locationCapture by lazy {
        LocationCapture(fusedLocationClient) { fix ->
            serviceScope.launch {
                dispatch(LocationEvent.FixReceived(fix))
            }
        }
    }

    fun stopLocationUpdates() {
        dispatch(LocationEvent.StopTracking)
        locationCapture.stop()
    }


    override fun onDestroy() {
        AppLogger.d(TAG, "Location capture foreground service stopped...")
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

    private fun dispatch(event: LocationEvent) {
        try {
            serviceScope.launch {
                _state.update { oldState ->
                    val newState = reducer.reduce(oldState, event)
                    AppLogger.d(TAG, "Event: $event, OldState: $oldState, NewState: $newState")
                    newState
                }
            }
        }catch (ex: Exception){
            // Log and update error state as fallback
            AppLogger.e(TAG, "Reducer failed: ${ex.message}")
            _state.value = _state.value.copy(
                status = TrackingStatus.ERROR,
                lastError = "Reducer exception: ${ex.message}",
                updatedAt = System.currentTimeMillis()
            )
        }
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
        stopLocationUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? = binder

    override fun startTracking() {
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
            AppLogger.d(TAG, "Location Notification Built Success. Staring location capture now...")
            dispatch(LocationEvent.StartTracking)
            AppLogger.d(TAG, "Tracking started")
            locationCapture.start(hasLocationPermission = true)
        } catch (ex: Exception) {
            dispatch(
                LocationEvent.ProviderError(
                    ex.message ?: "Unknown error during startTracking()"
                )
            )
        }
    }

    override fun stopTracking() {
        dispatch(LocationEvent.StopTracking)
        locationCapture.stop()
        AppLogger.d(TAG, "Tracking stopped")
    }
}