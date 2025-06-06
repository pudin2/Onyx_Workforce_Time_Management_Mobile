package com.example.horas_extra_tecnipalma

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import android.location.Location
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("MissingPermission")
suspend fun Context.getLastKnownLocation(): Location? = suspendCancellableCoroutine { cont ->
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(this)

    fusedLocationClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            cont.resume(location)
        }
        .addOnFailureListener { exception ->
            cont.resumeWithException(exception)
        }
}
