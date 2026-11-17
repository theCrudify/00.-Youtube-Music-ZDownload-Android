package com.thecrudify.zdownload.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.thecrudify.zdownload.ui.theme.AccentBlue
import com.thecrudify.zdownload.ui.theme.AccentCyan

@Composable
fun EqualizerVisualizer(
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 12
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            EqualizerBar(index = i, isAnimating = isAnimating)
        }
    }
}

@Composable
private fun EqualizerBar(index: Int, isAnimating: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer_$index")
    val duration = (400 + (index * 60) % 300)

    val heightFactor by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "height_$index"
    )

    val currentHeight = if (isAnimating) (6 + heightFactor * 22).dp else 6.dp

    Box(
        modifier = Modifier
            .width(3.5.dp)
            .height(currentHeight)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(AccentCyan, AccentBlue)
                )
            )
    )
}
