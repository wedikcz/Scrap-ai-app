package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.ui.components.CameraScannerDialog
import com.example.util.CameraPermission
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NetworkMode
import com.example.ui.components.HackerCoreBlock
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
import java.io.ByteArrayOutputStream

@Composable
fun ScannerScreen(
    viewModel: ScrapMasterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsState()
    val analysis by viewModel.currentAnalysis.collectAsState()
    val scanError by viewModel.scanError.collectAsState()
    val netMode by viewModel.networkMode.collectAsState()
    val aiSettings by viewModel.aiSettings.collectAsState()
    val canScan = aiSettings.isReady && netMode != NetworkMode.LOCAL_AIRGAPPED

    var manualInput by remember { mutableStateOf("") }
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }
    var imageFileName by remember { mutableStateOf<String?>(null) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var cameraPermissionDeniedNotice by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = CameraPermission.rememberCameraPermissionLauncher { isGranted ->
        if (isGranted) {
            cameraPermissionDeniedNotice = false
            showCameraDialog = true
        } else {
            cameraPermissionDeniedNotice = true
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                val bytes = outputStream.toByteArray()
                selectedImageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                imageFileName = "Foto součástky (${bytes.size / 1024} KB)"
            } catch (e: Exception) {
                // handle image load error gracefully
            }
        }
    }

    val chipPresets = listOf(
        "TP4056 24E4",
        "ESP32-WROOM-32D",
        "18650 3.7V Cell",
        "NE555 Timer",
        "CH340G USB",
        "SSD1306 OLED",
        "MP1584 Buck",
        "nRF24L01+"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBase)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Scanner Header Card
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
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "Scanner",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MULTIMODAL DUAL-PASS AI SKENER",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        NetworkModeBadge(mode = netMode)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dvoufázová analýza součástek: Fáze 1 Identifikace čipu/pouzdra → Fáze 2 Verifikace zapojení, Anti-halucinační filtr & párování s 35-bodovou repurpose maticí.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(
                            text = if (canScan) "FULL AI" else "AI OFF",
                            color = if (canScan) NeonEmerald else NeonAmber
                        )
                        TextButton(
                            onClick = { viewModel.setRoute("/ai") },
                            modifier = Modifier.testTag("scanner_ai_settings_btn")
                        ) {
                            Text(
                                text = if (aiSettings.isReady) "AI: ${aiSettings.effectiveVisionModel}" else "Aktivovat plné AI",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        if (!canScan) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("scanner_ai_gate"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonAmber.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SKENER ČEKÁ NA PLNÉ AI",
                                color = NeonAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (netMode == NetworkMode.LOCAL_AIRGAPPED) {
                                "Režim LOCAL_ONLY je určený pro offline RAG asistenta. Skenování součástek vyžaduje cloudový model – přepni režim sítě nebo se vrať k asistentovi."
                            } else {
                                "Bez API klíče se analýza nespustí. Aplikace záměrně nevrací náhradní demo výsledky – všechny údaje musí pocházet z reálného modelu."
                            },
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.setRoute("/ai") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = DarkBase),
                            modifier = Modifier.testTag("scanner_activate_ai_btn")
                        ) {
                            Text("AKTIVOVAT PLNÉ AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        item {
            // Input & Capture Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "TEXTOVÝ KÓD NEBO FOTOGRAFIE SOUČÁSTKY",
                        color = NeonEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        placeholder = { Text("Zadejte potisk čipu (např. TP4056, ESP32, AMS1117...)", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scanner_input_field"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // CameraX live scanner and image picker controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (CameraPermission.hasCameraPermission(context)) {
                                    showCameraDialog = true
                                } else {
                                    cameraPermissionLauncher.launch(CameraPermission.PERMISSION)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("camera_scan_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "CameraX Live Sken",
                                tint = NeonEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kamera Live",
                                color = NeonEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Vybrat foto z galerie",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (imageFileName != null) "Foto vybráno" else "Z galerie",
                                color = NeonCyan,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val query = manualInput.trim()
                            if (query.isNotBlank() || selectedImageBase64 != null) {
                                viewModel.scanComponent(query, selectedImageBase64)
                            }
                        },
                        enabled = !isScanning && canScan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_scan_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBase)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DarkBase, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Skenuji (Dual-Pass AI)...", color = DarkBase, fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Spustit AI Analýzu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AnimatedVisibility(visible = cameraPermissionDeniedNotice) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Oprávnění kamery bylo zamítnuto. Povolte přístup ke kameře pro živé skenování.",
                                    color = NeonAmber,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { cameraPermissionLauncher.launch(CameraPermission.PERMISSION) },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Povolit", fontSize = 10.sp, color = NeonAmber)
                                }
                            }
                        }
                    }

                    if (imageFileName != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = imageFileName ?: "", color = NeonEmerald, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        item {
            // Chip Presets for fast test
            Column {
                Text(
                    text = "RYCHLÉ PŘEDVOLBY E-WASTE:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(chipPresets) { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                .clickable(enabled = canScan) {
                                    manualInput = preset
                                    viewModel.scanComponent(preset, null)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                color = if (canScan) NeonCyan else TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        if (scanError != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonRose.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonRose)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = NeonRose)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = scanError ?: "", color = NeonRose, fontSize = 12.sp)
                    }
                }
            }
        }

        // Analysis Result Card
        if (analysis != null) {
            val itemAnalysis = analysis!!
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = itemAnalysis.componentName,
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Pouzdro: ${itemAnalysis.packageType}",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                StatusBadge(text = itemAnalysis.gatewayProviderUsed.badge, color = NeonCyan)
                                Spacer(modifier = Modifier.height(4.dp))
                                StatusBadge(text = "${itemAnalysis.latencyMs}ms", color = NeonEmerald)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Odhadovaný zdroj: ${itemAnalysis.estimatedOrigin}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Testování: ${itemAnalysis.conditionAssessment}",
                            color = NeonAmber,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Full Hacker Core Protocol Inspection
                        HackerCoreBlock(analysis = itemAnalysis)

                        Spacer(modifier = Modifier.height(14.dp))

                        // 1-Click Action to Add to Inventory
                        Button(
                            onClick = { viewModel.addCurrentAnalysisToInventory() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_scanned_to_inventory_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = DarkBase)
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PŘIDAT DO SKLADU E-WASTE (1-KLIK)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    if (showCameraDialog) {
        CameraScannerDialog(
            onDismiss = { showCameraDialog = false },
            onImageCaptured = { base64, fileName ->
                selectedImageBase64 = base64
                imageFileName = fileName
                showCameraDialog = false
                val query = manualInput.ifBlank { "SMD PCB Component" }
                viewModel.analyzeComponent(base64, query)
            }
        )
    }
}
