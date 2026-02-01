package com.subham.livelocationclient

import com.subham.livelocationclient.capture.enums.TrackingStatus
import com.subham.livelocationclient.capture.events.LocationEvent
import com.subham.livelocationclient.capture.service.LocationTrackingEngine
import com.subham.livelocationclient.debug.AppLogger
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LocationTrackingEngineTest {
    private lateinit var engine: LocationTrackingEngine

    @Before
    fun setup() {
        AppLogger.enabled = false
        engine = LocationTrackingEngine(clock = { 1_000L })
    }

    @Test
    fun `start tracking updates state`() {
        engine.dispatch(LocationEvent.StartTracking)

        assertEquals(
            TrackingStatus.TRACKING,
            engine.state.value.status
        )
    }

    @Test
    fun `stop tracking updates state`() {
        engine.dispatch(LocationEvent.StartTracking)
        assertEquals(
            TrackingStatus.TRACKING,
            engine.state.value.status
        )
        engine.dispatch(LocationEvent.StopTracking)
        assertEquals(
            TrackingStatus.STOPPED,
            engine.state.value.status
        )
    }
}