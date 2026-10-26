package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GatewayTelemetry
import com.example.data.model.NetworkMode
import com.example.ui.components.FloatingGlassDock
import com.example.ui.components.SelfHealingErrorBoundary
import com.example.ui.screens.AiSettingsScreen
import com.example.ui.screens.AssistantRagScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GatewayTelemetryScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.PinoutScreen
import com.example.ui.screens.ProjectsKanbanScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.screens.TermuxMcpScreen
import com.example.ui.theme.DarkBase
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LightBase
import com.example.ui.theme.LightCard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ScrapMasterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScrapMasterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val isTerminalMode by viewModel.isTerminalMode.collectAsState()
            val currentRoute by viewModel.currentRoute.collectAsState()
            val netMode by viewModel.networkMode.collectAsState()
            val gatewayTelemetry by viewModel.gatewayTelemetry.collectAsState()
            val selfHealingToast by viewModel.selfHealingToast.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                // BackHandler to handle custom state stack
                BackHandler(enabled = isTerminalMode || currentRoute != "/") {
                    if (isTerminalMode) {
                        viewModel.toggleTerminalMode()
                    } else if (currentRoute != "/") {
                        viewModel.setRoute("/")
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isTerminalMode) {
                        TerminalScreen(viewModel = viewModel)
                    } else {
                        Scaffold(
                            topBar = {
                                TopCyberBar(
                                    isDarkTheme = isDarkTheme,
                                    netMode = netMode,
                                    gatewayTelemetry = gatewayTelemetry,
                                    onToggleDark = { viewModel.toggleDarkTheme() },
                                    onToggleNetMode = {
                                        val next = when (netMode) {
                                            NetworkMode.AUTO_HYBRID -> NetworkMode.CLOUD_ONLY
                                            NetworkMode.CLOUD_ONLY -> NetworkMode.LOCAL_AIRGAPPED
                                            NetworkMode.LOCAL_AIRGAPPED -> NetworkMode.AUTO_HYBRID
                                        }
                                        viewModel.setNetworkMode(next)
                                    },
                                    onOpenGateway = { viewModel.setRoute("/gateway") },
                                    onOpenAi = { viewModel.setRoute("/ai") },
                                    onToggleTerminal = { viewModel.toggleTerminalMode() }
                                )
                            },
                            bottomBar = {
                                FloatingGlassDock(
                                    currentRoute = currentRoute,
                                    onNavigate = { route -> viewModel.setRoute(route) }
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.background
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = innerPadding.calculateTopPadding())
                            ) {
                                SelfHealingErrorBoundary(
                                    onRetry = { viewModel.setRoute("/") }
                                ) {
                                    AnimatedContent(
                                        targetState = currentRoute,
                                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                                        label = "screen_transition"
                                    ) { route ->
                                        when (route) {
                                            "/" -> DashboardScreen(viewModel = viewModel)
                                            "/inventory" -> InventoryScreen(viewModel = viewModel)
                                            "/scanner" -> ScannerScreen(viewModel = viewModel)
                                            "/pinouts" -> PinoutScreen(viewModel = viewModel)
                                            "/projects" -> ProjectsKanbanScreen(viewModel = viewModel)
                                            "/assistant" -> AssistantRagScreen(viewModel = viewModel)
                                            "/termux" -> TermuxMcpScreen(viewModel = viewModel)
                                            "/gateway" -> GatewayTelemetryScreen(viewModel = viewModel)
                                            "/ai" -> AiSettingsScreen(viewModel = viewModel)
                                            else -> DashboardScreen(viewModel = viewModel)
                                        }
                                    }
                                }

                                // Self-Healing Floating Notification Toast
                                AnimatedVisibility(
                                    visible = selfHealingToast != null,
                                    enter = slideInVertically() + fadeIn(),
                                    exit = slideOutVertically() + fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DarkCard)
                                            .border(1.dp, NeonEmerald, RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    imageVector = Icons.Default.Shield,
                                                    contentDescription = null,
                                                    tint = NeonEmerald,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = selfHealingToast ?: "",
                                                    color = TextPrimary,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.clearSelfHealingToast() },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopCyberBar(
    isDarkTheme: Boolean,
    netMode: NetworkMode,
    gatewayTelemetry: GatewayTelemetry,
    onToggleDark: () -> Unit,
    onToggleNetMode: () -> Unit,
    onOpenGateway: () -> Unit,
    onOpenAi: () -> Unit,
    onToggleTerminal: () -> Unit
) {
    val containerBg = if (isDarkTheme) DarkSurface else LightCard
    val borderClr = if (isDarkTheme) DarkCardBorder else MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerBg)
            .statusBarsPadding()
            .border(width = 0.5.dp, color = borderClr)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Identity & Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onToggleTerminal() }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_scrapmaster_icon),
                    contentDescription = "ScrapMaster Logo",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ScrapMaster",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "LiteLLM Gateway • aarch64",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Top Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Full-AI configuration (key / provider / models)
                IconButton(
                    onClick = onOpenAi,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .testTag("ai_settings_top_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Nastavení AI",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // LiteLLM Gateway Telemetry Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .clickable { onOpenGateway() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("litellm_gateway_top_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = "Gateway Telemetry",
                            tint = NeonCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = gatewayTelemetry.activeProvider.badge,
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Network Mode Clicker
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onToggleNetMode() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("net_mode_toggle_btn")
                ) {
                    Text(
                        text = netMode.badge,
                        color = when (netMode) {
                            NetworkMode.AUTO_HYBRID -> NeonCyan
                            NetworkMode.CLOUD_ONLY -> NeonEmerald
                            NetworkMode.LOCAL_AIRGAPPED -> NeonAmber
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Dark/Light Theme Toggle
                IconButton(
                    onClick = onToggleDark,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("dark_mode_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkTheme) "Přepnout na světlý režim" else "Přepnout na tmavý režim",
                        tint = if (isDarkTheme) NeonAmber else NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Interactive Terminal CLI Toggle
                IconButton(
                    onClick = onToggleTerminal,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .testTag("terminal_cli_toggle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Hacker CLI Terminal",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
