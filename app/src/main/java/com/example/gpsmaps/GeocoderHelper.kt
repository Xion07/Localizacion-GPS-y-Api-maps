package com.example.gpsmaps

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object GeocoderHelper {

    /** "Universidad de Antioquia" -> LatLng */
    suspend fun geocode(context: Context, query: String): LatLng? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val address: Address? =
            if (Build.VERSION.SDK_INT >= 33) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            cont.resume(addresses.firstOrNull())
                        }
                        override fun onError(errorMessage: String?) {
                            cont.resume(null)
                        }
                    })
                }
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(query, 1)?.firstOrNull()
                    } catch (e: Exception) { null }
                }
            }
        return address?.let { LatLng(it.latitude, it.longitude) }
    }

    /** LatLng -> "Calle 67 #53-108, Medellín, Colombia" */
    suspend fun reverseGeocode(context: Context, point: LatLng): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val address: Address? =
            if (Build.VERSION.SDK_INT >= 33) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(point.latitude, point.longitude, 1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                cont.resume(addresses.firstOrNull())
                            }
                            override fun onError(errorMessage: String?) {
                                cont.resume(null)
                            }
                        })
                }
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(point.latitude, point.longitude, 1)?.firstOrNull()
                    } catch (e: Exception) { null }
                }
            }
        return address?.getAddressLine(0)
    }
}
