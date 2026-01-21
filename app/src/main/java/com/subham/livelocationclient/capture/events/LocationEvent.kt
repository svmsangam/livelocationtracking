package com.subham.livelocationclient.capture.events

import com.subham.livelocationclient.data.RawLocationFix

sealed interface LocationEvent {
    data object StartTracking : LocationEvent
    data object StopTracking : LocationEvent

    data class FixReceived(
        val fix: RawLocationFix
    ) : LocationEvent

    data class ProviderError(
        val reason: String
    ) : LocationEvent
}