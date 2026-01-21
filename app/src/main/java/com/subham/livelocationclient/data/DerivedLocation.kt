package com.subham.livelocationclient.data

data class DerivedLocation(
    val latitude: Double, val longitude: Double, val accuracyMeters: Float,

    val speedMps: Float?,          // trusted speed
    val bearingDeg: Float?,        // trusted direction

    val sourceDeviceTimeMs: Long   // which raw fix this came from
) {

}
