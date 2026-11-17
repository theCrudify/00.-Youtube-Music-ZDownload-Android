package com.thecrudify.zdownload

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.thecrudify.zdownload.ui.LocaleHelper
import com.thecrudify.zdownload.ui.LocalLang
import com.thecrudify.zdownload.ui.player.ExpandedPlayer
import com.thecrudify.zdownload.ui.player.MiniPlayer
import com.thecrudify.zdownload.ui.screens.home.HomeScreen
import com.thecrudify.zdownload.ui.screens.library.LibraryScreen
import com.thecrudify.zdownload.ui.screens.playlists.PlaylistsScreen
import com.thecrudify.zdownload.ui.screens.settings.SettingsScreen
import com.thecrudify.zdownload.ui.theme.ZDownloadTheme
import com.thecrudify.zdownload.ui.tr
import com.thecrudify.zdownload.viewmodel.LibraryViewModel
import com.thecrudify.zdownload.viewmodel.MainViewModel

enum class MainTab { HOME, LIBRARY, PLAYLISTS }

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Ensure playback controller is connected
        (application as ZDownloadApp).playback.connect()

        // Handle shared link from YouTube App
        handleSharedIntent(intent)

        setContent {
            val themeMode by libraryViewModel.themeMode.collectAsState()
            val languageCode by libraryViewModel.language.collectAsState()

            val localizedContext = remember(languageCode) {
                LocaleHelper.applyLocale(this@MainActivity, languageCode)
            }
            val appLanguage = remember(languageCode) {
                LocaleHelper.toAppLanguage(languageCode)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalLang provides appLanguage,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides this@MainActivity,
                androidx.activity.compose.LocalOnBackPressedDispatcherOwner provides this@MainActivity
            ) {
                ZDownloadTheme(themeMode = themeMode) {
                    MainAppScreen(
                        mainVm = mainViewModel,
                        libraryVm = libraryViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val urlRegex = Regex("""https?://[^\s]+""")
                val foundUrl = urlRegex.find(sharedText)?.value ?: sharedText.trim()
                mainViewModel.onSharedLink(foundUrl)
            }
        }
    }
}

@Composable
fun MainAppScreen(
    mainVm: MainViewModel,
    libraryVm: LibraryViewModel
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val playerState by libraryVm.playback.state.collectAsState()
    val allTracks by libraryVm.allTracks.collectAsState()
    val currentTrack = remember(playerState.current?.trackId, allTracks) {
        allTracks.firstOrNull { it.id == playerState.current?.trackId }
    }

    // Permission request for Android 13+ notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                mainVm.getApplication(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Switch to Home tab when a link is shared from outside
    LaunchedEffect(mainVm.sharedLinks) {
        mainVm.sharedLinks.collect {
            selectedTab = MainTab.HOME
            showSettings = false
        }
    }

    // Show messages as snackbars
    LaunchedEffect(libraryVm.messages) {
        libraryVm.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = isPlayerExpanded) {
        isPlayerExpanded = false
    }
    BackHandler(enabled = showSettings && !isPlayerExpanded) {
        showSettings = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            // Each screen owns its status-bar inset (TopAppBar / statusBarsPadding); the bottom bar owns the nav-bar inset.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // NavigationBar pads for the system nav bar itself; when it's hidden (Settings) the column must.
                Column(if (showSettings) Modifier.navigationBarsPadding() else Modifier) {
                    // Persistent Mini Player above Navigation Bar
                    AnimatedVisibility(
                        visible = playerState.current != null && !isPlayerExpanded,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                    ) {
                        MiniPlayer(
                            playback = libraryVm.playback,
                            state = playerState,
                            onExpand = { isPlayerExpanded = true }
                        )
                    }

                    if (!showSettings) {
                        NavigationBar {
                            NavItem(selectedTab == MainTab.HOME, { selectedTab = MainTab.HOME }, Icons.Default.Download, stringResource(R.string.nav_downloads))
                            NavItem(selectedTab == MainTab.LIBRARY, { selectedTab = MainTab.LIBRARY }, Icons.Default.LibraryMusic, stringResource(R.string.nav_library))
                            NavItem(selectedTab == MainTab.PLAYLISTS, { selectedTab = MainTab.PLAYLISTS }, Icons.AutoMirrored.Filled.QueueMusic, stringResource(R.string.nav_playlists))
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showSettings) {
                    SettingsScreen(libraryVm = libraryVm, mainVm = mainVm, onBack = { showSettings = false })
                } else when (selectedTab) {
                    MainTab.HOME -> HomeScreen(viewModel = mainVm, libraryVm = libraryVm, onOpenSettings = { showSettings = true })
                    MainTab.LIBRARY -> LibraryScreen(vm = libraryVm)
                    MainTab.PLAYLISTS -> PlaylistsScreen(vm = libraryVm)
                }
            }
        }

        // Full Screen Expanded Player Animated Overlay
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            ExpandedPlayer(
                playback = libraryVm.playback,
                state = playerState,
                isFavorite = currentTrack?.isFavorite ?: false,
                lyrics = currentTrack?.entity?.lyrics,
                onToggleFavorite = {
                    currentTrack?.let { libraryVm.toggleFavorite(it) }
                },
                onCollapse = { isPlayerExpanded = false },
                onEditLyrics = null
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(selected: Boolean, onClick: () -> Unit, icon: ImageVector, label: String) {
    NavigationBarItem(selected = selected, onClick = onClick, icon = { Icon(icon, contentDescription = null) }, label = { Text(label) })
}
