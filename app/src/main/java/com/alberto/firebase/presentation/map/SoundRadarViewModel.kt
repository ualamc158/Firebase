package com.alberto.firebase.presentation.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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


    private val notifiedUsers = mutableSetOf<String>()

    init {
        listenToNearbyUsers()
    }

    @SuppressLint("MissingPermission")
    fun emitCurrentLocation(context: Context, songTitle: String, artistName: String) {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

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

                        database.child(userId).setValue(myPresence)
                    }
                }
        } catch (e: SecurityException) {

        }
    }

    private fun listenToNearbyUsers() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locations =
                    snapshot.children.mapNotNull { it.getValue(UserLocation::class.java) }
                _usersNearby.value = locations
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun removeCurrentLocation() {
        val userId = auth.currentUser?.uid ?: return
        database.child(userId).removeValue()
    }


    @SuppressLint("MissingPermission")
    fun checkProximity(context: Context) {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { myLoc: Location? ->
                if (myLoc != null) {
                    val currentEmail = auth.currentUser?.email
                    val others = _usersNearby.value

                    for (user in others) {
                        if (user.email == currentEmail) continue


                        val results = FloatArray(1)
                        Location.distanceBetween(
                            myLoc.latitude,
                            myLoc.longitude,
                            user.latitude,
                            user.longitude,
                            results
                        )
                        val distanceInMeters = results[0]

                        if (distanceInMeters < 50) {
                            val userEmailKey = user.email ?: continue
                            if (!notifiedUsers.contains(userEmailKey)) {
                                sendNotification(context, user)
                                notifiedUsers.add(userEmailKey)
                            }
                        } else {

                            user.email?.let { notifiedUsers.remove(it) }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("RADAR", "Sin permisos para comprobar proximidad")
        }
    }


    private fun sendNotification(context: Context, user: UserLocation) {
        val channelId = "music_tags_channel"


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Music Tags Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_map) // Icono por defecto de Android
            .setContentTitle("¡Alguien cerca de ti!")
            .setContentText("Está sonando: ${user.songTitle} de ${user.artistName}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Verificamos el permiso en tiempo real antes de lanzar la notificación
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val notificationId = user.email?.hashCode() ?: System.currentTimeMillis().toInt()
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }
}