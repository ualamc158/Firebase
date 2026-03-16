package com.alberto.firebase.presentation.homescreen

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alberto.firebase.data.model.Artist
import com.alberto.firebase.data.model.Player
import com.alberto.firebase.data.network.RetrofitClient
import com.alberto.firebase.presentation.map.SoundRadarViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    init {
        loadRecommendations()
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            try {
                val artistsRes = RetrofitClient.apiService.searchTracks("pop")
                _recommendedArtists.value = artistsRes.data.map {
                    Artist(name = it.artist.name, description = "Artista Popular", image = it.album.coverMedium)
                }.distinctBy { it.name }.take(10)

                val songsRes = RetrofitClient.apiService.searchTracks("top hits")
                _recommendedSongs.value = songsRes.data.map {
                    Artist(name = it.artist.name, description = it.title, image = it.album.coverMedium, audioUrl = it.preview)
                }.distinctBy { it.description }.take(15)
            } catch (e: Exception) { e.printStackTrace() }
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
    }

    fun onPlaySelected() {
        val currentPlayer = _player.value ?: return
        val isPlaying = currentPlayer.play ?: false
        val audioUrl = currentPlayer.artist?.audioUrl

        if (!isPlaying) {
            _player.value = currentPlayer.copy(play = true)
            playAudio(audioUrl)
        } else {
            _player.value = currentPlayer.copy(play = false)
            mediaPlayer?.pause()
        }
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
            }
        }
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

    fun onCancelSelected() {
        _player.value = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopProgressTracking()
        _songProgress.value = 0f
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        stopProgressTracking()
    }

    // 🌟 NUEVA FUNCIÓN MAESTRA: Une la música con el GPS
    fun startMusicAndEmitLocation(context: Context, radarViewModel: SoundRadarViewModel) {
        val currentArtist = _player.value?.artist ?: return

        // Primero lanzamos el play normal
        onPlaySelected()

        // Si el estado ha cambiado a "Play", emitimos al radar
        if (_player.value?.play == true) {
            radarViewModel.emitCurrentLocation(
                context = context,
                songTitle = currentArtist.description ?: "Desconocida",
                artistName = currentArtist.name ?: "Artista"
            )
        }
    }
}