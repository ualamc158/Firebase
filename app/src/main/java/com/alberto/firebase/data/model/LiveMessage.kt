package com.alberto.firebase.data.model

data class LiveMessage(
    val id: String = "",
    val senderId: String = "",
    val senderEmail: String = "",
    val textContent: String = "",
    val timestamp: Long = 0L
)