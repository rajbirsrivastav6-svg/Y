package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.InteractionModality
import com.example.engine.VoiceController
import com.example.ui.components.CosmicBackground
import com.example.ui.components.OfflineBadgePill
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.VoiceOrbScreen
import com.example.ui.theme.LocalLumaColors
import kotlinx.coroutines.flow.collectLatest

const val ROUTE_HOME = "home"
const val ROUTE_CHAT = "chat"
const val ROUTE_VOICE = "voice"
const val ROUTE_VAULT = "vault"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navDestinations = listOf(
    NavDestinationItem(ROUTE_HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavDestinationItem(ROUTE_CHAT, "Chat", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
    NavDestinationItem(ROUTE_VOICE, "Voice", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
    NavDestinationItem(ROUTE_VAULT, "Vault", Icons.Filled.FolderSpecial, Icons.Outlined.FolderSpecial),
    NavDestinationItem(ROUTE_SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumaApp(viewModel: LumaViewModel) {
    val context = LocalContext.current
    val lumaColors = LocalLumaColors.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val vaultInteractions by viewModel.vaultInteractions.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()

    val voiceController = remember(context) {
        VoiceController(context = context) { recognizedText ->
            viewModel.sendMessage(recognizedText, modality = InteractionModality.VOICE)
        }
    }
    val voiceState by voiceController.voiceState.collectAsStateWithLifecycle()

    LaunchedEffect(preferences.speechRate) {
        voiceController.updateSpeechRate(preferences.speechRate)
    }

    LaunchedEffect(Unit) {
        viewModel.speakEvents.collectLatest { text ->
            voiceController.speak(text)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceController.release()
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val currentTitle = when (currentDestination?.route) {
        ROUTE_CHAT -> "Luma Chat"
        ROUTE_VOICE -> "Voice Orb"
        ROUTE_VAULT -> "Offline Vault"
        ROUTE_SETTINGS -> "Settings"
        else -> "Luma"
    }

    CosmicBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = lumaColors.glassSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.weight(0.2f))
                        navDestinations.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("nav_rail_${item.route}")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        CenterAlignedTopAppBar(
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = lumaColors.glassSurface
                            ),
                            navigationIcon = {
                                Box(modifier = Modifier.padding(start = 12.dp)) {
                                    OfflineBadgePill()
                                }
                            },
                            title = {
                                Text(
                                    text = currentTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.toggleDarkModeQuick(lumaColors.isDark) },
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .testTag("quick_theme_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (lumaColors.isDark) {
                                            Icons.Rounded.LightMode
                                        } else {
                                            Icons.Rounded.DarkMode
                                        },
                                        contentDescription = if (lumaColors.isDark) {
                                            "Switch to Light Mode"
                                        } else {
                                            "Switch to Dark Mode"
                                        },
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (!useNavigationRail) {
                            NavigationBar(
                                containerColor = lumaColors.glassSurface,
                                tonalElevation = 0.dp
                            ) {
                                navDestinations.forEach { item ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(item.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.testTag("nav_bar_${item.route}")
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                themeMode = preferences.themeMode,
                                timerState = timerState,
                                recentMessages = messages,
                                pendingTaskCount = tasks.count { !it.isCompleted },
                                noteCount = notes.size,
                                onOpenVoice = { navigateTopLevel(ROUTE_VOICE) },
                                onOpenChat = { navigateTopLevel(ROUTE_CHAT) },
                                onOpenVault = { navigateTopLevel(ROUTE_VAULT) },
                                onToggleTheme = { viewModel.toggleDarkModeQuick(lumaColors.isDark) },
                                onSendPrompt = { viewModel.sendMessage(it) },
                                onStartQuickTimer = { viewModel.startTimer(25 * 60, "Focus Session") },
                                onPauseResumeTimer = { viewModel.pauseResumeTimer() },
                                onCancelTimer = { viewModel.cancelTimer() }
                            )
                        }

                        composable(ROUTE_CHAT) {
                            ChatScreen(
                                messages = messages,
                                timerState = timerState,
                                onSendMessage = { viewModel.sendMessage(it) },
                                onSpeakText = { voiceController.speak(it) },
                                onOpenVoice = { navigateTopLevel(ROUTE_VOICE) },
                                onPauseResumeTimer = { viewModel.pauseResumeTimer() },
                                onCancelTimer = { viewModel.cancelTimer() }
                            )
                        }

                        composable(ROUTE_VOICE) {
                            VoiceOrbScreen(
                                voiceState = voiceState,
                                autoSpeak = preferences.autoSpeak,
                                latestAssistantMessage = messages.lastOrNull(),
                                onStartListening = { voiceController.startListening() },
                                onStopListening = { voiceController.stopListening() },
                                onStopSpeaking = { voiceController.stopSpeaking() },
                                onToggleAutoSpeak = { enabled ->
                                    viewModel.setAutoSpeak(enabled)
                                    if (!enabled) voiceController.stopSpeaking()
                                },
                                onSendVoiceFallbackCommand = { cmd ->
                                    viewModel.sendMessage(cmd, modality = InteractionModality.VOICE)
                                },
                                onOpenChat = { navigateTopLevel(ROUTE_CHAT) }
                            )
                        }

                        composable(ROUTE_VAULT) {
                            VaultScreen(
                                tasks = tasks,
                                notes = notes,
                                memories = memories,
                                interactions = vaultInteractions,
                                onAddTask = { title, priority -> viewModel.addTask(title, priority) },
                                onToggleTask = { viewModel.toggleTask(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                onAddNote = { title, content -> viewModel.addNote(title, content) },
                                onDeleteNote = { viewModel.deleteNote(it) },
                                onAddMemory = { key, ans -> viewModel.addMemory(key, ans) },
                                onDeleteMemory = { viewModel.deleteMemory(it) },
                                onToggleInteractionPin = { viewModel.toggleInteractionPin(it) },
                                onDeleteInteraction = { viewModel.deleteVaultInteraction(it) },
                                onSpeakText = { voiceController.speak(it) }
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                preferences = preferences,
                                messageCount = messages.size,
                                taskCount = tasks.size,
                                noteCount = notes.size,
                                memoryCount = memories.size,
                                onSelectThemeMode = { viewModel.setThemeMode(it) },
                                onToggleDynamicColor = { viewModel.setDynamicColor(it) },
                                onToggleAutoSpeak = { viewModel.setAutoSpeak(it) },
                                onChangeSpeechRate = { viewModel.setSpeechRate(it) },
                                onTestVoice = {
                                    voiceController.speak(
                                        "Hello! Luma is running completely offline in ${preferences.themeMode.label} mode."
                                    )
                                },
                                onClearChat = { viewModel.clearChat() }
                            )
                        }
                    }
                }
            }
        }
    }
}
