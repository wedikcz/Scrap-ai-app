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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBase
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ScrapMasterViewModel

@Composable
fun TerminalScreen(
    viewModel: ScrapMasterViewModel,
    modifier: Modifier = Modifier
) {
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickCommands = listOf(
        "sys-state",
        "invent",
        "scan TP4056",
        "pinout esp32",
        "matrix",
        "dork ESP32",
        "rag 18650",
        "help",
        "clear"
    )

    LaunchedEffect(terminalOutput.size) {
        if (terminalOutput.isNotEmpty()) {
            listState.animateScrollToItem(terminalOutput.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBg)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top Terminal Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBase)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = TerminalGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VT100 // aarch64 HACKER TERMINAL PROTOCOL",
                    color = TerminalGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = { viewModel.toggleTerminalMode() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBase),
                modifier = Modifier.testTag("switch_to_gui_btn")
            ) {
                Text("GUI MÓD", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // Terminal Log Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(terminalOutput) { line ->
                val lineColor = when {
                    line.startsWith("root@") -> NeonCyan
                    line.startsWith("===") -> TextMuted
                    line.startsWith("[SYS-STATE") || line.startsWith("[DATA-DUMP") -> NeonEmerald
                    line.startsWith("<HACKER-CORE>") || line.startsWith("</HACKER-CORE>") -> NeonAmber
                    line.contains("ERROR") || line.contains("WARN") -> NeonRose
                    else -> TerminalGreen
                }

                Text(
                    text = line,
                    color = lineColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        // Quick Command Pills
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBase)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickCommands) { cmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurface)
                        .border(1.dp, NeonEmerald.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable { viewModel.executeTerminalCommand(cmd) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cmd,
                        color = TerminalGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Command Input Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBase)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "# ",
                color = TerminalGreen,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = inputCommand,
                onValueChange = { inputCommand = it },
                placeholder = { Text("Zadejte příkaz (např. 'sys-state', 'scan TP4056')...", color = TextMuted, fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_input_field"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = TerminalGreen,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TerminalGreen,
                    unfocusedTextColor = TerminalGreen
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputCommand.isNotBlank()) {
                        viewModel.executeTerminalCommand(inputCommand)
                        inputCommand = ""
                    }
                })
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    if (inputCommand.isNotBlank()) {
                        viewModel.executeTerminalCommand(inputCommand)
                        inputCommand = ""
                    }
                },
                modifier = Modifier.testTag("terminal_send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Spustit", tint = TerminalGreen)
            }
        }
    }
}
