package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PinoutItem
import com.example.ui.components.AntiHallucinationLockBadge
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
fun PinoutScreen(
    viewModel: ScrapMasterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val pinouts by viewModel.pinouts.collectAsState()
    val searchQuery by viewModel.pinoutSearch.collectAsState()

    val importReport by viewModel.pinoutImportReport.collectAsState()
    val isImporting by viewModel.isImportingPinouts.collectAsState()

    var selectedPinoutId by remember { mutableStateOf<String?>(null) }
    var showEditUrlDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    val activeItem = pinouts.find { it.id == selectedPinoutId } ?: pinouts.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBase)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PINOUT KATALOG",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${pinouts.size} čipů v lokální DB · JSON schéma import",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Button(
                    onClick = {
                        viewModel.clearPinoutImportReport()
                        showImportDialog = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = NeonCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("open_pinout_import_btn")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import JSON", fontSize = 11.sp)
                }
            }
        }

        item {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.pinoutSearch.value = it },
                placeholder = { Text("Hledat čip (ESP32, TP4056, NE555, CH340...)", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pinout_search_field"),
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
        }

        item {
            // Chip Selector Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pinouts) { item ->
                    val isSelected = (item.id == activeItem?.id)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurface)
                            .border(
                                1.dp,
                                if (isSelected) NeonCyan else DarkCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedPinoutId = item.id }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = item.name,
                            color = if (isSelected) NeonCyan else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        if (activeItem != null) {
            item {
                PinoutDetailCard(
                    pinout = activeItem,
                    onOpenUrl = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // handle invalid URL
                        }
                    },
                    onEditUrlClick = { showEditUrlDialog = true },
                    onCopyDork = { dork ->
                        clipboardManager.setText(AnnotatedString(dork))
                        copiedNotice = "OSINT Dork zkopírován!"
                    }
                )
            }
        }

        if (copiedNotice != null) {
            item {
                Text(
                    text = copiedNotice ?: "",
                    color = NeonEmerald,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    if (showImportDialog) {
        PinoutImportDialog(
            report = importReport,
            isImporting = isImporting,
            onDismiss = { showImportDialog = false },
            onImport = { json -> viewModel.importPinoutsSchema(json) }
        )
    }

    if (showEditUrlDialog && activeItem != null) {
        EditDatasheetUrlDialog(
            currentUrl = activeItem.datasheetUrl,
            suggestedUrl = activeItem.suggestedDatasheetUrl,
            chipName = activeItem.name,
            onDismiss = { showEditUrlDialog = false },
            onSave = { newUrl ->
                viewModel.updatePinoutDatasheet(activeItem.id, newUrl)
                showEditUrlDialog = false
            }
        )
    }
}

@Composable
private fun PinoutDetailCard(
    pinout: PinoutItem,
    onOpenUrl: (String) -> Unit,
    onEditUrlClick: () -> Unit,
    onCopyDork: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pinout.name,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Kód: ${pinout.chipCode} | Pouzdro: ${pinout.packageType}",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                StatusBadge(text = pinout.voltageRange, color = NeonAmber)
            }

            Spacer(modifier = Modifier.height(8.dp))
            AntiHallucinationLockBadge(status = pinout.antiHallucinationStatus)

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = pinout.description,
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ASCII Pin Diagram
            Text(
                text = "ASCII SCHÉMA ZAPOJENÍ PINŮ:",
                color = NeonEmerald,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBase)
                    .border(1.dp, NeonEmerald.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = pinout.pinDiagram,
                    color = NeonEmerald,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pin functions list
            Text(
                text = "FUNKCE VÝVODŮ:",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pinout.pinDetailsJson,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Hacker Tip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonAmber.copy(alpha = 0.1f))
                    .border(1.dp, NeonAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "⚡ HACKER SALVAGE TIP:",
                        color = NeonAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pinout.hackerTip,
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // DATASHEET URL INTEGRATION & SUGGESTIONS
            // ==========================================
            Text(
                text = "EXTERNÍ DATASHEET & OSINT KATALOG:",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (pinout.datasheetUrl.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Asociovaný datasheet:",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = pinout.datasheetUrl,
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { onOpenUrl(pinout.datasheetUrl) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBase),
                                modifier = Modifier.testTag("open_datasheet_url_btn")
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Otevřít", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "K tomuto čipu zatím není přiřazen žádný přímý odkaz.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEditUrlClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (pinout.datasheetUrl.isNotBlank()) "Změnit URL" else "+ Přiřadit URL",
                                color = NeonCyan,
                                fontSize = 11.sp
                            )
                        }

                        if (pinout.suggestedDatasheetUrl.isNotBlank() && pinout.datasheetUrl != pinout.suggestedDatasheetUrl) {
                            OutlinedButton(
                                onClick = { onOpenUrl(pinout.suggestedDatasheetUrl) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Návrh výrobce", color = NeonEmerald, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // OSINT Google Dork & Web Search
                    Text(
                        text = "OSINT Dork dotaz (pro stažení originálního PDF):",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkBase)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pinout.googleDork,
                            color = NeonAmber,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Dork",
                            tint = NeonAmber,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onCopyDork(pinout.googleDork) }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    // One-click search on web
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val searchUrl = "https://www.google.com/search?q=" + Uri.encode(pinout.googleDork)
                                onOpenUrl(searchUrl)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Text("Google OSINT", color = TextSecondary, fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val searchUrl = "https://www.alldatasheet.com/view.jsp?Searchword=" + Uri.encode(pinout.chipCode)
                                onOpenUrl(searchUrl)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Text("AllDatasheet", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditDatasheetUrlDialog(
    currentUrl: String,
    suggestedUrl: String,
    chipName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var urlText by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "DATASHEET LINK: $chipName",
                color = NeonCyan,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Vložte přímou URL adresu k PDF technickému listu součástky. Odkaz bude uložen v lokální offline databázi.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    placeholder = { Text("https://...", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (suggestedUrl.isNotBlank() && urlText != suggestedUrl) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBase)
                            .border(1.dp, NeonEmerald.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { urlText = suggestedUrl }
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "💡 Použít navržený datasheet výrobce:",
                                color = NeonEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = suggestedUrl,
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(urlText.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBase)
            ) {
                Text("Uložit odkaz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
