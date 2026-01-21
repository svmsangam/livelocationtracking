package com.subham.livelocationclient.capture.reducer

import com.subham.livelocationclient.capture.enums.LocationConfidence
import com.subham.livelocationclient.capture.enums.MotionState
import com.subham.livelocationclient.capture.enums.TrackingStatus
import com.subham.livelocationclient.capture.events.LocationEvent
import com.subham.livelocationclient.data.DerivedLocation
import com.subham.livelocationclient.data.LocationState
import com.subham.livelocationclient.data.RawLocationFix

private const val MAX_ACCEPTABLE_ACCURACY = 50f
private const val MOVING_SPEED_THRESHOLD = 0.5f

class LocationStateReducer(private val clock: () -> Long) {

    fun reduce(
        old: LocationState,
        event: LocationEvent
    ): LocationState {
        return when (event) {
            is LocationEvent.StartTracking -> onStart(old)
            is LocationEvent.StopTracking -> onStop(old)
            is LocationEvent.FixReceived -> onFix(old, event.fix)
            is LocationEvent.ProviderError -> onError(old)
        }
    }

    private fun onStart(old: LocationState): LocationState =
        old.copy(
            sessionId = java.util.UUID.randomUUID().toString(),
            status = TrackingStatus.TRACKING,
            startedAt = clock(),
            updatedAt = clock(),
            confidence = LocationConfidence.NONE,
            motion = MotionState.UNKNOWN
        )

    private fun onStop(old: LocationState): LocationState =
        old.copy(
            status = TrackingStatus.STOPPED,
            derivedLocation =  null,
            confidence = LocationConfidence.NONE,
            motion = MotionState.UNKNOWN,
            updatedAt = clock()
        )

    private fun onFix(
        old: LocationState,
        fix: RawLocationFix
    ): LocationState {

        if (old.status != TrackingStatus.TRACKING) return old

        val confidence =
            if (fix.accuracyMeters <= MAX_ACCEPTABLE_ACCURACY) {
                when {
                    fix.accuracyMeters <= 10f -> LocationConfidence.HIGH
                    fix.accuracyMeters <= 30f -> LocationConfidence.MEDIUM
                    else -> LocationConfidence.LOW
                }
            } else LocationConfidence.NONE


        val derived =
            if (fix.accuracyMeters <= MAX_ACCEPTABLE_ACCURACY) {
                DerivedLocation(
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    accuracyMeters = fix.accuracyMeters,
                    speedMps = fix.speedMps,
                    sourceDeviceTimeMs = fix.deviceTimeMs,
                    bearingDeg = fix.bearingDeg
                )
            } else null

        val motion =
            when {
                fix.speedMps == null -> old.motion
                fix.speedMps > MOVING_SPEED_THRESHOLD -> MotionState.MOVING
                else -> MotionState.STATIONARY
            }

        return old.copy(
            lastRawFix = fix,
            derivedLocation = derived,
            confidence = confidence,
            motion = motion,
            updatedAt = clock()
        )
    }

    private fun onError(old: LocationState): LocationState =
        old.copy(
            status =  TrackingStatus.ERROR,
            confidence = LocationConfidence.NONE,
            lastError = "Some error tracking location",
            updatedAt = clock()
        )


}