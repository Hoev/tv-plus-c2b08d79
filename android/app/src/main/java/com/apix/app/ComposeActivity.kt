package com.apix.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apix.app.data.*
import com.apix.app.ui.screens.*
import com.apix.app.ui.theme.APiXTheme
import com.apix.app.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.gson.Gson

class ComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init Firebase
        if (FirebaseApp.getApps(this).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyBxT35NMrvWYPJRvWek_NKeu8QtNInISC4")
                .setApplicationId("1:659730944639:web:1c00b6f7118bf85bdde54a")
                .setDatabaseUrl("https://cinema-plus-d1238-default-rtdb.firebaseio.com")
                .setProjectId("cinema-plus-d1238")
                .setStorageBucket("cinema-plus-d1238.firebasestorage.app")
                .build()
            FirebaseApp.initializeApp(this, options)
        }

        // Show system bars normally
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            var isDarkMode by remember { mutableStateOf(true) }
            var isInPlayer by remember { mutableStateOf(false) }

            // Control immersive mode and orientation based on player state
            LaunchedEffect(isInPlayer) {
                if (isInPlayer) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    WindowInsetsControllerCompat(window, window.decorView).let { ctrl ->
                        ctrl.hide(WindowInsetsCompat.Type.systemBars())
                        ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            APiXTheme(darkTheme = isDarkMode) {
                AppNavigation(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = it },
                    onPlayerStateChanged = { isInPlayer = it }
                )
            }
        }
    }
}

sealed class Screen {
    data object Main : Screen()
    data class SubChannels(val menuName: String, val channels: List<Channel>) : Screen()
    data object Search : Screen()
    data class Player(val config: PlayerConfig) : Screen()
}

@Composable
fun AppNavigation(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onPlayerStateChanged: (Boolean) -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val sideMenus by viewModel.sideMenus.collectAsState()

    // Navigation stack for proper back behavior
    val navigationStack = remember { mutableStateListOf<Screen>() }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
    var isSettings by remember { mutableStateOf(false) }

    // Notify activity about player state
    LaunchedEffect(currentScreen) {
        onPlayerStateChanged(currentScreen is Screen.Player)
    }

    val channels = remember(uiState.selectedCategory) {
        viewModel.getVisibleChannels()
    }

    fun navigateTo(screen: Screen) {
        navigationStack.add(currentScreen)
        currentScreen = screen
    }

    fun goBack(): Boolean {
        if (navigationStack.isNotEmpty()) {
            currentScreen = navigationStack.removeAt(navigationStack.lastIndex)
            return true
        }
        return false
    }

    fun handleChannelClick(channel: Channel) {
        when (channel.actionType) {
            "open_submenu" -> {
                val menuId = channel.sideMenuId ?: return
                val menu = sideMenus[menuId] ?: return
                val subChannels = menu.channels?.values
                    ?.filter { !it.hidden }
                    ?.sortedBy { it.sortOrder }
                    ?.map { sc ->
                        Channel(
                            id = sc.id,
                            name = sc.name,
                            imageUrl = sc.imageUrl,
                            sortOrder = sc.sortOrder,
                            actionType = "direct_play",
                            stream = sc.stream,
                            androidStream = sc.androidStream,
                            androidActionType = sc.androidActionType
                        )
                    } ?: emptyList()
                navigateTo(Screen.SubChannels(channel.name, subChannels))
            }
            "external_link" -> {
                // handled by intent
            }
            else -> {
                val config = viewModel.buildPlayerConfig(channel) ?: return
                navigateTo(Screen.Player(config))
            }
        }
    }

    fun handleCategorySelect(cat: Category) {
        val lower = cat.name.lowercase()
        if (lower.contains("setting") || lower.contains("إعدادات")) {
            isSettings = true
        } else {
            isSettings = false
            viewModel.selectCategory(cat)
        }
    }

    // Back handling
    androidx.activity.compose.BackHandler(currentScreen !is Screen.Main || isSettings) {
        if (isSettings) {
            isSettings = false
        } else {
            goBack()
        }
    }

    when (val screen = currentScreen) {
        is Screen.Main -> {
            if (isSettings) {
                SettingsScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )
            } else {
                MainScreen(
                    uiState = uiState,
                    onCategorySelected = ::handleCategorySelect,
                    onChannelClick = ::handleChannelClick,
                    onSearchClick = { navigateTo(Screen.Search) },
                    channels = channels
                )
            }
        }
        is Screen.SubChannels -> {
            SubChannelScreen(
                menuName = screen.menuName,
                channels = screen.channels,
                onChannelClick = ::handleChannelClick,
                onBack = { goBack() }
            )
        }
        is Screen.Search -> {
            SearchScreen(
                onSearch = { viewModel.searchChannels(it) },
                onChannelClick = { ch ->
                    val config = viewModel.buildPlayerConfig(ch)
                    if (config != null) {
                        navigateTo(Screen.Player(config))
                    }
                },
                onClose = { goBack() }
            )
        }
        is Screen.Player -> {
            PlayerScreen(
                config = screen.config,
                onBack = { goBack() }
            )
        }
    }
}
