package com.subham.livelocationclient.capture.service

import com.subham.livelocationclient.capture.enums.TrackingStatus
import com.subham.livelocationclient.capture.events.LocationEvent
import com.subham.livelocationclient.capture.reducer.LocationStateReducer
import com.subham.livelocationclient.data.LocationState
import com.subham.livelocationclient.debug.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "LocationTrackingEngine"

class LocationTrackingEngine(
    private val clock: () -> Long
) {

    private val reducer = LocationStateReducer(clock)
    private val _state =
        MutableStateFlow(LocationState.initial())
    val state: StateFlow<LocationState> = _state.asStateFlow()

    fun dispatch(event: LocationEvent) {
        try {
            val oldState = _state.value
            val newState = reducer.reduce(oldState, event)
            _state.value = newState
        } catch (ex: Exception) {
            AppLogger.e(TAG, "Reducer failed", ex.message)

            _state.value = _state.value.copy(
                status = TrackingStatus.ERROR,
                lastError = ex.message ?: "Reducer exception",
                updatedAt = clock()
            )
        }
    }

    companion object {
        private const val TAG = "LocationTrackingEngine"
    }
}