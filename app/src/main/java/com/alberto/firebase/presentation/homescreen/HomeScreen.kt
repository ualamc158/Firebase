package com.alberto.firebase.presentation.homescreen

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.alberto.firebase.R
import com.alberto.firebase.data.model.Artist
import com.alberto.firebase.data.model.Player
import com.alberto.firebase.presentation.map.SoundRadarViewModel
import com.alberto.firebase.ui.theme.Black
import com.alberto.firebase.ui.theme.Purple40
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewmodel: HomeViewmodel = HomeViewmodel(),
    radarViewModel: SoundRadarViewModel = viewModel(), // 🌟 Inyectamos el radar
    auth: FirebaseAuth,
    navigateToInitial: () -> Unit,
    navigateToChat: () -> Unit,
    navigateToRadar: () -> Unit
) {
    val recommendedArtists by viewmodel.recommendedArtists.collectAsState()
    val recommendedSongs by viewmodel.recommendedSongs.collectAsState()
    val searchResults by viewmodel.searchResults.collectAsState()
    val player by viewmodel.player.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 🌟 LANZADOR DE PERMISOS: Maneja el cartelito de Android
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            // Si el usuario acepta, intentamos emitir la ubicación
            viewmodel.startMusicAndEmitLocation(context, radarViewModel)
        }
    }

    ModalNavigationDrawer(
        modifier = Modifier.systemBarsPadding(),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1E1E1E),
                drawerContentColor = Color.White
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Menú Principal",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider(color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                NavigationDrawerItem(
                    label = { Text("SoundRadar 🌍") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navigateToRadar()
                    },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("Cerrar sesión") }, selected = false,
                    onClick = {
                        val gso =
                            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                        GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                            auth.signOut()
                            navigateToInitial()
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                NavigationDrawerItem(
                    label = { Text("Salir de la App") }, selected = false,
                    onClick = { (context as? Activity)?.finish() },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "SoundConnect",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menú", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Black)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navigateToChat() },
                    containerColor = Purple40,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Chat, "Chat")
                }
            },
            bottomBar = {
                val progress by viewmodel.songProgress.collectAsState()
                player?.let {
                    PlayerComponent(
                        player = it,
                        progress = progress,
                        onProgressChange = { newProgress -> viewmodel.seekAudio(newProgress) },
                        onPlaySelected = {
                            // 🌟 ACCIÓN AL DAR PLAY: Pedir permisos y emitir ubicación
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                            viewmodel.startMusicAndEmitLocation(context, radarViewModel)
                        },
                        onCancelSelected = { viewmodel.onCancelSelected() }
                    )
                }
            }
        ) { paddingValues ->
            Column(Modifier
                .fillMaxSize()
                .background(Black)
                .padding(paddingValues)) {
                var searchText by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it; viewmodel.searchMusicFromDeezer(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("¿Qué quieres escuchar?", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, "Buscar", tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple40,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (searchText.isBlank()) {
                        item {
                            Text(
                                "Artistas Populares",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(recommendedArtists) { ArtistItem(artist = it, { }) }
                            }
                        }
                        item {
                            Text(
                                "Top Hits Globales",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 24.dp,
                                    bottom = 8.dp
                                )
                            )
                        }
                        items(recommendedSongs) { track ->
                            TrackItem(
                                track = track,
                                onItemSelected = { viewmodel.addPlayer(track) })
                        }
                    } else {
                        items(searchResults) { track ->
                            TrackItem(
                                track = track,
                                onItemSelected = { viewmodel.addPlayer(track) })
                        }
                    }
                }
            }
        }
    }
}

// ... (TrackItem, PlayerComponent y ArtistItem se mantienen iguales que antes)
@Composable
fun TrackItem(track: Artist, onItemSelected: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemSelected() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.image,
            contentDescription = "Carátula",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.description.orEmpty(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(text = track.name.orEmpty(), color = Color.Gray, fontSize = 12.sp, maxLines = 1)
        }
        Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir", tint = Purple40)
    }
}

@Composable
fun PlayerComponent(
    player: Player,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onPlaySelected: () -> Unit,
    onCancelSelected: () -> Unit
) {
    val icon = if (player.play == true) R.drawable.ic_pause else R.drawable.ic_play
    Column(Modifier
        .fillMaxWidth()
        .background(Purple40)) {
        Slider(
            value = progress,
            onValueChange = onProgressChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
        )
        Row(Modifier
            .height(45.dp)
            .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = player.artist?.description.orEmpty(),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f),
                color = Color.White,
                maxLines = 1
            )
            Image(
                painter = painterResource(id = icon),
                contentDescription = "play/pause",
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onPlaySelected() })
            Image(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Close",
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onCancelSelected() })
        }
    }
}

@Composable
fun ArtistItem(artist: Artist, onItemSelected: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onItemSelected() }
            .width(80.dp)
    ) {
        AsyncImage(
            model = artist.image,
            contentDescription = "Artists image",
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name.orEmpty(),
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}