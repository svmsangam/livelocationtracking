package com.subham.livelocationclient

import android.content.Intent
import android.os.IBinder
import com.subham.livelocationclient.capture.LocationCapture
import com.subham.livelocationclient.capture.enums.TrackingStatus
import com.subham.livelocationclient.capture.service.LocationForegroundService
import com.subham.livelocationclient.data.RawLocationFix
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocationForegroundServiceTest {

    private lateinit var service: LocationForegroundService
    private lateinit var testScope: TestScope
    private lateinit var scheduler: TestCoroutineScheduler

    private val locationCaptureMock = mockk<LocationCapture>(relaxed = true)

    @Before
    fun setup() {
        // ---- coroutine test setup ----
        scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        testScope = TestScope(dispatcher)

        val controller = Robolectric.buildService(LocationForegroundService::class.java)

        // ---- Robolectric creates the Service ----
        service = controller.get()

        // ---- inject test dependencies BEFORE onCreate ----
        service.serviceScope = testScope
        service.clock = { 123456L }
        service.locationCapture = locationCaptureMock

        service.locationCaptureFactory = { callback ->
            println("Mocked locationCaptureFactory called")
            every { locationCaptureMock.start(any()) } answers {
                println("Mocked locationCapture.start called with $it")
                callback(
                    RawLocationFix(
                        latitude = 1.0,
                        longitude = 2.0,
                        accuracyMeters = 5f,
                        deviceTimeMs = 123456L
                    )
                )
            }
            locationCaptureMock
        }

        // ---- now let Android lifecycle run ----
        controller.create()
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun testStartTracking() = runTest {
        service.startTracking()

        // Advance coroutine tasks
        testScope.advanceUntilIdle()

        // Verify that locationCapture.start() was called with true for permission
        verify { locationCaptureMock.start(hasLocationPermission = true) }

        // Assert the tracking engine state updated to TRACKING
        val state = service.state.first()
        Assert.assertEquals(TrackingStatus.TRACKING, state.status)
    }

    @Test
    fun testStopTracking() = runTest {
        service.stopTracking()

        testScope.advanceUntilIdle()

        verify { locationCaptureMock.stop() }
        val state = service.state.first()
        Assert.assertEquals(TrackingStatus.IDLE, state.status)
    }

    @Test
    fun testOnBinder() {
        val binder: IBinder? = service.onBind(Intent())
        assert(binder is LocationForegroundService.LocalBinder)
    }
}