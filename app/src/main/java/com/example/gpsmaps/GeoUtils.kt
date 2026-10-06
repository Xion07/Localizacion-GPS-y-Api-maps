package com.example.gpsmaps

import android.location.Location
import com.google.android.gms.maps.model.LatLng

/** Acepta "6.2676, -75.5685" (punto como decimal). Si no es válido devuelve null. */
fun parseLatLng(text: String): LatLng? {
    val p = text.trim().split(Regex("[,;\\s]+")).filter { it.isNotEmpty() }
    if (p.size != 2) return null
    val lat = p[0].toDoubleOrNull() ?: return null
    val lng = p[1].toDoubleOrNull() ?: return null
    return if (lat in -90.0..90.0 && lng in -180.0..180.0) LatLng(lat, lng) else null
}

/** Distancia en metros entre dos puntos, sobre el elipsoide terrestre (WGS84). */
fun distanceMeters(a: LatLng, b: LatLng): Float {
    val r = FloatArray(1)
    Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, r)
    return r[0]
}
