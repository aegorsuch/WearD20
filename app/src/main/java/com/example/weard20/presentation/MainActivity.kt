package com.example.weard20.presentation

import android.content.Context
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

    // MODERN VIBRATOR LOGIC: Fixes the deprecation error
    val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .combinedClickable(
                onClick = {
                    rollResult = (1..currentMax).random()

                    when (rollResult) {
                        currentMax -> {
                            // Crit Success: Double Pulse
                            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), intArrayOf(0, 255, 0, 255), -1))
                        }
                        1 -> {
                            // Crit Fail: Long Buzz
                            vibrator.vibrate(VibrationEffect.createOneShot(400, 150))
                        }
                        else -> {
                            // Normal: Short tap
                            vibrator.vibrate(VibrationEffect.createOneShot(30, 100))
                        }
                    }
                },
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
                text = "Hold to change",
                style = MaterialTheme.typography.caption2,
                color = Color.LightGray
            )
        }
    }
}