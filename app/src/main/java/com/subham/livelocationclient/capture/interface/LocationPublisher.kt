package com.subham.livelocationclient.capture.`interface`

import com.subham.livelocationclient.data.LocationState
import kotlinx.coroutines.flow.StateFlow

interface LocationPublisher {
    val state: StateFlow<LocationState>

    fun startTracking()
    fun stopTracking()
}