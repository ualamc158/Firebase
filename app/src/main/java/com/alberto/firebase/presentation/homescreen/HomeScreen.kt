package com.alberto.firebase.presentation.homescreen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.alberto.firebase.R
import com.alberto.firebase.data.model.Artist
import com.alberto.firebase.data.model.Player
import com.alberto.firebase.presentation.map.SoundRadarViewModel
import com.alberto.firebase.ui.theme.Black
import com.alberto.firebase.ui.theme.Purple40
import com.alberto.firebase.utils.ShakeDetector
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File

fun createImageUri(context: Context): Uri {
    val imagePath = File(context.cacheDir, "camera_images")
    imagePath.mkdirs()
    val newFile = File(imagePath, "profile_pic.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        newFile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewmodel: HomeViewmodel = HomeViewmodel(),
    radarViewModel: SoundRadarViewModel = viewModel(),
    auth: FirebaseAuth,
    navigateToInitial: () -> Unit,
    navigateToChat: () -> Unit,
    navigateToRadar: () -> Unit,
    navigateToFavorites: () -> Unit
) {
    val recommendedArtists by viewmodel.recommendedArtists.collectAsState()
    val recommendedSongs by viewmodel.recommendedSongs.collectAsState()
    val searchResults by viewmodel.searchResults.collectAsState()
    val player by viewmodel.player.collectAsState()
    val isLoading by viewmodel.isLoading.collectAsState()
    val favorites by viewmodel.favorites.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var searchText by remember { mutableStateOf("") }

    val profilePictureUrl by viewmodel.profilePictureUrl.collectAsState()
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewmodel.uploadProfilePicture(context, it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { viewmodel.uploadProfilePicture(context, it) }
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Foto de perfil") },
            text = { Text("¿Desde dónde quieres subir la foto?") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    val uri = createImageUri(context)
                    cameraImageUri = uri
                    cameraLauncher.launch(uri)
                }) {
                    Text("Cámara", color = Purple40)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Galería", color = Purple40)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    if (drawerState.isOpen) {
        BackHandler { scope.launch { drawerState.close() } }
    } else if (searchText.isNotBlank()) {
        BackHandler {
            searchText = ""
            viewmodel.searchMusicFromDeezer("")
        }
    }

    LaunchedEffect(auth.currentUser?.uid) {
        viewmodel.initLocalDatabase(context)
        if (auth.currentUser != null) {
            viewmodel.loadProfilePicture()
        }
        viewmodel.onRemoveFromMap = { radarViewModel.removeCurrentLocation() }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewmodel.startMusicAndEmitLocation(context, radarViewModel) }

    val sensorManager =
        remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    DisposableEffect(Unit) {
        val shakeDetector = ShakeDetector { viewmodel.playRandomSong() }
        sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(shakeDetector) }
    }

    ModalNavigationDrawer(
        modifier = Modifier.systemBarsPadding(),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1E1E1E),
                drawerContentColor = Color.White
            ) {
                Spacer(Modifier.height(32.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = profilePictureUrl
                            ?: "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png",
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { showImageSourceDialog = true }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Toca para cambiar foto", fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(Modifier.height(16.dp))
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
                    label = { Text("Mis Favoritos ❤️") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navigateToFavorites()
                    },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    label = { Text("Cerrar sesión") }, selected = false,
                    onClick = {
                        @Suppress("DEPRECATION")
                        val gso =
                            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()

                        @Suppress("DEPRECATION")
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
                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.Chat, "Chat")
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
                            if (it.play == true) {
                                viewmodel.startMusicAndEmitLocation(context, radarViewModel)
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
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

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it; viewmodel.searchMusicFromDeezer(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = {
                        Text(
                            stringResource(id = R.string.search_hint),
                            color = Color.Gray
                        )
                    },
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
                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Purple40)
                            }
                        }
                    } else {
                        if (searchText.isBlank()) {
                            item {
                                Text(
                                    stringResource(id = R.string.popular_artists),
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
                                    items(recommendedArtists) { artist ->
                                        ArtistItem(artist = artist, onItemSelected = {
                                            val artistName = artist.name.orEmpty()
                                            searchText = artistName
                                            viewmodel.searchMusicFromDeezer(artistName)
                                        })
                                    }
                                }
                            }
                            item {
                                Text(
                                    stringResource(id = R.string.top_hits),
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
                                val isFav = favorites.any { it.title == track.description }
                                TrackItem(
                                    track = track,
                                    isFavorite = isFav,
                                    onFavoriteToggle = { viewmodel.toggleFavorite(track) },
                                    onItemSelected = { viewmodel.addPlayer(track) }
                                )
                            }
                        } else {
                            items(searchResults) { track ->
                                val isFav = favorites.any { it.title == track.description }
                                TrackItem(
                                    track = track,
                                    isFavorite = isFav,
                                    onFavoriteToggle = { viewmodel.toggleFavorite(track) },
                                    onItemSelected = { viewmodel.addPlayer(track) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackItem(
    track: Artist,
    isFavorite: Boolean = false,
    onFavoriteToggle: () -> Unit = {},
    onItemSelected: () -> Unit
) {
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

        IconButton(onClick = { onFavoriteToggle() }) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorito",
                tint = if (isFavorite) Color.Red else Color.Gray
            )
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
            value = progress, onValueChange = onProgressChange,
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