package com.example.weard20.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WearD20App()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearD20App() {
    val context = LocalContext.current
    val diceTypes = listOf(4, 6, 8, 10, 12, 20, 100)

    var dieIndex by remember { mutableIntStateOf(5) }
    var rollResult by remember { mutableIntStateOf(20) }

    val currentMax = diceTypes[dieIndex]

    // MODERN VIBRATOR LOGIC
    val vibrator = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // SHAKE DETECTION LOGIC
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    fun performRoll() {
        rollResult = (1..currentMax).random()
        when (rollResult) {
            currentMax -> {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), intArrayOf(0, 255, 0, 255), -1))
            }
            1 -> {
                vibrator.vibrate(VibrationEffect.createOneShot(400, 150))
            }
            else -> {
                vibrator.vibrate(VibrationEffect.createOneShot(30, 100))
            }
        }
    }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            private var lastUpdate: Long = 0
            private var lastX = 0f
            private var lastY = 0f
            private var lastZ = 0f
            private val SHAKE_THRESHOLD = 800 // Sensitivity (lower is more sensitive)

            override fun onSensorChanged(event: SensorEvent) {
                val curTime = System.currentTimeMillis()
                if ((curTime - lastUpdate) > 100) {
                    val diffTime = curTime - lastUpdate
                    lastUpdate = curTime

                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val speed = sqrt(((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) + (z - lastZ) * (z - lastZ)).toDouble()) / diffTime * 10000

                    if (speed > SHAKE_THRESHOLD) {
                        performRoll()
                    }

                    lastX = x
                    lastY = y
                    lastZ = z
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .combinedClickable(
                onClick = { performRoll() },
                onLongClick = {
                    dieIndex = (dieIndex + 1) % diceTypes.size
                    rollResult = diceTypes[dieIndex]
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "d$currentMax",
                style = MaterialTheme.typography.caption1,
                color = Color.Cyan
            )

            val resultColor = when (rollResult) {
                currentMax -> Color.Green
                1 -> Color.Red
                else -> Color.White
            }

            Text(
                text = rollResult.toString(),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = resultColor
            )

            Text(
                text = "Shake to roll",
                style = MaterialTheme.typography.caption2,
                color = Color.LightGray
            )
        }
    }
}