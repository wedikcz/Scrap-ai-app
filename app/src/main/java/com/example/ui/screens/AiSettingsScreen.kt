package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiPreset
import com.example.data.ai.AiProvider
import com.example.data.ai.ModelCatalog
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

/**
 * Runtime AI configuration. This is what activates full AI in an installed APK
 * without rebuilding it: the key is entered here once and sealed in the Android
 * Keystore. There is no demo answer to fall back to, so this screen is also the
 * only place that can turn the scanner on.
 */
@Composable
fun AiSettingsScreen(
    viewModel: ScrapMasterViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.aiSettings.collectAsState()
    val isTesting by viewModel.isTestingAi.collectAsState()
    val probe by viewModel.aiProbeResult.collectAsState()
    val memory by viewModel.assistantMemory.collectAsState()
    val context = LocalContext.current

    var preset by remember(settings.preset) { mutableStateOf(settings.preset) }
    var baseUrl by remember(settings.preset, settings.baseUrl) { mutableStateOf(settings.baseUrl) }
    var visionModel by remember(settings.preset, settings.visionModel) { mutableStateOf(settings.visionModel) }
    var chatModel by remember(settings.preset, settings.chatModel) { mutableStateOf(settings.chatModel) }
    var codeModel by remember(settings.preset, settings.codeModel) { mutableStateOf(settings.codeModel) }
    var keyInput by remember(settings.preset) { mutableStateOf("") }

    // Never keep a typed secret in the text field after it has been sealed.
    LaunchedEffect(settings.apiKey) { keyInput = "" }

    val activePreset = preset
    val needsKey = activePreset != AiPreset.OLLAMA

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBase)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
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
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI ENGINE — PLNÉ AI",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        StatusBadge(
                            text = if (settings.isReady) "FULL AI" else "AI OFF",
                            color = if (settings.isReady) NeonEmerald else NeonAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = settings.statusLabel(),
                        color = if (settings.isReady) NeonEmerald else NeonAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Klíč se ukládá zašifrovaný (AES-256-GCM) do Android Keystore a odesílá se pouze vybranému poskytovateli. APK žádný klíč neobsahuje.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        item {
            Text(
                text = "POSKYTOVATEL",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AiPreset.entries.toList()) { option ->
                    FilterChip(
                        selected = preset == option,
                        onClick = {
                            preset = option
                            baseUrl = option.defaultBaseUrl
                            visionModel = option.defaultVisionModel
                            chatModel = option.defaultChatModel
                            codeModel = option.defaultCodeModel
                            keyInput = ""
                        },
                        label = {
                            Text(option.label, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan,
                            labelColor = TextSecondary,
                            containerColor = DarkSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = preset == option,
                            borderColor = DarkCardBorder,
                            selectedBorderColor = NeonCyan
                        )
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "API KLÍČ",
                            color = NeonAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (settings.hasKey) {
                        Text(
                            text = "Aktivní: ${settings.maskedKey()} · zdroj ${keySourceLabel(settings.keySource)}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = {
                            Text(
                                text = activePreset.keyHint,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_key_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (needsKey)
                            "Pole nech prázdné, pokud chceš zachovat už uložený klíč."
                        else
                            "Ollama běžící v telefonu (Termux) klíč nevyžaduje.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.saveAiSettings(
                                    preset = activePreset,
                                    baseUrl = baseUrl,
                                    visionModel = visionModel,
                                    chatModel = chatModel,
                                    codeModel = codeModel,
                                    newKey = keyInput.ifBlank { null }
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_save_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBase)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uložit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.testAiConnection() },
                            enabled = !isTesting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_test_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = NeonEmerald),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f))
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = NeonEmerald, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isTesting) "Testuji…" else "Otestovat", fontSize = 12.sp)
                        }
                    }

                    if (settings.hasKey) {
                        TextButton(
                            onClick = { viewModel.clearAiKey() },
                            modifier = Modifier.testTag("ai_clear_key_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = NeonRose, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Odstranit klíč tohoto poskytovatele", color = NeonRose, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        probe?.let { result ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (result.ok) NeonEmerald else NeonRose
                    )
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (result.ok) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (result.ok) NeonEmerald else NeonRose,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (result.ok) "SPOJENÍ FUNGUJE" else "TEST SELHAL",
                                color = if (result.ok) NeonEmerald else NeonRose,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(result.message, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                            if (result.ok) {
                                Text(
                                    text = "Latence ${result.latencyMs} ms · ${result.model}",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = if (activePreset.provider == AiProvider.GEMINI) "MODELY GEMINI" else "MODELY / ENDPOINT",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (activePreset.provider == AiProvider.OPENAI_COMPATIBLE) {
                ConfigField(
                    label = "Base URL (OpenAI-compatible)",
                    value = baseUrl,
                    placeholder = activePreset.defaultBaseUrl.ifBlank { "https://…/v1" },
                    onChange = { baseUrl = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            ConfigField(
                label = "Vision model (foto / PCB)",
                value = visionModel,
                placeholder = activePreset.defaultVisionModel,
                onChange = { visionModel = it },
                suggestions = ModelCatalog.suggestionsFor(activePreset),
                onSuggestion = { visionModel = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            ConfigField(
                label = "Chat model (asistent / RAG)",
                value = chatModel,
                placeholder = activePreset.defaultChatModel,
                onChange = { chatModel = it },
                suggestions = ModelCatalog.suggestionsFor(activePreset),
                onSuggestion = { chatModel = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            ConfigField(
                label = "Code model (firmware / skripty)",
                value = codeModel,
                placeholder = activePreset.defaultCodeModel,
                onChange = { codeModel = it },
                suggestions = ModelCatalog.suggestionsFor(activePreset),
                onSuggestion = { codeModel = it }
            )
        }

        item {
            val link = providerLink(activePreset)
            if (link != null) {
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.second)))
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(link.first, color = NeonCyan, fontSize = 11.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HERMES PAMĚŤ ASISTENTA",
                            color = NeonEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${memory.size} uložených zpráv relace. Kontext se do promptu posílá v klouzavém okně (max 10 zpráv / 4000 znaků).",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adaptace Panus/Hermes: trvalá paměť, klouzavé okno a modelový self-repair JSON kontraktu. Spouštění kódu z modelu je záměrně vypnuté – na telefonu by to bylo RCE.",
                        color = NeonEmerald,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.clearAssistantMemory() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonRose.copy(alpha = 0.15f),
                            contentColor = NeonRose
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("ai_clear_memory_button")
                    ) {
                        Text("Vymazat paměť", fontSize = 11.sp)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

@Composable
private fun ConfigField(
    label: String,
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    suggestions: List<String> = emptyList(),
    onSuggestion: (String) -> Unit = {}
) {
    Column {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = TextMuted, fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = DarkCardBorder
            )
        )
        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(suggestions) { suggestion ->
                    Text(
                        text = suggestion,
                        color = NeonCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .testTag("ai_model_suggestion")
                            .clickable { onSuggestion(suggestion) }
                    )
                }
            }
        }
    }
}

private fun keySourceLabel(source: com.example.data.ai.AiKeySource): String = when (source) {
    com.example.data.ai.AiKeySource.APP_STORAGE -> "v aplikaci"
    com.example.data.ai.AiKeySource.BUILD_CONFIG -> "build (.env)"
    com.example.data.ai.AiKeySource.NONE -> "—"
}

private fun providerLink(preset: AiPreset): Pair<String, String>? = when (preset) {
    AiPreset.GEMINI -> "Získat Gemini klíč" to "https://aistudio.google.com/apikey"
    AiPreset.OPENROUTER -> "Získat OpenRouter klíč" to "https://openrouter.ai/keys"
    AiPreset.OLLAMA -> "Nainstalovat Ollamu" to "https://ollama.com/download"
    AiPreset.CUSTOM -> null
}
