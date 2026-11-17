package com.thecrudify.zdownload.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thecrudify.zdownload.data.db.Track
import com.thecrudify.zdownload.ui.theme.AccentAmber
import com.thecrudify.zdownload.ui.theme.AccentCyan
import com.thecrudify.zdownload.ui.theme.TextMuted
import com.thecrudify.zdownload.ui.theme.TextPrimary
import com.thecrudify.zdownload.ui.theme.TextSecondary
import com.thecrudify.zdownload.ui.tr

/**
 * One song in a list. Tap plays (or toggles selection while selecting), long-press starts selecting.
 * [trailing] holds the per-screen buttons (favorite, menu, reorder…).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selecting: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) AccentCyan.copy(alpha = 0.12f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selecting) {
            Icon(
                if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (selected) tr("Dipilih", "Selected") else tr("Belum dipilih", "Not selected"),
                tint = if (selected) AccentCyan else TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
        Artwork(track.entity.artworkPath, Modifier.size(48.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                color = if (isCurrent) AccentCyan else TextPrimary,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (track.entity.isMissing) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                    Text(tr("File hilang", "File missing"), color = AccentAmber, fontSize = 12.sp, maxLines = 1)
                } else {
                    Text(
                        listOf(track.artistName, track.album).filter { it.isNotBlank() }.joinToString(" • "),
                        color = TextSecondary, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
        Text(formatDuration(track.entity.durationMs), color = TextMuted, fontSize = 12.sp)
        trailing()
    }
}
