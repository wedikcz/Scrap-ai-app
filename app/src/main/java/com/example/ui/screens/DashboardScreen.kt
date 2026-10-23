package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import java.util.Locale
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NetworkMode
import com.example.data.model.SalvageItem
import com.example.ui.components.NetworkModeBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DarkBase
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ScrapMasterViewModel

@Composable
fun DashboardScreen(
    viewModel: ScrapMasterViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val netMode by viewModel.networkMode.collectAsState()
    val gatewayTelemetry by viewModel.gatewayTelemetry.collectAsState()

    val totalUnits = items.sumOf { it.quantity }
    val totalSkus = items.size
    val totalValue = items.sumOf { it.quantity * it.estimatedValueUsd }
    val lowStockCount = items.count { it.quantity <= it.minQuantityAlert }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBase)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Telemetry Banner [SYS-STATE]
            SystemTelemetryCard(
                netMode = netMode,
                onToggleNetMode = {
                    val next = when (netMode) {
                        NetworkMode.AUTO_HYBRID -> NetworkMode.CLOUD_ONLY
                        NetworkMode.CLOUD_ONLY -> NetworkMode.LOCAL_AIRGAPPED
                        NetworkMode.LOCAL_AIRGAPPED -> NetworkMode.AUTO_HYBRID
                    }
                    viewModel.setNetworkMode(next)
                },
                onOpenTerminal = { viewModel.toggleTerminalMode() }
            )
        }

        item {
            // LiteLLM Gateway All-in-One Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setRoute("/gateway") }
                    .testTag("litellm_gateway_dashboard_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LITELLM GATEWAY V5.0",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusBadge(text = gatewayTelemetry.activeProvider.badge, color = NeonEmerald)
                            }
                            Text(
                                text = "Tokeny: ${gatewayTelemetry.totalTokens} | Latence: ${gatewayTelemetry.lastLatencyMs}ms | Self-Healing: OK",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = "Detail →",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "SALVAGED STOCK",
                    value = "$totalUnits ks",
                    subtitle = "$totalSkus unikátních SKU",
                    color = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "ODHADOVANÁ HODNOTA",
                    value = "$${String.format(Locale.US, "%.1f", totalValue)}",
                    subtitle = "Tržní hodnota z e-waste",
                    color = NeonEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "PROJEKTY V DÍLNĚ",
                    value = "${projects.size}",
                    subtitle = "${projects.count { it.status == "IN_PROGRESS" }} rozpracováno",
                    color = NeonAmber,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "NÍZKÝ STAV SKLADU",
                    value = "$lowStockCount",
                    subtitle = if (lowStockCount > 0) "Doplnit ze šrotu!" else "Zásoby stabilní",
                    color = if (lowStockCount > 0) NeonRose else NeonEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            // [AUTO-TRIAGE: ACTION MENU]
            AutoTriageActionMenu(
                onScanClick = { viewModel.setRoute("/scanner") },
                onAddInventoryClick = { viewModel.setRoute("/inventory") },
                onPinoutClick = { viewModel.setRoute("/pinouts") },
                onProjectsClick = { viewModel.setRoute("/projects") },
                onTermuxClick = { viewModel.setRoute("/termux") }
            )
        }

        item {
            // Section: Poslední součástky ve skladu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[DATA-DUMP: AKTUÁLNÍ SKLAD E-WASTE]",
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Zobrazit vše →",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { viewModel.setRoute("/inventory") }
                )
            }
        }

        items(items.take(4)) { item ->
            DashboardItemCard(
                item = item,
                onAdjustQty = { delta -> viewModel.adjustQuantity(item.id, delta) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Space for floating dock
        }
    }
}

@Composable
private fun SystemTelemetryCard(
    netMode: NetworkMode,
    onToggleNetMode: () -> Unit,
    onOpenTerminal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NeonEmerald)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SYS-STATE: ONLINE aarch64",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCard)
                            .clickable { onToggleNetMode() }
                            .padding(4.dp)
                    ) {
                        NetworkModeBadge(mode = netMode)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .clickable { onOpenTerminal() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "CLI",
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CLI",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Target Platform: Infinix HOT 50 Pro Plus / Android 15 (ARM64)\nKernel: 6.6-android15 | Fallback: Edge Neural + Indexed Room DB\nAnti-Halucinační Zámek: AKTIVNÍ (Status: Verified)",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun AutoTriageActionMenu(
    onScanClick: () -> Unit,
    onAddInventoryClick: () -> Unit,
    onPinoutClick: () -> Unit,
    onProjectsClick: () -> Unit,
    onTermuxClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "[AUTO-TRIAGE: ACTION MENU]",
                color = NeonAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionTile(
                    title = "AI Skener",
                    icon = Icons.Default.DocumentScanner,
                    color = NeonCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onScanClick
                )
                ActionTile(
                    title = "Pinouty",
                    icon = Icons.Default.Memory,
                    color = NeonEmerald,
                    modifier = Modifier.weight(1f),
                    onClick = onPinoutClick
                )
                ActionTile(
                    title = "35x Matrix",
                    icon = Icons.Default.Build,
                    color = NeonAmber,
                    modifier = Modifier.weight(1f),
                    onClick = onProjectsClick
                )
                ActionTile(
                    title = "Termux",
                    icon = Icons.Default.Terminal,
                    color = NeonRose,
                    modifier = Modifier.weight(1f),
                    onClick = onTermuxClick
                )
            }
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun DashboardItemCard(
    item: SalvageItem,
    onAdjustQty: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.category} • Zdroj: ${item.sourceDevice}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .clickable { onAdjustQty(-1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "-", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${item.quantity}",
                    color = NeonCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .clickable { onAdjustQty(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
