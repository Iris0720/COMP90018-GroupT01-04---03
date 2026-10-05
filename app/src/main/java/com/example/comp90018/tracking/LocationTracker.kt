package com.example.comp90018.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationTracker(
    context: Context
) {
    // Stores the app-level Context for safe long-term use.
    private val applicationContext = context.applicationContext

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(applicationContext)

    private var locationCallback: LocationCallback? = null
    private var currentLocationTokenSource: CancellationTokenSource? = null

    fun hasLocationPermission(): Boolean {
        val finePermission = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val coarsePermission = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        return finePermission == PackageManager.PERMISSION_GRANTED ||
                coarsePermission == PackageManager.PERMISSION_GRANTED
    }

    // Starts location tracking and returns location updates to the caller.
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(
        onLocationReceived: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission()) {
            onError("Location permission is required.")
            return
        }

        if (locationCallback != null) {
            return
        }

        /*
         * Immediately request one current location.
         * This provides the starting point without waiting for the first
         * periodic location update.
         */
        currentLocationTokenSource?.cancel()

        // Creates a token that can cancel the current-location request.
        val tokenSource = CancellationTokenSource()
        currentLocationTokenSource = tokenSource

        // Creates a high-accuracy request for the current location.
        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(10_000L)
            .setDurationMillis(10_000L)
            .build()

        // Requests the current location once.
        fusedLocationClient
            .getCurrentLocation(
                currentLocationRequest,
                tokenSource.token
            )
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationReceived(location)
                }
            }

        // Continue receiving locations so the route can be drawn while the user is moving.
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2_000L
        )
            .setMinUpdateIntervalMillis(1_000L)
            .setMinUpdateDistanceMeters(2f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(
                locationResult: LocationResult
            ) {
                locationResult.locations.forEach { location ->
                    onLocationReceived(location)
                }
            }
        }

        locationCallback = callback

        fusedLocationClient
            .requestLocationUpdates(
                locationRequest,
                callback,
                applicationContext.mainLooper
            )
            .addOnFailureListener { error ->
                locationCallback = null

                onError(
                    error.message
                        ?: "Unable to receive location updates."
                )
            }
    }

    fun stopLocationUpdates() {
        currentLocationTokenSource?.cancel()
        currentLocationTokenSource = null

        val callback = locationCallback
        if (callback != null) {
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
    }
}