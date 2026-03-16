package com.alberto.firebase.presentation.homescreen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alberto.firebase.data.model.Artist
import com.alberto.firebase.data.model.Player
import com.alberto.firebase.data.network.RetrofitClient
import com.alberto.firebase.presentation.map.SoundRadarViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

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

    // 🌟 AHORA ES 'Any?' PORQUE PUEDE GUARDAR UN TEXTO (URL) O UNA FOTO REAL (Bitmap)
    private val _profilePictureUrl = MutableStateFlow<Any?>(null)
    val profilePictureUrl: StateFlow<Any?> = _profilePictureUrl

    var onRemoveFromMap: (() -> Unit)? = null

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    init {
        loadRecommendations()
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            try {
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

                _recommendedArtists.value = tracksArtistas.map { track ->
                    Artist(
                        name = track.artist.name,
                        description = "Top España",
                        image = track.album.coverMedium
                    )
                }.distinctBy { it.name }.take(10)

                val songsRes = RetrofitClient.apiService.searchTracks("pop rock español")

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
                onRemoveFromMap?.invoke()
            }
        }
    }

    fun startMusicAndEmitLocation(context: Context, radarViewModel: SoundRadarViewModel) {
        val currentPlayer = _player.value ?: return
        val currentArtist = currentPlayer.artist ?: return
        val isPlaying = currentPlayer.play ?: false
        val audioUrl = currentArtist.audioUrl

        if (!isPlaying) {
            _player.value = currentPlayer.copy(play = true)
            playAudio(audioUrl)
            radarViewModel.emitCurrentLocation(context, currentArtist.description ?: "Desconocida", currentArtist.name ?: "Artista")
        } else {
            _player.value = currentPlayer.copy(play = false)
            mediaPlayer?.pause()
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
    // 🌟 SECCIÓN: CAPTURA MULTIMEDIA (El truco del Base64)
    // -------------------------------------------------------------------
    fun uploadProfilePicture(context: Context, imageUri: Uri) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        // 1. Actualización optimista: lo mostramos en pantalla enseguida
        _profilePictureUrl.value = imageUri

        // 2. Comprimimos y subimos por detrás en un hilo secundario
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Leemos el archivo original
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                // 🌟 SOLUCIÓN A LA ROTACIÓN: Leemos la etiqueta EXIF de la cámara
                var rotatedBitmap = originalBitmap
                val exifStream = context.contentResolver.openInputStream(imageUri)
                if (exifStream != null) {
                    val exif = android.media.ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL
                    )
                    val matrix = android.graphics.Matrix()
                    // Si la foto dice que está girada, la enderezamos con la matriz
                    when (orientation) {
                        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    // Aplicamos el giro definitivo
                    rotatedBitmap = Bitmap.createBitmap(
                        originalBitmap, 0, 0,
                        originalBitmap.width, originalBitmap.height,
                        matrix, true
                    )
                    exifStream.close()
                }

                // Lo hacemos más pequeño (400x400) para no bloquear la base de datos
                val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, 400, 400, true)

                // Lo convertimos en un texto gigante
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
                val imageBytes = outputStream.toByteArray()
                val base64String = Base64.encodeToString(imageBytes, Base64.DEFAULT)

                // Lo guardamos en Realtime Database (¡Gratis!)
                val database = FirebaseDatabase.getInstance().reference
                database.child("users").child(user.uid).child("profilePicture").setValue(base64String)
                    .addOnSuccessListener {
                        Log.d("DB_PROFILE", "¡Foto subida como texto a Realtime Database!")
                    }
            } catch (e: Exception) {
                Log.e("DB_PROFILE", "Error al procesar la imagen: ${e.message}")
            }
        }
    }

    fun loadProfilePicture() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val database = FirebaseDatabase.getInstance().reference

        // Buscamos el texto gigante en la base de datos
        database.child("users").child(user.uid).child("profilePicture").get()
            .addOnSuccessListener { snapshot ->
                val base64String = snapshot.getValue(String::class.java)
                if (base64String != null) {
                    try {
                        // Reconstruimos la foto a partir del texto
                        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                        // Coil pinta el Bitmap maravillosamente
                        _profilePictureUrl.value = bitmap
                    } catch (e: Exception) {
                        Log.e("DB_PROFILE", "Error reconstruyendo foto: ${e.message}")
                    }
                }
            }
    }
}