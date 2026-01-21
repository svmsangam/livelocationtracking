package com.subham.livelocationclient.data

import android.location.Location

data class RawLocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val deviceTimeMs: Long,
    val speedMps: Float? = null,
    val bearingDeg: Float? = null,
    val serverIngestTimestampMs: Long? = null
) {
    companion object {
        fun fromLocation(location: Location): RawLocationFix {
            return RawLocationFix(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy,
                speedMps = if (location.hasSpeed()) location.speed else null,
                bearingDeg = if (location.hasBearing()) location.bearing else null,
                deviceTimeMs = location.time
            )
        }
    }
}