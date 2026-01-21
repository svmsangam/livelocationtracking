package com.subham.livelocationclient

import com.subham.livelocationclient.capture.enums.LocationConfidence
import com.subham.livelocationclient.capture.enums.MotionState
import com.subham.livelocationclient.capture.enums.TrackingStatus
import com.subham.livelocationclient.capture.events.LocationEvent
import com.subham.livelocationclient.capture.reducer.LocationStateReducer
import com.subham.livelocationclient.data.LocationState
import com.subham.livelocationclient.data.RawLocationFix
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Test

class LocationStateReducerTest {

    private var now = 1_000L
    private val reducer = LocationStateReducer { now }

    private fun advanceTime(ms: Long = 1000) {
        now += ms
    }

    private fun idleState() = LocationState(
        sessionId = null,
        status = TrackingStatus.IDLE,
        lastRawFix = null,
        derivedLocation = null,
        confidence = LocationConfidence.NONE,
        motion = MotionState.UNKNOWN,
        startedAt = null,
        updatedAt = now,
        lastError = null
    )

    @Test
    fun `start tracking initializes session`() {
        val state = idleState()

        val newState = reducer.reduce(state, LocationEvent.StartTracking)

        assertEquals(TrackingStatus.TRACKING, newState.status)
        assertNotNull(newState.sessionId)
        assertNotNull(newState.startedAt)
    }

    @Test
    fun `fix ignored when not tracking`() {
        val state = idleState()

        val fix = RawLocationFix(1.0, 1.0, 10f, now)

        val newState = reducer.reduce(state, LocationEvent.FixReceived(fix))

        assertNull(newState.lastRawFix)
    }

    @Test
    fun `acceptable accuracy produces derived location`() {
        val started = reducer.reduce(idleState(), LocationEvent.StartTracking)

        val fix = RawLocationFix(27.7, 85.3, 12f, now, 0.6f)
        advanceTime()

        val newState = reducer.reduce(started, LocationEvent.FixReceived(fix))

        assertNotNull(newState.derivedLocation)
        assertEquals(LocationConfidence.MEDIUM, newState.confidence)
        assertEquals(MotionState.MOVING, newState.motion)
    }

    @Test
    fun `poor accuracy does not create derived location`() {
        val started = reducer.reduce(idleState(), LocationEvent.StartTracking)

        val fix = RawLocationFix(27.7, 85.3, 120f, now)

        val newState = reducer.reduce(started, LocationEvent.FixReceived(fix))

        assertNull(newState.derivedLocation)
        assertEquals(LocationConfidence.LOW, newState.confidence)
    }

    @Test
    fun `stop tracking updates status`() {
        val started = reducer.reduce(idleState(), LocationEvent.StartTracking)

        val stopped = reducer.reduce(started, LocationEvent.StopTracking)

        assertEquals(TrackingStatus.STOPPED, stopped.status)
    }


}