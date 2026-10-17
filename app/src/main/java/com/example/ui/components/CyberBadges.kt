package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NetworkMode
import com.example.ui.theme.DarkCard
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextPrimary

@Composable
fun StatusBadge(
    text: String,
    color: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun AntiHallucinationLockBadge(
    status: String, // VERIFIED, PROBABLE, UNKNOWN
    modifier: Modifier = Modifier
) {
    val (color, label) = when (status.uppercase()) {
        "VERIFIED" -> Pair(NeonEmerald, "ZÁMEK: OVĚŘENO (VERIFIED)")
        "PROBABLE" -> Pair(NeonAmber, "ZÁMEK: PRAVDĚPODOBNÉ")
        else -> Pair(NeonRose, "ZÁMEK: NEZNÁMÉ (DORK GENERATED)")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkCard)
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "  $label",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun NetworkModeBadge(
    mode: NetworkMode,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (mode) {
        NetworkMode.AUTO_HYBRID -> Pair(NeonCyan, "HYBRID (AUTO)")
        NetworkMode.CLOUD_ONLY -> Pair(NeonEmerald, "CLOUD (GEMINI)")
        NetworkMode.LOCAL_AIRGAPPED -> Pair(NeonAmber, "AIR-GAPPED (OFFLINE)")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
