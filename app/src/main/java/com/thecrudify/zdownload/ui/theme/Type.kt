package com.thecrudify.zdownload.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

// M3 default scale (Roboto); only titles get more weight so track names read as the anchor of each row.
// No colors here: a color would override Button/Surface contentColor.
private val base = Typography()

val Typography = base.copy(
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)
