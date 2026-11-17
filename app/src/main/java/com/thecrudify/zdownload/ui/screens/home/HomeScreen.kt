package com.thecrudify.zdownload.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thecrudify.zdownload.data.model.DownloadState
import com.thecrudify.zdownload.ui.components.EqualizerVisualizer
import com.thecrudify.zdownload.ui.theme.AccentCyan
import com.thecrudify.zdownload.ui.theme.BgBase
import com.thecrudify.zdownload.ui.theme.TextMuted
import com.thecrudify.zdownload.ui.theme.TextSecondary
import com.thecrudify.zdownload.viewmodel.MainViewModel

// Language Strings Dictionary
enum class AppLanguage { ID, EN }

/** Width buckets: <360dp small phones (4"), <600dp phones, >=600dp tablets; two-pane on wide landscape. */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val url by viewModel.url.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val scrollState = rememberScrollState()

    var currentLang by remember { mutableStateOf(AppLanguage.ID) }
    val isBusy = downloadState is DownloadState.FetchingInfo || downloadState is DownloadState.Downloading

    // Particle burst trigger on download success
    var showParticles by remember { mutableStateOf(false) }
    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Success) {
            showParticles = true
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BgBase)
    ) {
        val isCompact = maxWidth < 360.dp
        val isTablet = maxWidth >= 600.dp
        // Two-pane only when wide AND landscape; tall 13" portrait reads better as one column
        val isExpanded = maxWidth >= 840.dp && maxWidth > maxHeight
        val contentMaxWidth = if (isExpanded) 1280.dp else 760.dp
        val hPad = when {
            isCompact -> 16.dp
            isTablet -> 32.dp
            else -> 20.dp
        }
        // Right pane in two-pane mode gets ~58% of the content width
        val paneIsWide = isExpanded && (minOf(maxWidth, contentMaxWidth) - hPad * 2) * 0.58f >= 560.dp

        val inputArea: @Composable () -> Unit = {
            HeroHeader(
                currentLang = currentLang,
                titleSize = when {
                    isCompact -> 26.sp
                    isTablet -> 40.sp
                    else -> 32.sp
                }
            )
            Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 26.dp))
            InputSection(
                url = url,
                isBusy = isBusy,
                currentLang = currentLang,
                onUrlChanged = viewModel::onUrlChanged,
                onClear = viewModel::clearUrl,
                onPaste = viewModel::pasteFromClipboard,
                onAnalyze = viewModel::analyzeUrl
            )

            // Live Equalizer Visualizer while processing
            AnimatedVisibility(
                visible = isBusy,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                val statusText = when (val state = downloadState) {
                    is DownloadState.FetchingInfo -> state.message
                    is DownloadState.Downloading -> state.statusMessage
                    else -> ""
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EqualizerVisualizer(isAnimating = true)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = statusText,
                        color = AccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (downloadState is DownloadState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                ErrorBanner(message = (downloadState as DownloadState.Error).message)
            }
        }

        // Steps, loaded track card or success card
        val mainContent: @Composable (wide: Boolean) -> Unit = { wide ->
            when (val state = downloadState) {
                is DownloadState.Idle, is DownloadState.FetchingInfo, is DownloadState.Error -> {
                    IdleStepsSection(isTablet = wide, currentLang = currentLang)
                }
                is DownloadState.TrackLoaded -> {
                    TrackDetailsCard(
                        trackInfo = state.trackInfo,
                        selectedFormat = selectedFormat,
                        isTablet = wide,
                        currentLang = currentLang,
                        onFormatSelected = viewModel::selectFormat,
                        onDownload = { viewModel.startDownload(state.trackInfo) }
                    )
                }
                is DownloadState.Downloading -> {
                    TrackDetailsCard(
                        trackInfo = state.trackInfo,
                        selectedFormat = selectedFormat,
                        isTablet = wide,
                        currentLang = currentLang,
                        onFormatSelected = {},
                        onDownload = {},
                        isDownloading = true,
                        downloadProgress = state.progress
                    )
                }
                is DownloadState.Success -> {
                    SuccessCard(
                        state = state,
                        currentLang = currentLang,
                        onDownloadAgain = { viewModel.startDownload(state.trackInfo) }
                    )
                }
            }
        }

        // Ambient animated background (Floating Orbs + Grid)
        AmbientBackground(isBusy = isBusy)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = contentMaxWidth)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = hPad, vertical = if (isTablet) 28.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopBarSection(
                    isBusy = isBusy,
                    currentLang = currentLang,
                    showCreatorTag = !isCompact,
                    onLangChanged = { currentLang = it }
                )

                Spacer(modifier = Modifier.height(if (isTablet) 36.dp else 24.dp))

                if (isExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(0.42f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) { inputArea() }
                        Column(modifier = Modifier.weight(0.58f)) { mainContent(paneIsWide) }
                    }
                } else {
                    inputArea()
                    Spacer(modifier = Modifier.height(24.dp))
                    mainContent(isTablet)
                }

                Spacer(modifier = Modifier.height(38.dp))
                FooterSection(currentLang = currentLang)
            }
        }

        // Particle Celebration Overlay on Success
        if (showParticles) {
            ParticleBurstEffect(onFinish = { showParticles = false })
        }
    }
}

/* ==========================================================================
   Footer Attribution
   ========================================================================== */
@Composable
private fun FooterSection(currentLang: AppLanguage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (currentLang == AppLanguage.ID) "Dibuat dengan ❤️ oleh theCrudify" else "Crafted with ❤️ by theCrudify",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "ZDownload Standalone Mobile Edition",
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
