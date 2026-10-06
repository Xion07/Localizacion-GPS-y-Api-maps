package com.example.gpsmaps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseLatLngTest {

    @Test
    fun `coordenadas con coma y espacio`() {
        val p = parseLatLng("6.2676, -75.5685")!!
        assertEquals(6.2676, p.latitude, 1e-9)
        assertEquals(-75.5685, p.longitude, 1e-9)
    }

    @Test
    fun `acepta punto y coma, solo espacios y espacios sobrantes`() {
        assertEquals(6.2519, parseLatLng("6.2519;-75.5686")!!.latitude, 1e-9)
        assertEquals(-75.5686, parseLatLng("  6.2519   -75.5686  ")!!.longitude, 1e-9)
    }

    @Test
    fun `limites del rango son validos`() {
        assertEquals(90.0, parseLatLng("90, 180")!!.latitude, 0.0)
        assertEquals(-180.0, parseLatLng("-90, -180")!!.longitude, 0.0)
    }

    @Test
    fun `fuera de rango devuelve null`() {
        assertNull(parseLatLng("91, 0"))
        assertNull(parseLatLng("0, -181"))
    }

    @Test
    fun `texto que no son coordenadas devuelve null`() {
        assertNull(parseLatLng("Universidad de Antioquia"))
        assertNull(parseLatLng(""))
        assertNull(parseLatLng("6.2676"))
        assertNull(parseLatLng("6.2676, -75.5685, 10"))
    }

    @Test
    fun `coma decimal no se confunde con el separador`() {
        // "6,2676" se parte en dos numeros: 6 y 2676 -> longitud fuera de rango
        assertNull(parseLatLng("6,2676 -75,5685"))
    }
}
