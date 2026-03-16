package com.alberto.firebase.data.model

data class UserLocation(
    val email: String = "",
    val songTitle: String = "",
    val artistName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)