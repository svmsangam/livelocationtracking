package com.subham.livelocationclient.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.Manifest
import com.subham.livelocationclient.debug.AppLogger

private const val TAG = "LocationPermission"
fun Context.hasLocationPermission(): Boolean {
    val fineGranted =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    AppLogger.d(TAG, "hasLocationPermission() =  $fineGranted")
    return fineGranted
    //TODO implement background permission
//    if (!fineGranted) return false
//
//    // Background location only matters on Android 10+
//    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//        ContextCompat.checkSelfPermission(
//            this,
//            Manifest.permission.ACCESS_BACKGROUND_LOCATION
//        ) == PackageManager.PERMISSION_GRANTED
//    } else {
//        true
//    }
}
