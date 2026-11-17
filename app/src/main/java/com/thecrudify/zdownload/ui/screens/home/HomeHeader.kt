package com.thecrudify.zdownload.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thecrudify.zdownload.ui.theme.AccentBlue
import com.thecrudify.zdownload.ui.theme.AccentCyan
import com.thecrudify.zdownload.ui.theme.AccentViolet
import com.thecrudify.zdownload.ui.theme.BgBase
import com.thecrudify.zdownload.ui.theme.BorderSubtle
import com.thecrudify.zdownload.ui.theme.TextMuted
import com.thecrudify.zdownload.ui.theme.TextPrimary
import com.thecrudify.zdownload.ui.theme.TextSecondary

/* ==========================================================================
   Top Navigation Bar with Vinyl Disc & Language Switcher
   ========================================================================== */
@Composable
internal fun TopBarSection(
    isBusy: Boolean,
    currentLang: AppLanguage,
    showCreatorTag: Boolean,
    onLangChanged: (AppLanguage) -> Unit
) {
    // Spin only while working: an idle infinite animation keeps the GPU drawing 60fps forever
    val spin = if (isBusy) {
        rememberInfiniteTransition(label = "disc_spin").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 850, easing = LinearEasing)
            ),
            label = "rotation"
        )
    } else null

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo + Vinyl Disc
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer { rotationZ = spin?.value ?: 0f }
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(AccentBlue, AccentCyan, AccentViolet, AccentBlue)
                        )
                    )
                    .border(BorderStroke(1.dp, Color(0x33FFFFFF)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl grooves
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .border(BorderStroke(0.5.dp, Color(0x22FFFFFF)), CircleShape)
                )
                // Center spindle hole
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(BgBase)
                )
            }

            Text(
                text = "ZDownload",
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 21.sp,
                letterSpacing = (-0.5).sp
            )
        }

        // Action Group: GitHub Pill & Language Switcher
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Creator Tag (hidden on small phones; the footer still credits the author)
            if (showCreatorTag) Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .border(BorderStroke(1.dp, BorderSubtle), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "by theCrudify",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Language Pill Switcher
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x14FFFFFF))
                    .border(BorderStroke(1.dp, BorderSubtle), CircleShape)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageButton(
                    label = "ID",
                    isSelected = currentLang == AppLanguage.ID,
                    onClick = { onLangChanged(AppLanguage.ID) }
                )
                LanguageButton(
                    label = "EN",
                    isSelected = currentLang == AppLanguage.EN,
                    onClick = { onLangChanged(AppLanguage.EN) }
                )
            }
        }
    }
}

@Composable
private fun LanguageButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgModifier = if (isSelected) {
        Modifier.background(
            Brush.horizontalGradient(listOf(AccentBlue, AccentCyan)),
            CircleShape
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .then(bgModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ==========================================================================
   Hero Header with Gradient Typography
   ========================================================================== */
@Composable
internal fun HeroHeader(currentLang: AppLanguage, titleSize: TextUnit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tag badge
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0x1F3B82F6))
                .border(BorderStroke(1.dp, Color(0x403B82F6)), CircleShape)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AccentCyan)
                )
                Text(
                    text = if (currentLang == AppLanguage.ID) "AUDIO EXTRACTOR STANDALONE" else "STANDALONE AUDIO EXTRACTOR",
                    color = Color(0xFF93C5FD),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (currentLang == AppLanguage.ID) "Musik favoritmu," else "Your favorite music,",
            color = TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = titleSize,
            lineHeight = titleSize * 1.15f,
            textAlign = TextAlign.Center,
            letterSpacing = (-1).sp
        )
        Text(
            text = if (currentLang == AppLanguage.ID) "kualitas terbaik." else "best available quality.",
            color = AccentCyan,
            fontWeight = FontWeight.ExtraBold,
            fontSize = titleSize,
            lineHeight = titleSize * 1.15f,
            textAlign = TextAlign.Center,
            letterSpacing = (-1).sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (currentLang == AppLanguage.ID)
                "Ekstrak langsung di HP tanpa server — lengkap dengan sampul album dan metadata."
            else
                "Extract audio on-device without a server — complete with album art and metadata.",
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
