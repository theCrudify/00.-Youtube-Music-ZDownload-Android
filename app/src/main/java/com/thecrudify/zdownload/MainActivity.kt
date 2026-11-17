package com.thecrudify.zdownload

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.thecrudify.zdownload.ui.screens.home.HomeScreen
import com.thecrudify.zdownload.ui.theme.ZDownloadTheme
import com.thecrudify.zdownload.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Handle shared link from YouTube App
        handleSharedIntent(intent)

        setContent {
            ZDownloadTheme {
                HomeScreen(viewModel = viewModel)
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
                // Extract URL from shared text (sometimes YouTube app sends: "Check out this song: https://youtu.be/...")
                val urlRegex = Regex("""https?://[^\s]+""")
                val foundUrl = urlRegex.find(sharedText)?.value ?: sharedText.trim()
                viewModel.onUrlChanged(foundUrl)
                viewModel.analyzeUrl()
            }
        }
    }
}
