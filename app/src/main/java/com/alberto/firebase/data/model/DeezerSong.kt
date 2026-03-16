package com.alberto.firebase.data.model

import com.google.gson.annotations.SerializedName

// 1. La respuesta principal que contiene la lista de canciones en la variable "data"
data class DeezerResponse(
    val data: List<DeezerTrack>
)

// 2. La información de cada canción
data class DeezerTrack(
    val id: Long,
    val title: String,
    val preview: String, // ¡Este es el enlace al MP3 de 30 segundos!
    val artist: DeezerArtist,
    val album: DeezerAlbum
)

// 3. Deezer guarda el artista en un bloque separado
data class DeezerArtist(
    val name: String
)

// 4. Y la carátula viene dentro del bloque del álbum
data class DeezerAlbum(
    @SerializedName("cover_medium")
    val coverMedium: String // Usamos @SerializedName para pasarlo a formato Kotlin (sin guiones bajos)
)