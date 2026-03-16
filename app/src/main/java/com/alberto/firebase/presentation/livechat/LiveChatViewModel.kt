package com.alberto.firebase.presentation.livechat

import androidx.lifecycle.ViewModel
import com.alberto.firebase.data.model.LiveMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LiveChatViewModel(private val auth: FirebaseAuth) : ViewModel() {

    // Nodo único en tu base de datos para que no se mezcle con nada
    private val databaseRef = Firebase.database.reference.child("live_chat_board")

    private val _messages = MutableStateFlow<List<LiveMessage>>(emptyList())
    val messages: StateFlow<List<LiveMessage>> = _messages

    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    init {
        listenForLiveMessages()
    }

    private fun listenForLiveMessages() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val liveList = mutableListOf<LiveMessage>()
                for (child in snapshot.children) {
                    val msg = child.getValue(LiveMessage::class.java)
                    if (msg != null) {
                        liveList.add(msg.copy(id = child.key ?: ""))
                    }
                }
                // Ordenamos por fecha para que el chat baje de forma natural
                _messages.value = liveList.sortedBy { it.timestamp }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun sendLiveMessage(text: String) {
        if (text.isBlank()) return
        val user = auth.currentUser ?: return
        val messageId = databaseRef.push().key ?: return

        val newMsg = LiveMessage(
            id = messageId,
            senderId = user.uid,
            senderEmail = user.email ?: "SoundConnect User",
            textContent = text,
            timestamp = System.currentTimeMillis()
        )
        databaseRef.child(messageId).setValue(newMsg)
    }

    fun deleteLiveMessage(messageId: String) {
        databaseRef.child(messageId).removeValue()
    }
}