package com.example.gpsmaps

import com.google.android.gms.maps.model.LatLng

data class Place(val name: String, val latLng: LatLng)

/** Lugares del reto. Coordenadas aproximadas: verificalas en la app antes de la clase. */
object ChallengePlaces {
    val list = listOf(
        Place("Universidad de Antioquia (Ciudad Universitaria)", LatLng(6.2676, -75.5685)),
        Place("Jardín Botánico de Medellín", LatLng(6.2724, -75.5632)),
        Place("Plaza Botero", LatLng(6.2519, -75.5686)),
        Place("Estadio Atanasio Girardot", LatLng(6.2565, -75.5903)),
        Place("Pueblito Paisa (Cerro Nutibara)", LatLng(6.2352, -75.5792))
    )
}
