package com.example.gpsmaps

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrackingState(
    val current: LatLng? = null,
    val route: List<LatLng> = emptyList(),
    val distanceMeters: Float = 0f,
    val speedKmh: Float = 0f,
    val accuracyMeters: Float = 0f,
    val isTracking: Boolean = false
)

class MapViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LocationRepository(app)
    private var trackingJob: Job? = null

    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    /** Una sola lectura de ubicación. */
    fun locateOnce() {
        viewModelScope.launch {
            val loc = repo.getCurrentLocation() ?: repo.getLastLocation() ?: return@launch
            _state.update {
                it.copy(
                    current = LatLng(loc.latitude, loc.longitude),
                    accuracyMeters = loc.accuracy
                )
            }
        }
    }

    fun startTracking() {
        if (trackingJob != null) return
        _state.update { it.copy(isTracking = true) }
        trackingJob = viewModelScope.launch {
            repo.locationUpdates().collect { loc ->
                val point = LatLng(loc.latitude, loc.longitude)
                _state.update { s ->
                    val added = s.route.lastOrNull()?.let { distance(it, point) } ?: 0f
                    s.copy(
                        current = point,
                        route = s.route + point,
                        distanceMeters = s.distanceMeters + added,
                        speedKmh = loc.speed * 3.6f,   // m/s -> km/h
                        accuracyMeters = loc.accuracy
                    )
                }
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _state.update { it.copy(isTracking = false, speedKmh = 0f) }
    }

    fun clearRoute() {
        _state.update { it.copy(route = emptyList(), distanceMeters = 0f) }
    }

    companion object {
        /** Distancia en metros entre dos puntos. */
        fun distance(a: LatLng, b: LatLng): Float {
            val r = FloatArray(1)
            Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, r)
            return r[0]
        }
    }
}
