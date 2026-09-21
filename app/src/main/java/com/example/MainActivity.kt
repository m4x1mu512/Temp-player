package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.service.PlaybackService
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PermissionScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start PlaybackService via standard startService.
        // MediaSessionService automatically transitions to foreground when media playback begins.
        try {
            val serviceIntent = Intent(this, PlaybackService::class.java)
            startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Process incoming intent if app was launched via file manager
        handleIncomingIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode) {
                var hasAudioPermission by remember {
                    mutableStateOf(checkAudioPermission())
                }

                val hasExternalIntent = remember {
                    intent?.action == Intent.ACTION_VIEW || intent?.action == Intent.ACTION_SEND
                }

                LaunchedEffect(hasAudioPermission) {
                    if (hasAudioPermission) {
                        viewModel.scanMusic()
                    }
                }

                if (!hasAudioPermission && !hasExternalIntent) {
                    PermissionScreen(
                        onPermissionGranted = {
                            hasAudioPermission = true
                            viewModel.scanMusic()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        val action = incomingIntent.action ?: return
        val uri: Uri? = when (action) {
            Intent.ACTION_VIEW -> incomingIntent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> null
        }

        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Not all URIs support persistable permissions; standard granted intent URI permission is active
            }
            viewModel.handleExternalAudioUri(uri)
        }
    }

    private fun checkAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        viewModel.navigateToPlayerEvent.collect {
            if (navController.currentDestination?.route != "player") {
                navController.navigate("player") {
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "library",
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Down,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable("library") {
            LibraryScreen(
                viewModel = viewModel,
                onNavigateToPlayer = { navController.navigate("player") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        composable("player") {
            PlayerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate("player") }
            )
        }
    }
}
