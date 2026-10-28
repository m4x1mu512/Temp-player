package com.example

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Choreographer
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.MaterialTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SettingsDataStore
import com.example.data.model.ThemeMode
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.service.PlaybackService
import com.example.ui.components.ExpandablePlayerSheet
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PermissionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.PLAYER_COLLAPSE_DURATION
import com.example.ui.util.PLAYER_EXPAND_DURATION
import com.example.ui.util.PLAYER_TRANSITION_DURATION
import com.example.ui.util.PlayerEmphasizedAccelerateEasing
import com.example.ui.util.PlayerEmphasizedDecelerateEasing
import com.example.ui.util.PlayerEmphasizedEasing
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var isServiceBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
        }
    }

    private var serviceStartPosted = false
    private var serviceStartPending = false

    override fun onStart() {
        super.onStart()
        viewModel.ensureVisualizerStarted()
        if (!serviceStartPosted) {
            serviceStartPosted = true
            serviceStartPending = true
            // Create the service after the first frame. Its onCreate builds ExoPlayer and the
            // media session, and both used to sit on the launch path between onCreate and the
            // first draw. MediaSessionService promotes itself to the foreground once playback
            // begins, so starting a frame later is safe; the player's pending-play mechanism
            // holds anything requested in the meantime.
            Choreographer.getInstance().postFrameCallback {
                window.decorView.post {
                    if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@post
                    serviceStartPending = false
                    startPlaybackService()
                    bindPlaybackService()
                }
            }
        } else {
            // Covers a start that was skipped because the activity left the foreground before
            // the deferred callback ran.
            if (serviceStartPending) {
                serviceStartPending = false
                startPlaybackService()
            }
            bindPlaybackService()
        }
    }

    private fun startPlaybackService() {
        try {
            startService(Intent(this, PlaybackService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun bindPlaybackService() {
        try {
            bindService(
                Intent(this, PlaybackService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialTheme = SettingsDataStore.getInitialThemeMode(this)
        val isSystemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val initialIsDark = when (initialTheme) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemDark
        }

        val windowBgColor = if (initialIsDark) 0xFF121316.toInt() else 0xFFFDFCFF.toInt()
        window.setBackgroundDrawable(ColorDrawable(windowBgColor))

        enableEdgeToEdge(
            statusBarStyle = if (initialIsDark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            },
            navigationBarStyle = if (initialIsDark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            }
        )

        // Process incoming intent if app was launched via file manager
        handleIncomingIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()
            val globalBackgroundEnabled by viewModel.globalBackgroundEnabled.collectAsStateWithLifecycle()
            val autoRotate by viewModel.autoRotate.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                com.example.data.model.ThemeMode.LIGHT -> false
                com.example.data.model.ThemeMode.DARK -> true
                com.example.data.model.ThemeMode.SYSTEM -> isSystemDark
            }

            DisposableEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    // Dark theme: status bar text and icons are white (isAppearanceLightStatusBars = false)
                    // Light theme: status bar text and icons are black (isAppearanceLightStatusBars = true)
                    isAppearanceLightStatusBars = !isDarkTheme
                    isAppearanceLightNavigationBars = !isDarkTheme
                }
                onDispose { }
            }

            DisposableEffect(autoRotate) {
                requestedOrientation = if (autoRotate) {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                onDispose { }
            }

            MyApplicationTheme(
                themeMode = themeMode,
                accentColor = accentColor,
                globalBackgroundEnabled = globalBackgroundEnabled
            ) {
                var hasAudioPermission by remember {
                    mutableStateOf(checkAudioPermission())
                }

                val hasExternalIntent = remember {
                    intent?.action == Intent.ACTION_VIEW || intent?.action == Intent.ACTION_SEND
                }

                if (!hasAudioPermission && !hasExternalIntent) {
                    PermissionScreen(
                        onPermissionGranted = {
                            hasAudioPermission = true
                            viewModel.onAudioPermissionGranted()
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
    // Record only the destination content. The mini-player is a sibling reader above it,
    // never part of the recorded layer, preventing recursive capture and self-blur.
    val appBackdrop = rememberLayerBackdrop()
    val backdropSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val navController = rememberNavController()
    var openingQueueFromPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(openingQueueFromPlayer) {
        if (openingQueueFromPlayer) {
            // Keep the queue transition lightweight only for this action.
            androidx.compose.runtime.withFrameNanos { }
            kotlinx.coroutines.delay(320)
            openingQueueFromPlayer = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToQueueEvent.collect {
            if (navController.currentDestination?.route != "library") {
                val popped = navController.popBackStack("library", inclusive = false)
                if (!popped) {
                    navController.navigate("library") {
                        popUpTo("library") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "library",
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (backdropSupported) {
                        Modifier
                            .layerBackdrop(appBackdrop)
                            // Keep the backdrop source transparent so the collapsed player
                            // reveals the actual library content instead of a solid theme-colored block.
                            .graphicsLayer()
                    } else {
                        Modifier
                    }
                ),
            enterTransition = {
                fadeIn(animationSpec = tween(220, easing = PlayerEmphasizedDecelerateEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180, easing = PlayerEmphasizedAccelerateEasing))
            }
        ) {
            composable(
                route = "library",
                enterTransition = {
                    if (openingQueueFromPlayer) {
                        EnterTransition.None
                    } else if (initialState.destination.route == "settings") {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(240, easing = PlayerEmphasizedDecelerateEasing),
                            initialOffset = { -(it * 0.16f).toInt() }
                        ) + scaleIn(
                            animationSpec = tween(240, easing = PlayerEmphasizedDecelerateEasing),
                            initialScale = 0.94f
                        ) + fadeIn(animationSpec = tween(180, easing = PlayerEmphasizedDecelerateEasing))
                    } else {
                        fadeIn(animationSpec = tween(220, easing = PlayerEmphasizedDecelerateEasing))
                    }
                },
                exitTransition = {
                    if (openingQueueFromPlayer) {
                        ExitTransition.None
                    } else if (targetState.destination.route == "settings") {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(200, easing = PlayerEmphasizedAccelerateEasing),
                            targetOffset = { -(it * 0.14f).toInt() }
                        ) + scaleOut(
                            animationSpec = tween(200, easing = PlayerEmphasizedAccelerateEasing),
                            targetScale = 0.94f
                        ) + fadeOut(animationSpec = tween(160, easing = PlayerEmphasizedAccelerateEasing))
                    } else {
                        fadeOut(animationSpec = tween(180, easing = PlayerEmphasizedAccelerateEasing))
                    }
                },
                popEnterTransition = {
                    if (openingQueueFromPlayer) {
                        EnterTransition.None
                    } else slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(240, easing = PlayerEmphasizedDecelerateEasing),
                        initialOffset = { -(it * 0.16f).toInt() }
                    ) + scaleIn(
                        animationSpec = tween(240, easing = PlayerEmphasizedDecelerateEasing),
                        initialScale = 0.94f
                    ) + fadeIn(animationSpec = tween(180, easing = PlayerEmphasizedDecelerateEasing))
                }
            ) {
                LibraryScreen(
                    viewModel = viewModel,
                    onNavigateToPlayer = { viewModel.requestNavigateToPlayer() },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }

            composable(
                route = "settings",
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(240, easing = PlayerEmphasizedDecelerateEasing),
                        initialOffset = { (it * 0.18f).toInt() }
                    ) + scaleIn(
                        animationSpec = tween(240, easing = PlayerEmphasizedDecelerateEasing),
                        initialScale = 0.92f
                    ) + fadeIn(animationSpec = tween(180, easing = PlayerEmphasizedDecelerateEasing))
                },
                exitTransition = {
                    if (openingQueueFromPlayer) {
                        ExitTransition.None
                    } else slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(200, easing = PlayerEmphasizedAccelerateEasing),
                        targetOffset = { -(it * 0.14f).toInt() }
                    ) + scaleOut(
                        animationSpec = tween(200, easing = PlayerEmphasizedAccelerateEasing),
                        targetScale = 0.94f
                    ) + fadeOut(animationSpec = tween(160, easing = PlayerEmphasizedAccelerateEasing))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(240, easing = PlayerEmphasizedDecelerateEasing),
                        initialOffset = { -(it * 0.16f).toInt() }
                    ) + scaleIn(
                        animationSpec = tween(240, easing = PlayerEmphasizedDecelerateEasing),
                        initialScale = 0.94f
                    ) + fadeIn(animationSpec = tween(180, easing = PlayerEmphasizedDecelerateEasing))
                },
                popExitTransition = {
                    if (openingQueueFromPlayer) {
                        ExitTransition.None
                    } else slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(200, easing = PlayerEmphasizedAccelerateEasing),
                        targetOffset = { (it * 0.18f).toInt() }
                    ) + scaleOut(
                        animationSpec = tween(200, easing = PlayerEmphasizedAccelerateEasing),
                        targetScale = 0.92f
                    ) + fadeOut(animationSpec = tween(160, easing = PlayerEmphasizedAccelerateEasing))
                }
            ) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { viewModel.requestNavigateToPlayer() }
                )
            }
        }

        ExpandablePlayerSheet(
            viewModel = viewModel,
            backdrop = if (backdropSupported) appBackdrop else null,
            onNavigateToQueue = {
                openingQueueFromPlayer = true
                if (navController.currentDestination?.route != "library") {
                    navController.popBackStack("library", inclusive = false)
                }
                viewModel.openPlaybackQueue()
            },
            onNavigateToSettings = {
                navController.navigate("settings")
            },
        )
    }
}
