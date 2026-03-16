package com.alberto.firebase.presentation.homescreen

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alberto.firebase.data.model.Artist
import com.alberto.firebase.data.model.Player
import com.alberto.firebase.data.network.RetrofitClient
import com.alberto.firebase.presentation.map.SoundRadarViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewmodel : ViewModel() {

    private val _recommendedArtists = MutableStateFlow<List<Artist>>(emptyList())
    val recommendedArtists: StateFlow<List<Artist>> = _recommendedArtists

    private val _recommendedSongs = MutableStateFlow<List<Artist>>(emptyList())
    val recommendedSongs: StateFlow<List<Artist>> = _recommendedSongs

    private val _searchResults = MutableStateFlow<List<Artist>>(emptyList())
    val searchResults: StateFlow<List<Artist>> = _searchResults

    private val _player = MutableStateFlow<Player?>(null)
    val player: StateFlow<Player?> = _player

    private val _songProgress = MutableStateFlow(0f)
    val songProgress: StateFlow<Float> = _songProgress

    // 🌟 ESTADO PARA LA FOTO DE PERFIL (Para la Captura Multimedia)
    private val _profilePictureUrl = MutableStateFlow<String?>(null)
    val profilePictureUrl: StateFlow<String?> = _profilePictureUrl

    // 🌟 CHIVATO PARA EL MAPA: Avisa cuando hay que borrar el marcador
    var onRemoveFromMap: (() -> Unit)? = null

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    init {
        loadRecommendations()
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            try {
                Log.d("DEEZER_API", "Iniciando peticiones en paralelo a Deezer...")

                // 1. Búsquedas específicas de Artistas Españoles
                val nombresArtistas = listOf(
                    "Estopa", "Melendi", "Fito y Fitipaldis",
                    "Dani Martín", "Aitana", "Rosalía",
                    "Alejandro Sanz", "Leiva", "Amaral", "C Tangana"
                )

                val peticionesArtistas = nombresArtistas.map { nombre ->
                    async {
                        try {
                            val respuesta = RetrofitClient.apiService.searchTracks(nombre)
                            respuesta.data.firstOrNull()
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                val tracksArtistas = peticionesArtistas.awaitAll().filterNotNull()
                Log.d("DEEZER_API", "¡Éxito! Artistas recibidos: ${tracksArtistas.size}")

                _recommendedArtists.value = tracksArtistas.map { track ->
                    Artist(
                        name = track.artist.name,
                        description = "Top España",
                        image = track.album.coverMedium
                    )
                }.distinctBy { it.name }.take(10)

                // 2. Búsqueda general para las canciones de abajo
                val songsRes = RetrofitClient.apiService.searchTracks("pop rock español")
                Log.d("DEEZER_API", "¡Éxito! Canciones recibidas: ${songsRes.data.size}")

                _recommendedSongs.value = songsRes.data.map {
                    Artist(
                        name = it.artist.name,
                        description = it.title,
                        image = it.album.coverMedium,
                        audioUrl = it.preview
                    )
                }.distinctBy { it.description }.take(15)

            } catch (e: Exception) {
                Log.e("DEEZER_ERROR", "Error crítico en la conexión: ${e.message}", e)
            }
        }
    }

    fun searchMusicFromDeezer(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.searchTracks(query)
                _searchResults.value = response.data.map { track ->
                    Artist(name = track.artist.name, description = track.title, image = track.album.coverMedium, audioUrl = track.preview)
                }.distinctBy { it.description }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun addPlayer(selectedArtist: Artist) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopProgressTracking()
        _player.value = Player(artist = selectedArtist, play = false)

        // Al cambiar de canción forzamos a que se quite del mapa hasta que le den a Play
        onRemoveFromMap?.invoke()
    }

    private fun playAudio(url: String?) {
        if (url == null) return
        if (mediaPlayer != null) {
            mediaPlayer?.start()
            startProgressTracking()
            return
        }

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener { start(); startProgressTracking() }
            setOnCompletionListener {
                _player.value = _player.value?.copy(play = false)
                stopProgressTracking()
                _songProgress.value = 0f

                // 🌟 Cuando la canción termina sola, desapareces del mapa
                onRemoveFromMap?.invoke()
            }
        }
    }

    // 🌟 FUNCIÓN MAESTRA DE REPRODUCCIÓN (Maneja Play, Pause y el Mapa)
    fun startMusicAndEmitLocation(context: Context, radarViewModel: SoundRadarViewModel) {
        val currentPlayer = _player.value ?: return
        val currentArtist = currentPlayer.artist ?: return
        val isPlaying = currentPlayer.play ?: false
        val audioUrl = currentArtist.audioUrl

        if (!isPlaying) {
            // AL DARLE AL PLAY
            _player.value = currentPlayer.copy(play = true)
            playAudio(audioUrl)

            // Apareces en el mapa
            radarViewModel.emitCurrentLocation(
                context = context,
                songTitle = currentArtist.description ?: "Desconocida",
                artistName = currentArtist.name ?: "Artista"
            )
        } else {
            // AL DARLE AL PAUSE
            _player.value = currentPlayer.copy(play = false)
            mediaPlayer?.pause()

            // 🌟 Desapareces del mapa al pausar
            onRemoveFromMap?.invoke()
        }
    }

    fun onCancelSelected() {
        _player.value = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopProgressTracking()
        _songProgress.value = 0f

        // 🌟 Desapareces del mapa al cerrar el reproductor
        onRemoveFromMap?.invoke()
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                if (mediaPlayer?.isPlaying == true) {
                    val current = mediaPlayer?.currentPosition?.toFloat() ?: 0f
                    val total = mediaPlayer?.duration?.toFloat() ?: 1f
                    if (total > 0) _songProgress.value = current / total
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracking() { progressJob?.cancel() }

    fun seekAudio(fraction: Float) {
        mediaPlayer?.let {
            val newPosition = (fraction * it.duration).toInt()
            it.seekTo(newPosition)
            _songProgress.value = fraction
        }
    }

    fun playRandomSong() {
        val allSongs = _recommendedSongs.value
        if (allSongs.isNotEmpty()) {
            val randomSong = allSongs.random()

            addPlayer(randomSong)

            val currentPlayer = _player.value
            if (currentPlayer != null) {
                _player.value = currentPlayer.copy(play = true)
                playAudio(randomSong.audioUrl)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        stopProgressTracking()
        onRemoveFromMap?.invoke()
    }

    // -------------------------------------------------------------------
    // 🌟 NUEVA SECCIÓN: CAPTURA MULTIMEDIA (Subir foto a Firebase Storage)
    // -------------------------------------------------------------------
    fun uploadProfilePicture(imageUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference
        val user = FirebaseAuth.getInstance().currentUser

        // Guarda la imagen en la carpeta 'profiles' con un nombre único
        val fileRef = storageRef.child("profiles/${user?.uid ?: UUID.randomUUID()}.jpg")

        fileRef.putFile(imageUri)
            .addOnSuccessListener {
                // Cuando se sube, pedimos la URL para mostrarla en pantalla
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    _profilePictureUrl.value = uri.toString()
                    Log.d("STORAGE", "Foto subida con éxito: $uri")
                }
            }
            .addOnFailureListener {
                Log.e("STORAGE", "Error al subir foto: ${it.message}")
            }
    }
}