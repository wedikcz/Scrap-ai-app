package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.PinoutImportReport
import com.example.data.repository.PinoutJsonImporter
import com.example.ui.theme.DarkBase
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bulk import of component pinouts from the documented JSON schema.
 * The user can paste the schema, load it from a file, or start from the sample.
 * The dialog renders the importer report so rejected rows are never silent.
 */
@Composable
fun PinoutImportDialog(
    report: PinoutImportReport?,
    isImporting: Boolean,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var jsonText by remember { mutableStateOf("") }
    var fileNotice by remember { mutableStateOf<String?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }

    val openFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (loaded.isNullOrBlank()) {
                fileError = "Soubor se nepodařilo přečíst (nebo je prázdný)."
                fileNotice = null
            } else {
                jsonText = loaded
                fileError = null
                fileNotice = "Načteno ${loaded.length} znaků ze souboru."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "IMPORT PINOUTŮ Z JSON",
                color = NeonCyan,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Schéma schemaVersion ${PinoutJsonImporter.SCHEMA_VERSION}: objekt 'components' s poli " +
                        "name, chipCode, packageType, pinCount a 'pins'. Každá komponenta se validuje zvlášť – " +
                        "neplatné řádky se odmítnou s důvodem, platné se uloží do lokální DB.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            openFile.launch(arrayOf("application/json", "text/plain", "text/*", "*/*"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Načíst soubor", color = NeonCyan, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            jsonText = PinoutJsonImporter.SAMPLE_JSON
                            fileError = null
                            fileNotice = "Vloženo vzorové schéma."
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Vložit vzor", color = NeonEmerald, fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = {
                        jsonText = it
                        fileError = null
                    },
                    placeholder = { Text("{ \"schemaVersion\": 1, \"components\": [...] }", color = TextMuted, fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .testTag("pinout_json_input"),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextPrimary),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBase,
                        unfocusedContainerColor = DarkBase,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = NeonCyan.copy(alpha = 0.3f),
                        cursorColor = NeonCyan
                    )
                )

                fileNotice?.let {
                    Text(text = it, color = NeonEmerald, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                fileError?.let {
                    Text(text = it, color = NeonRose, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                if (isImporting) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Validuji a ukládám…", color = TextSecondary, fontSize = 11.sp)
                    }
                }

                report?.let { PinoutImportReportView(it) }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(jsonText) },
                enabled = !isImporting && jsonText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBase),
                modifier = Modifier.testTag("pinout_import_confirm_btn")
            ) {
                Text("Importovat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zavřít", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
private fun PinoutImportReportView(report: PinoutImportReport) {
    val acceptedColor = if (report.accepted.isNotEmpty()) NeonEmerald else NeonRose
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBase)
            .border(1.dp, acceptedColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = acceptedColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Přijato ${report.accepted.size} z ${report.declared}, odmítnuto ${report.rejectedCount}",
                color = acceptedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        report.accepted.forEach { item ->
            Text(
                text = "✓ ${item.name} · ${item.chipCode} · ${item.packageType} · ${item.antiHallucinationStatus}",
                color = NeonEmerald,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        report.errors.forEach { issue ->
            Text(
                text = "✗ ${issue.componentRef}: ${issue.message}",
                color = NeonRose,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        report.warnings.forEach { warning ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.Top) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${warning.componentRef}: ${warning.message}",
                    color = NeonAmber,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
    }
}
