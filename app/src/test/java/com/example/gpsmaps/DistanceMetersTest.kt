package com.example.gpsmaps

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric ejecuta Location.distanceBetween (framework de Android) en la JVM
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DistanceMetersTest {

    private val udea = LatLng(6.2676, -75.5685)
    private val plazaBotero = LatLng(6.2519, -75.5686)

    @Test
    fun `mismo punto da cero`() {
        assertEquals(0f, distanceMeters(udea, udea), 0.01f)
    }

    @Test
    fun `UdeA a Plaza Botero es aprox 1,7 km`() {
        val d = distanceMeters(udea, plazaBotero)
        assertTrue("distancia = $d", d in 1700f..1780f)
    }

    @Test
    fun `la distancia es simetrica`() {
        assertEquals(distanceMeters(udea, plazaBotero), distanceMeters(plazaBotero, udea), 0.5f)
    }

    @Test
    fun `un grado de latitud en el ecuador mide unos 110,6 km`() {
        val d = distanceMeters(LatLng(0.0, 0.0), LatLng(1.0, 0.0))
        assertEquals(110_574f, d, 50f)
    }
}
