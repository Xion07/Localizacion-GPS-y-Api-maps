package com.example.gpsmaps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** Acepta "6.2676, -75.5685" (punto como decimal). Si no es válido devuelve null. */
private fun parseLatLng(text: String): LatLng? {
    val p = text.trim().split(Regex("[,;\\s]+")).filter { it.isNotEmpty() }
    if (p.size != 2) return null
    val lat = p[0].toDoubleOrNull() ?: return null
    val lng = p[1].toDoubleOrNull() ?: return null
    return if (lat in -90.0..90.0 && lng in -180.0..180.0) LatLng(lat, lng) else null
}

@Composable
fun MapScreen(vm: MapViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsStateWithLifecycle()

    // ---------- PASO 1: Permisos ----------
    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasPermission) vm.locateOnce()
    }
    // Si el usuario concede el permiso desde Ajustes, lo detectamos al volver a la app
    LifecycleResumeEffect(Unit) {
        val now = hasLocationPermission(context)
        if (now && !hasPermission) vm.locateOnce()
        hasPermission = now
        onPauseOrDispose { }
    }
    fun requestPermission() = permissionLauncher.launch(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    LaunchedEffect(Unit) {
        if (hasPermission) vm.locateOnce()
        else permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // ---------- Estado del mapa y de la UI ----------
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(6.2442, -75.5812), 12f) // Medellín
    }
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var markers by remember { mutableStateOf(listOf<LatLng>()) }
    var query by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf<String?>(null) }

    // Estado del reto
    var tab by remember { mutableIntStateOf(0) }            // 0 = Tracking, 1 = Reto
    var challengeIndex by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf<LatLng?>(null) }
    var revealed by remember { mutableStateOf(false) }

    // La cámara sigue al usuario hasta que él mueve el mapa con el dedo
    var follow by remember { mutableStateOf(true) }
    LaunchedEffect(cameraState.isMoving) {
        if (cameraState.isMoving &&
            cameraState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE
        ) follow = false
    }

    // Alto de las tarjetas para que el mapa no ponga controles ni el logo de Google debajo
    val density = LocalDensity.current
    var topPadding by remember { mutableStateOf(0.dp) }
    var bottomPadding by remember { mutableStateOf(0.dp) }

    // La cámara sigue al usuario solo en la pestaña de tracking
    LaunchedEffect(state.current) {
        if (tab == 0 && follow) {
            state.current?.let {
                cameraState.animate(CameraUpdateFactory.newLatLngZoom(it, 17f), 800)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {

        // ---------- PASO 2: El mapa ----------
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
            properties = MapProperties(
                isMyLocationEnabled = hasPermission,   // punto azul
                mapType = mapType
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = hasPermission,
                compassEnabled = true
            ),
            onMapClick = { point ->
                markers = markers + point
                scope.launch { addressText = GeocoderHelper.reverseGeocode(context, point) }
            }
        ) {
            // PASO 3: Marcadores (los que tocas o buscas)
            markers.forEachIndexed { i, p ->
                Marker(
                    state = rememberMarkerState(key = "m$i", position = p),
                    title = "Punto ${i + 1}",
                    snippet = "%.5f, %.5f".format(p.latitude, p.longitude)
                )
            }

            // Marcador verde del objetivo del reto
            target?.let { t ->
                Marker(
                    state = rememberMarkerState(key = "target", position = t),
                    title = "Objetivo",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )
                // Línea entre tu último marcador y el objetivo
                markers.lastOrNull()?.let { guess ->
                    Polyline(points = listOf(guess, t), color = Color(0xFF2E7D32), width = 8f)
                }
            }

            // Línea roja entre los dos primeros marcadores (modo libre)
            if (target == null && markers.size >= 2) {
                Polyline(points = listOf(markers[0], markers[1]), color = Color.Red, width = 8f)
            }

            // PASO 5: Ruta recorrida (tracking)
            if (state.route.size > 1) {
                Polyline(points = state.route, color = Color(0xFF1565C0), width = 14f)
            }

            // Círculo de precisión
            state.current?.let {
                Circle(
                    center = it,
                    radius = state.accuracyMeters.toDouble().coerceAtLeast(5.0),
                    fillColor = Color(0x221565C0),
                    strokeColor = Color(0xFF1565C0),
                    strokeWidth = 2f
                )
            }
        }

        // ---------- PASO 6: Buscador (coordenadas o dirección) ----------
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(12.dp)
                .fillMaxWidth()
                .onSizeChanged { topPadding = with(density) { it.height.toDp() } + 12.dp }
        ) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("lat, lng  o  dirección") }
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    scope.launch {
                        // 1) ¿son coordenadas? 2) si no, geocodificar como dirección
                        val point = parseLatLng(query) ?: GeocoderHelper.geocode(context, query)
                        if (point != null) {
                            markers = markers + point
                            cameraState.animate(CameraUpdateFactory.newLatLngZoom(point, 16f), 800)
                        } else {
                            Toast.makeText(context, "No se encontró", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Ir") }
            }
        }

        // ---------- Panel inferior con dos pestañas ----------
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp)
                .fillMaxWidth()
                .onSizeChanged { bottomPadding = with(density) { it.height.toDp() } + 12.dp }
        ) {
            Column {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Tracking") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Reto") })
                }

                Column(Modifier.padding(12.dp)) {
                    if (tab == 0) {
                        // ----- Pestaña TRACKING -----
                        if (!hasPermission) {
                            Text(
                                "Sin permiso de ubicación: el mapa funciona, pero no el GPS.",
                                color = MaterialTheme.colorScheme.error
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { requestPermission() }) { Text("Dar permiso") }
                                // Si lo negó dos veces, Android ya no muestra el diálogo: toca ir a Ajustes
                                TextButton(onClick = {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                    )
                                }) { Text("Ajustes") }
                            }
                        }
                        Text(
                            "Distancia: %.0f m | Vel: %.1f km/h | Precisión: %.0f m"
                                .format(state.distanceMeters, state.speedKmh, state.accuracyMeters),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (markers.size >= 2) {
                            Text("Punto 1 → Punto 2: %.0f m".format(MapViewModel.distance(markers[0], markers[1])))
                        }
                        addressText?.let { Text("Último punto: $it") }
                        if (!follow && state.current != null) {
                            TextButton(onClick = {
                                follow = true
                                state.current?.let {
                                    scope.launch {
                                        cameraState.animate(CameraUpdateFactory.newLatLngZoom(it, 17f), 800)
                                    }
                                }
                            }) { Text("Seguirme") }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!state.isTracking) {
                                Button(enabled = hasPermission, onClick = { vm.startTracking() }) {
                                    Text("Iniciar")
                                }
                            } else {
                                Button(onClick = { vm.stopTracking() }) { Text("Detener") }
                            }
                            OutlinedButton(onClick = {
                                vm.clearRoute()
                                markers = emptyList()
                                addressText = null
                            }) { Text("Limpiar") }
                            OutlinedButton(onClick = {
                                mapType =
                                    if (mapType == MapType.NORMAL) MapType.SATELLITE else MapType.NORMAL
                            }) { Text("Mapa") }
                        }
                    } else {
                        // ----- Pestaña RETO -----
                        val place = ChallengePlaces.list[challengeIndex]
                        Text(
                            "Reto ${challengeIndex + 1}/${ChallengePlaces.list.size}: " +
                                "%.4f, %.4f".format(place.latLng.latitude, place.latLng.longitude),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (revealed) Text("📍 ${place.name}")
                        target?.let { t ->
                            markers.lastOrNull()?.let { guess ->
                                Text("Tu marcador quedó a %.0f m del objetivo".format(MapViewModel.distance(guess, t)))
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(modifier = Modifier.weight(1f), onClick = {
                                target = place.latLng
                                scope.launch {
                                    cameraState.animate(CameraUpdateFactory.newLatLngZoom(place.latLng, 15f), 800)
                                }
                            }) { Text("Ver") }
                            OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                revealed = true
                            }) { Text("Nombre") }
                            OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                challengeIndex = (challengeIndex + 1) % ChallengePlaces.list.size
                                target = null
                                revealed = false
                                markers = emptyList()
                            }) { Text("Siguiente") }
                        }
                    }
                }
            }
        }
    }
}
