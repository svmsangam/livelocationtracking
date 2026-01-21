package com.subham.livelocationclient.data

import android.location.Location
import com.subham.livelocationclient.capture.enums.LocationConfidence
import com.subham.livelocationclient.capture.enums.MotionState
import com.subham.livelocationclient.capture.enums.TrackingStatus

data class LocationState(
    val sessionId: String?,
    val status: TrackingStatus,
    val lastRawFix: RawLocationFix?,
    val derivedLocation: DerivedLocation?,
    val confidence: LocationConfidence,
    val motion: MotionState,
    val startedAt: Long?,
    val updatedAt: Long?,
    val lastError: String?
){
    companion object {
        fun initial(): LocationState {
            return LocationState(
                sessionId = null,
                status = TrackingStatus.IDLE,
                lastRawFix = null,
                derivedLocation =  null,
                confidence = LocationConfidence.NONE,
                motion = MotionState.UNKNOWN,
                startedAt = null,
                updatedAt = null,
                lastError = null
            )
        }
    }
}