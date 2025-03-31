package com.example.attendancemanagementapp.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun WaveAnimation(
    modifier: Modifier = Modifier.Companion,
    waveColor: Color = Color.Companion.Blue.copy(alpha = 0.3f),
    waveCount: Int = 3,
    waveRadius: Dp = 100.dp,
    isAnimating: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition()
    val waveScales = List(waveCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    delayMillis = index * 200,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "waveScale$index"
        )
    }

    if (isAnimating) {
        Canvas(modifier = modifier.size(waveRadius * 2)) {
            val center = Offset(size.width / 2, size.height / 2)
            waveScales.forEach { scale ->
                val animatedRadius = waveRadius.toPx() * scale.value
                val alpha = 1f - scale.value // Fade out as it expands
                drawCircle(
                    color = waveColor.copy(alpha = alpha),
                    radius = animatedRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}