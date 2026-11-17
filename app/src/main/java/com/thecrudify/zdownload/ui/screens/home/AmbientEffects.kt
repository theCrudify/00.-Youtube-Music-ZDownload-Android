package com.thecrudify.zdownload.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.thecrudify.zdownload.ui.theme.AccentAmber
import com.thecrudify.zdownload.ui.theme.AccentBlue
import com.thecrudify.zdownload.ui.theme.AccentCyan
import com.thecrudify.zdownload.ui.theme.AccentViolet
import kotlin.random.Random

/* ==========================================================================
   Ambient Background with Floating Orbs & Notes
   ========================================================================== */
@Composable
internal fun AmbientBackground(isBusy: Boolean) {
    // Drift only while working; when idle the background is drawn once and stays static (battery)
    val transition = if (isBusy) rememberInfiniteTransition(label = "ambient") else null
    val orb1 = transition?.animateFloat(
        initialValue = -30f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1"
    )
    val orb2 = transition?.animateFloat(
        initialValue = 25f,
        targetValue = -35f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2"
    )

    val cBlue = AccentBlue
    val cViolet = AccentViolet
    val cCyan = AccentCyan

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val orb1Offset = orb1?.value ?: -30f
        val orb2Offset = orb2?.value ?: 25f

        // Orb 1: Sapphire Blue Top-Left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(cBlue.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(w * 0.15f + orb1Offset, h * 0.1f + orb1Offset),
                radius = w * 0.7f
            ),
            radius = w * 0.7f,
            center = Offset(w * 0.15f + orb1Offset, h * 0.1f + orb1Offset)
        )

        // Orb 2: Electric Violet Bottom-Right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(cViolet.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(w * 0.85f + orb2Offset, h * 0.85f + orb2Offset),
                radius = w * 0.75f
            ),
            radius = w * 0.75f,
            center = Offset(w * 0.85f + orb2Offset, h * 0.85f + orb2Offset)
        )

        // Orb 3: Cyan Ambient Center
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(cCyan.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(w * 0.5f, h * 0.45f)
        )
    }
}

/* ==========================================================================
   Particle Celebration Burst Animation
   ========================================================================== */
@Composable
internal fun ParticleBurstEffect(onFinish: () -> Unit) {
    val cyan = AccentCyan
    val blue = AccentBlue
    val amber = AccentAmber
    val violet = AccentViolet
    val particles = remember {
        List(30) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 250f + 100f
            Triple(
                Offset(kotlin.math.cos(angle) * speed, kotlin.math.sin(angle) * speed),
                listOf(cyan, blue, amber, violet).random(),
                Random.nextFloat() * 4f + 3f
            )
        }
    }

    val progress = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
        onFinish()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * 0.65f)
        particles.forEach { (targetOffset, color, radius) ->
            val curOffset = center + targetOffset * progress.value
            val alpha = (1f - progress.value).coerceIn(0f, 1f)
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius * (1f - progress.value * 0.5f),
                center = curOffset
            )
        }
    }
}
