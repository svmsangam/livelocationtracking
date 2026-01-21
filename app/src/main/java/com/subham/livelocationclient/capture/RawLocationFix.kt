package com.subham.livelocationclient.capture

import android.location.Location

class RawLocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val deviceTimeMs: Long,
    val serverIngestTimestampMs: Long? = null
) {
    companion object {
        fun fromLocation(location: Location): RawLocationFix {
            return RawLocationFix(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy,
                deviceTimeMs = location.time
            )
        }
    }
}