package com.example.gpsmaps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LocationRepository(context: Context) {

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /** Ubicación actual, una sola vez. */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = try {
        client.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).await()
    } catch (e: CancellationException) {
        throw e   // nunca tragarse la cancelación de la corrutina
    } catch (e: Exception) {
        null
    }

    /** Última ubicación en caché (rápida, puede ser null). */
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? = try {
        client.lastLocation.await()
    } catch (e: CancellationException) {
        throw e   // nunca tragarse la cancelación de la corrutina
    } catch (e: Exception) {
        null
    }

    /** Flujo continuo de ubicaciones (tracking en tiempo real). */
    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMs: Long = 3000L): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(3f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }
}
