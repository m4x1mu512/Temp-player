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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
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

    override fun onStart() {
        super.onStart()
        try {
            val serviceIntent = Intent(this, PlaybackService::class.java)
            startService(serviceIntent)
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
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
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(animationSpec = tween(PLAYER_TRANSITION_DURATION, easing = PlayerEmphasizedDecelerateEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(PLAYER_TRANSITION_DURATION, easing = PlayerEmphasizedAccelerateEasing))
            }
        ) {
            composable(
                route = "library",
                enterTransition = {
                    if (initialState.destination.route == "settings") {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(280, easing = PlayerEmphasizedDecelerateEasing)
                        ) + fadeIn(animationSpec = tween(220))
                    } else {
                        fadeIn(animationSpec = tween(PLAYER_TRANSITION_DURATION, easing = PlayerEmphasizedDecelerateEasing))
                    }
                },
                exitTransition = {
                    if (targetState.destination.route == "settings") {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(280, easing = PlayerEmphasizedAccelerateEasing)
                        ) + fadeOut(animationSpec = tween(220))
                    } else {
                        fadeOut(animationSpec = tween(220))
                    }
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(280, easing = PlayerEmphasizedDecelerateEasing)
                    ) + fadeIn(animationSpec = tween(220))
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
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(220))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(220))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(220))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(220))
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
            onNavigateToQueue = {
                viewModel.openPlaybackQueue()
                if (navController.currentDestination?.route != "library") {
                    navController.popBackStack("library", inclusive = false)
                }
            },
            onNavigateToSettings = {
                navController.navigate("settings")
            }
        )
    }
}
