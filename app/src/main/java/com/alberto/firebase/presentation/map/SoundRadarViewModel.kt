package com.alberto.firebase.presentation.map

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import com.alberto.firebase.data.model.UserLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SoundRadarViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance().getReference("nearby_listeners")
    private val auth = FirebaseAuth.getInstance()

    private val _usersNearby = MutableStateFlow<List<UserLocation>>(emptyList())
    val usersNearby: StateFlow<List<UserLocation>> = _usersNearby

    init {
        listenToNearbyUsers()
    }

    // 🌟 ESTA ES LA FUNCIÓN QUE TE DABA ERROR:
    @SuppressLint("MissingPermission")
    fun emitCurrentLocation(context: Context, songTitle: String, artistName: String) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Intentamos obtener la ubicación actual con alta precisión
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userId = auth.currentUser?.uid ?: return@addOnSuccessListener
                    val userEmail = auth.currentUser?.email ?: "Anónimo"

                    val myPresence = UserLocation(
                        email = userEmail,
                        songTitle = songTitle,
                        artistName = artistName,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )

                    // Guardamos la información en Realtime Database
                    database.child(userId).setValue(myPresence)
                }
            }
    }

    private fun listenToNearbyUsers() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locations = snapshot.children.mapNotNull { it.getValue(UserLocation::class.java) }
                _usersNearby.value = locations
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun removeCurrentLocation() {
        val userId = auth.currentUser?.uid ?: return
        database.child(userId).removeValue()
    }
}