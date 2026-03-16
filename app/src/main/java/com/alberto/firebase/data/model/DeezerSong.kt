package com.alberto.firebase.data.model

import com.google.gson.annotations.SerializedName


data class DeezerResponse(
    val data: List<DeezerTrack>
)


data class DeezerTrack(
    val id: Long,
    val title: String,
    val preview: String,
    val artist: DeezerArtist,
    val album: DeezerAlbum
)


data class DeezerArtist(
    val name: String
)


data class DeezerAlbum(
    @SerializedName("cover_medium")
    val coverMedium: String
)