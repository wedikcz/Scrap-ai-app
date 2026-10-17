package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HackerCoreAnalysis
import com.example.ui.theme.DarkBase
import com.example.ui.theme.DarkCard
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HackerCoreBlock(
    analysis: HackerCoreAnalysis,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBase)
            .border(1.dp, NeonEmerald.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Hacker Core",
                    tint = NeonEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = " <HACKER-CORE PROTOCOL V4.0>",
                    color = NeonEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (analysis.isDemoFallback) {
                    StatusBadge(text = "LOCAL EDGE FALLBACK", color = NeonAmber)
                } else {
                    StatusBadge(text = "DUAL-PASS GEMINI", color = NeonCyan)
                }
                Spacer(modifier = Modifier.size(6.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                // 1. Multidisciplinary Analysis
                SectionHeader("1. MULTIDISCIPLINÁRNÍ ANALÝZA (HW, RF, SecOps, Firmware)")
                ProtocolItem("HARDWARE", analysis.hardwareAnalysis, NeonCyan)
                ProtocolItem("RF / EMI", analysis.rfAnalysis, NeonEmerald)
                ProtocolItem("SECOPS", analysis.secOpsAnalysis, NeonRose)
                ProtocolItem("FIRMWARE", analysis.firmwareAnalysis, NeonAmber)

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Self-Critique & Sebereflexi
                SectionHeader("2. SELF-CRITIQUE & SEBEREFLEXI (VERIFIKACE ZAPOJENÍ)")
                analysis.selfCritiqueReflections.forEach { reflection ->
                    Text(
                        text = "• $reflection",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. OSINT Dorking
                SectionHeader("3. OSINT DORKING (CHYBĚJÍCÍ DATASHEETY)")
                analysis.osintGoogleDorks.forEach { dork ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkCard)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = dork,
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. EDC & Repurpose Brainstorming
                SectionHeader("4. EDC & REPURPOSE (35-BODOVÁ MATICE)")
                analysis.topEdcMatches.forEach { match ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ $match",
                            color = NeonEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "</HACKER-CORE>",
                    color = NeonEmerald.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = NeonAmber,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
    )
}

@Composable
private fun ProtocolItem(label: String, content: String, labelColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "[$label] ",
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = content,
            color = TextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}
