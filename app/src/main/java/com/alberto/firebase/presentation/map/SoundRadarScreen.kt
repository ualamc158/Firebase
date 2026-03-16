package com.alberto.firebase.presentation.map

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    // 🌟 PETICIÓN DE PERMISOS DE NOTIFICACIONES Y GPS
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Si nos dan permiso, comprobamos si alguien está cerca
        radarViewModel.checkProximity(context)
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        // Solo pedimos POST_NOTIFICATIONS si el móvil tiene Android 13 o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    // 🌟 CADA VEZ QUE ALGUIEN PONGA MÚSICA O SE MUEVA, COMPROBAMOS DISTANCIAS
    LaunchedEffect(usersNearby) {
        radarViewModel.checkProximity(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SoundRadar 🌍 En Vivo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            factory = { ctx ->
                MapView(ctx).apply {
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