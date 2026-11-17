package com.thecrudify.zdownload.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thecrudify.zdownload.data.model.AudioFormat
import com.thecrudify.zdownload.data.model.TrackInfo
import com.thecrudify.zdownload.ui.theme.AccentAmber
import com.thecrudify.zdownload.ui.theme.AccentBlue
import com.thecrudify.zdownload.ui.theme.AccentCyan
import com.thecrudify.zdownload.ui.theme.AccentViolet
import com.thecrudify.zdownload.ui.theme.BgBase
import com.thecrudify.zdownload.ui.theme.BgElevated
import com.thecrudify.zdownload.ui.theme.BorderHighlight
import com.thecrudify.zdownload.ui.theme.BorderSubtle
import com.thecrudify.zdownload.ui.theme.TextMuted
import com.thecrudify.zdownload.ui.theme.TextPrimary
import com.thecrudify.zdownload.ui.theme.TextSecondary

/* ==========================================================================
   Track Details Card (with Ambient Blurred Cover Glow & Tablet 2-Col Split)
   ========================================================================== */
@Composable
internal fun TrackDetailsCard(
    trackInfo: TrackInfo,
    selectedFormat: AudioFormat,
    isTablet: Boolean,
    currentLang: AppLanguage,
    onFormatSelected: (AudioFormat) -> Unit,
    onDownload: () -> Unit,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BgElevated),
        border = BorderStroke(1.dp, BorderHighlight),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Blurred Ambient Cover Glow in Background (Spotify / Apple Music style)
            AsyncImage(
                model = trackInfo.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .scale(1.2f)
                    .blur(65.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.32f
            )

            // Content Container
            if (isTablet) {
                // Tablet 2-Column Split Layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column: Album Art + Duration
                    Column(
                        modifier = Modifier.weight(0.42f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ThumbnailBox(trackInfo = trackInfo)
                        TrackMetaText(trackInfo = trackInfo, currentLang = currentLang)
                    }

                    // Right Column: Formats & Download Button
                    Column(
                        modifier = Modifier.weight(0.58f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FormatSelectorSection(
                            selectedFormat = selectedFormat,
                            currentLang = currentLang,
                            onFormatSelected = onFormatSelected
                        )

                        DownloadActionArea(
                            isDownloading = isDownloading,
                            downloadProgress = downloadProgress,
                            currentLang = currentLang,
                            onDownload = onDownload
                        )
                    }
                }
            } else {
                // Phone Vertical Stacked Layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ThumbnailBox(trackInfo = trackInfo)

                    TrackMetaText(trackInfo = trackInfo, currentLang = currentLang)

                    FormatSelectorSection(
                        selectedFormat = selectedFormat,
                        currentLang = currentLang,
                        onFormatSelected = onFormatSelected
                    )

                    DownloadActionArea(
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        currentLang = currentLang,
                        onDownload = onDownload
                    )
                }
            }
        }
    }
}

@Composable
private fun ThumbnailBox(trackInfo: TrackInfo) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0F1118))
            .border(BorderStroke(1.dp, BorderHighlight), RoundedCornerShape(18.dp))
    ) {
        AsyncImage(
            model = trackInfo.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Duration Badge Bottom-Right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xE606070B))
                .border(BorderStroke(0.5.dp, Color(0x33FFFFFF)), RoundedCornerShape(8.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(
                text = trackInfo.formattedDuration,
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TrackMetaText(trackInfo: TrackInfo, currentLang: AppLanguage) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (currentLang == AppLanguage.ID) "TRACK TERDETEKSI" else "TRACK DETECTED",
            color = AccentCyan,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = trackInfo.title,
            color = TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 24.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = trackInfo.uploader,
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FormatSelectorSection(
    selectedFormat: AudioFormat,
    currentLang: AppLanguage,
    onFormatSelected: (AudioFormat) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (currentLang == AppLanguage.ID) "PILIH FORMAT AUDIO" else "CHOOSE AUDIO FORMAT",
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        AudioFormat.values().forEach { format ->
            FormatCardItem(
                format = format,
                isSelected = selectedFormat == format,
                currentLang = currentLang,
                onSelect = { onFormatSelected(format) }
            )
        }
    }
}

@Composable
private fun FormatCardItem(
    format: AudioFormat,
    isSelected: Boolean,
    currentLang: AppLanguage,
    onSelect: () -> Unit
) {
    val accentColor = when (format) {
        AudioFormat.ORIGINAL -> AccentAmber
        AudioFormat.MP3 -> AccentCyan
        AudioFormat.FLAC -> AccentViolet
    }

    val borderColor = if (isSelected) accentColor else BorderSubtle
    val containerColor = if (isSelected) accentColor.copy(alpha = 0.12f) else Color(0x0CFFFFFF)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Radio Dot Indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(2.dp, if (isSelected) accentColor else TextMuted), CircleShape)
                            .background(if (isSelected) accentColor else Color.Transparent)
                    )

                    Text(
                        text = format.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (format) {
                                AudioFormat.ORIGINAL -> if (currentLang == AppLanguage.ID) "Stream Asli" else "Original Stream"
                                AudioFormat.MP3 -> "320 kbps"
                                AudioFormat.FLAC -> "Lossless"
                            },
                            color = accentColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (format) {
                        AudioFormat.ORIGINAL -> if (currentLang == AppLanguage.ID)
                            "Stream asli YouTube tanpa konversi ulang (Opus/M4A). Kualitas tertinggi."
                        else
                            "Untouched YouTube stream without re-encoding (Opus/M4A). Best quality."
                        AudioFormat.MP3 -> if (currentLang == AppLanguage.ID)
                            "Kompatibel di semua perangkat. Dikonversi ulang ke 320kbps."
                        else
                            "Plays on all devices. Re-encoded into 320kbps MP3."
                        AudioFormat.FLAC -> if (currentLang == AppLanguage.ID)
                            "Format lossless tanpa kompresi tambahan. File lebih besar."
                        else
                            "Lossless container without extra compression. Larger file size."
                    },
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(start = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadActionArea(
    isDownloading: Boolean,
    downloadProgress: Float,
    currentLang: AppLanguage,
    onDownload: () -> Unit
) {
    if (isDownloading) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LinearProgressIndicator(
                progress = { downloadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(CircleShape),
                color = AccentCyan,
                trackColor = Color(0x33FFFFFF)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (currentLang == AppLanguage.ID) "Mengekstrak audio..." else "Extracting audio...",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "${(downloadProgress * 100).toInt()}%",
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Button(
            onClick = onDownload,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = BgBase
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (currentLang == AppLanguage.ID) "Unduh Audio Sekarang" else "Download Audio Now",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        }
    }
}
