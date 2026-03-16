package com.alberto.firebase.utils // Cambia esto si lo pones en otra carpeta

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
    private var lastShakeTime: Long = 0

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculamos la fuerza G (Gravedad)
            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            // Fórmula matemática para saber la fuerza total del movimiento
            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            // Si la fuerza G es mayor a 2.7 (agitar con fuerza media-alta)
            if (gForce > 2.7f) {
                val now = System.currentTimeMillis()
                // Le ponemos un "cooldown" de 1 segundo para que no salte 50 veces seguidas
                if (now - lastShakeTime > 1000) {
                    lastShakeTime = now
                    onShake() // ¡Avisamos a la pantalla de que se ha agitado!
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No necesitamos hacer nada aquí
    }
}