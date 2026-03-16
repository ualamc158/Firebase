package com.alberto.firebase.presentation.livechat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alberto.firebase.data.model.LiveMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class LiveChatViewModel(private val auth: FirebaseAuth) : ViewModel() {

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


    fun sendImageMessage(context: Context, imageUri: Uri) {
        val user = auth.currentUser ?: return
        val messageId = databaseRef.push().key ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {

                val inputStream = context.contentResolver.openInputStream(imageUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                var rotatedBitmap = originalBitmap
                val exifStream = context.contentResolver.openInputStream(imageUri)
                if (exifStream != null) {
                    val exif = android.media.ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL
                    )
                    val matrix = android.graphics.Matrix()
                    when (orientation) {
                        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    rotatedBitmap = Bitmap.createBitmap(
                        originalBitmap, 0, 0,
                        originalBitmap.width, originalBitmap.height,
                        matrix, true
                    )
                    exifStream.close()
                }


                val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, 500, 500, true)
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
                val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)


                val newMsg = LiveMessage(
                    id = messageId,
                    senderId = user.uid,
                    senderEmail = user.email ?: "SoundConnect User",
                    textContent = "IMG:$base64String",
                    timestamp = System.currentTimeMillis()
                )
                databaseRef.child(messageId).setValue(newMsg)

            } catch (e: Exception) {
                Log.e("CHAT_ERROR", "Error al enviar imagen: ${e.message}")
            }
        }
    }

    fun deleteLiveMessage(messageId: String) {
        databaseRef.child(messageId).removeValue()
    }
}