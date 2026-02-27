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
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

enum class RollModifier {
    NORMAL, ADVANTAGE, DISADVANTAGE
}

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
    val scope = rememberCoroutineScope()
    val diceTypes = listOf(4, 6, 8, 10, 12, 20, 100)

    var dieIndex by remember { mutableIntStateOf(5) }
    var rollResult by remember { mutableIntStateOf(20) }
    var subRolls by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var modifierMode by remember { mutableStateOf(RollModifier.NORMAL) }
    var isRolling by remember { mutableStateOf(false) }

    // Use a derived state for currentMax to ensure it always matches dieIndex
    val currentMax by remember { derivedStateOf { diceTypes[dieIndex] } }

    val vibrator = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    fun performRoll() {
        if (isRolling) return
        
        scope.launch {
            isRolling = true
            val startTime = System.currentTimeMillis()
            
            // Rolling Animation: Use the actual currentMax
            while (System.currentTimeMillis() - startTime < 600) {
                rollResult = (1..currentMax).random()
                vibrator.vibrate(VibrationEffect.createOneShot(15, 60))
                delay(50)
            }

            // Capture the specific currentMax for the final roll to be extra safe
            val targetMax = currentMax

            if (modifierMode == RollModifier.NORMAL) {
                rollResult = (1..targetMax).random()
                subRolls = null
            } else {
                val r1 = (1..targetMax).random()
                val r2 = (1..targetMax).random()
                subRolls = r1 to r2
                rollResult = if (modifierMode == RollModifier.ADVANTAGE) maxOf(r1, r2) else minOf(r1, r2)
            }

            // Reset modifier after roll
            modifierMode = RollModifier.NORMAL

            when (rollResult) {
                targetMax -> vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), intArrayOf(0, 255, 0, 255), -1))
                1 -> vibrator.vibrate(VibrationEffect.createOneShot(400, 150))
                else -> vibrator.vibrate(VibrationEffect.createOneShot(30, 100))
            }
            isRolling = false
        }
    }

    val draggableState = rememberDraggableState { delta ->
        if (delta > 40) { // Swipe Right -> Advantage (Now on Right)
            modifierMode = if (modifierMode == RollModifier.ADVANTAGE) RollModifier.NORMAL else RollModifier.ADVANTAGE
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (delta < -40) { // Swipe Left -> Disadvantage (Now on Left)
            modifierMode = if (modifierMode == RollModifier.DISADVANTAGE) RollModifier.NORMAL else RollModifier.DISADVANTAGE
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hint")
    val hintAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintAlpha"
    )

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            private var lastUpdate: Long = 0
            private var lastRollTime: Long = 0
            private var lastX = 0f; private var lastY = 0f; private var lastZ = 0f
            override fun onSensorChanged(event: SensorEvent) {
                val curTime = System.currentTimeMillis()
                if ((curTime - lastUpdate) > 100) {
                    val diffTime = curTime - lastUpdate; lastUpdate = curTime
                    val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                    val speed = sqrt(((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) + (z - lastZ) * (z - lastZ)).toDouble()) / diffTime * 10000
                    if (speed > 850 && (curTime - lastRollTime) > 1500) {
                        lastRollTime = curTime
                        performRoll()
                    }
                    lastX = x; lastY = y; lastZ = z
                }
            }
            override fun onAccuracyChanged(s: Sensor, a: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .draggable(state = draggableState, orientation = Orientation.Horizontal)
    ) {
        // Main Interaction Layer (Roll)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = { performRoll() },
                    onLongClick = {
                        if (!isRolling) {
                            dieIndex = (dieIndex + 1) % diceTypes.size
                            rollResult = diceTypes[dieIndex]
                            subRolls = null
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                DiceShape(sides = currentMax)
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Current Die Display
                    Text(
                        text = "d$currentMax",
                        style = MaterialTheme.typography.caption1,
                        color = Color.Cyan.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // ADV/DIS Prominent Label
                    if (modifierMode != RollModifier.NORMAL || isRolling) {
                         val displayMode = if (modifierMode == RollModifier.ADVANTAGE) "ADVANTAGE" 
                                           else if (modifierMode == RollModifier.DISADVANTAGE) "DISADVANTAGE" 
                                           else ""

                        if (displayMode.isNotEmpty()) {
                            Text(
                                text = displayMode,
                                style = MaterialTheme.typography.button,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (displayMode == "ADVANTAGE") Color.Green else Color.Red
                            )
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (subRolls != null && !isRolling) {
                        Text(
                            text = "${subRolls!!.first} & ${subRolls!!.second}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    val resultColor = when {
                        isRolling -> Color.White
                        rollResult == currentMax -> Color.Green
                        rollResult == 1 -> Color.Red
                        else -> Color.White
                    }

                    Text(
                        text = rollResult.toString(),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        color = resultColor
                    )

                    Text(
                        text = if (isRolling) "Rolling..." else "Shake to roll",
                        style = MaterialTheme.typography.caption2,
                        color = Color.DarkGray
                    )
                }
            }
        }

        // Side Buttons for Advantage/Disadvantage (FLIPPED)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DISADVANTAGE Area (Left side of screen)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .clickable {
                        if (!isRolling) {
                            modifierMode = if (modifierMode == RollModifier.DISADVANTAGE) RollModifier.NORMAL else RollModifier.DISADVANTAGE
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                        }
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("<", color = Color.Gray.copy(alpha = hintAlpha), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("-", color = Color.Red.copy(alpha = hintAlpha), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // ADVANTAGE Area (Right side of screen)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .clickable {
                        if (!isRolling) {
                            modifierMode = if (modifierMode == RollModifier.ADVANTAGE) RollModifier.NORMAL else RollModifier.ADVANTAGE
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                        }
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(">", color = Color.Gray.copy(alpha = hintAlpha), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("+", color = Color.Green.copy(alpha = hintAlpha), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}