package com.example.weard20.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun DiceShape(sides: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(140.dp)) {
        val width = size.width
        val height = size.height
        val center = size / 2f

        val path = Path()
        when (sides) {
            4 -> { // Triangle
                path.moveTo(width / 2f, 0f)
                path.lineTo(width, height)
                path.lineTo(0f, height)
                path.close()
            }
            6 -> { // Square
                path.addRect(androidx.compose.ui.geometry.Rect(0f, 0f, width, height))
            }
            8 -> { // Diamond
                path.moveTo(width / 2f, 0f)
                path.lineTo(width, height / 2f)
                path.lineTo(width / 2f, height)
                path.lineTo(0f, height / 2f)
                path.close()
            }
            10, 12, 20 -> { // Hexagon (Simplified for multiple polyhedrals)
                val radius = width / 2f
                for (i in 0..5) {
                    val angle = Math.toRadians((i * 60 - 30).toDouble())
                    val x = center.width + radius * Math.cos(angle).toFloat()
                    val y = center.height + radius * Math.sin(angle).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            100 -> { // Circle
                drawCircle(color = Color.DarkGray.copy(alpha = 0.3f), radius = width / 2f)
                drawCircle(color = Color.DarkGray, radius = width / 2f, style = Stroke(width = 2.dp.toPx()))
                return@Canvas
            }
        }
        drawPath(path = path, color = Color.DarkGray.copy(alpha = 0.3f))
        drawPath(path = path, color = Color.DarkGray, style = Stroke(width = 2.dp.toPx()))
    }
}