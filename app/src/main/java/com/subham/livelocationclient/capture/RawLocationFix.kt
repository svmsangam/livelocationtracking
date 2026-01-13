package com.subham.livelocationclient.capture

class RawLocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val deviceTimeMs: Long
) {
}