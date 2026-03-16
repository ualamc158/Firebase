package com.alberto.firebase.presentation.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundRadarScreen(
    onBack: () -> Unit,
    radarViewModel: SoundRadarViewModel = viewModel()
) {
    val usersNearby by radarViewModel.usersNearby.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SoundRadar 🌍 En Vivo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            factory = { context ->
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(5.0)
                    controller.setCenter(GeoPoint(40.4168, -3.7038)) // Inicio en España
                }
            },
            update = { mapView ->
                mapView.overlays.clear() // Limpiamos marcadores viejos

                usersNearby.forEach { user ->
                    val marker = Marker(mapView)
                    marker.position = GeoPoint(user.latitude, user.longitude)
                    marker.title = user.email
                    marker.snippet = "Escuchando: ${user.songTitle} - ${user.artistName}"
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(marker)
                }
                mapView.invalidate() // Refrescar mapa
            }
        )
    }
}