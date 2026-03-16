package com.alberto.firebase.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alberto.firebase.data.local.FavoriteSong
import com.alberto.firebase.data.model.Artist
import com.alberto.firebase.presentation.homescreen.HomeViewmodel
import com.alberto.firebase.ui.theme.Black
import com.alberto.firebase.ui.theme.Purple40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: HomeViewmodel,
    onBack: () -> Unit
) {
    // 🌟 Leemos la lista de la base de datos local
    val favorites by viewModel.favorites.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Favoritos ❤️", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Black)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Black)
                .padding(paddingValues)
        ) {
            if (favorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aún no tienes canciones favoritas", color = Color.Gray)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(favorites) { favSong ->
                        FavoriteItemRow(
                            song = favSong,
                            onPlay = {
                                // Convertimos la favorita al formato que entiende el reproductor
                                val artist = Artist(
                                    name = favSong.artist,
                                    description = favSong.title,
                                    image = favSong.imageUrl,
                                    audioUrl = favSong.audioUrl
                                )
                                viewModel.addPlayer(artist)
                            },
                            onRemove = {
                                // Usamos la misma función para quitarla
                                val artist = Artist(description = favSong.title)
                                viewModel.toggleFavorite(artist)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteItemRow(song: FavoriteSong, onPlay: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.imageUrl,
            contentDescription = "Carátula",
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1)
            Text(text = song.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
        }

        // 🌟 Botón para quitar de favoritos
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Favorite, contentDescription = "Quitar Favorito", tint = Color.Red)
        }
        Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir", tint = Purple40)
    }
}