/**
 * Бренд «Атмосфера»: палитра неба, дисциплины и общие UI-элементы (шапки, бейджи, heatmap).
 * UI-слой (Compose); согласовано с макетом приложения.
 */
package ru.golpom.atmosphere.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

object AtmosphereBrand {
    val SkyDeep = Color(0xFF3D7A6A)
    val SkyMid = Color(0xFF5BA68A)
    val SkyLight = Color(0xFF8ECDB8)
    val SkyGlow = Color(0xFFB8E6D4)
    val Mist = Color(0xFFF2F6F4)
    val Cloud = Color(0xFFFFFFFF)
    val Paper = Color(0xFFFFFFFF)

    val Ink = Color(0xFF1E2D2A)
    val InkSoft = Color(0xFF4A5F5A)
    val InkMuted = Color(0xFF8A9E97)
    val OnSky = Color(0xFFF8FFFC)

    val Positive = Color(0xFF5CB87A)
    val PositiveSoft = Color(0xFFE3F5EA)
    val PositiveDeep = Color(0xFF3D9A62)
    val Negative = Color(0xFFE07A7A)
    val NegativeSoft = Color(0xFFFCEAEA)
    val NegativeDeep = Color(0xFFC95555)
    val Neutral = Color(0xFFB0BEC5)
    val NeutralSoft = Color(0xFFF0F3F2)
    val Grid = NeutralSoft

    val Rule = Color(0xFFE0EAE6)
    val TealAccent = Color(0xFF5BA68A)
    val ChipBg = Color(0xFFE8F0ED)

    val skyHeaderGradient: Brush
        get() = Brush.verticalGradient(
            0f to SkyDeep,
            0.35f to SkyMid,
            0.7f to SkyLight,
            1f to SkyGlow,
        )

    val mistCanvasGradient: Brush
        get() = Brush.verticalGradient(
            0f to Mist,
            1f to Color(0xFFE8F0EC),
        )
}

fun atmosphereScoreColor(score: Int): Color = when {
    score > 0 -> AtmosphereBrand.Positive
    score < 0 -> AtmosphereBrand.Negative
    else -> AtmosphereBrand.Neutral
}

fun atmosphereHeatmapColor(score: Int, maxAbs: Int): Color {
    if (score == 0) return AtmosphereBrand.NeutralSoft
    val t = (abs(score).toFloat() / maxOf(maxAbs, 1)).coerceIn(0.1f, 1f)
    return if (score > 0) {
        androidx.compose.ui.graphics.lerp(AtmosphereBrand.PositiveSoft, AtmosphereBrand.PositiveDeep, t)
    } else {
        androidx.compose.ui.graphics.lerp(AtmosphereBrand.NegativeSoft, AtmosphereBrand.NegativeDeep, t)
    }
}

fun atmosphereHeatmapTextColor(score: Int, maxAbs: Int): Color {
    if (score == 0) return AtmosphereBrand.InkMuted
    val t = abs(score).toFloat() / maxOf(maxAbs, 1)
    return if (t > 0.45f) Color.White else AtmosphereBrand.InkSoft
}

@Composable
fun AtmosphereScoreBadge(
    score: Int,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) = when {
        score > 0 -> AtmosphereBrand.Positive to Color.White
        score < 0 -> AtmosphereBrand.Negative to Color.White
        else -> AtmosphereBrand.Neutral to Color.White
    }
    val text = when {
        score > 0 -> "+$score"
        score < 0 -> score.toString()
        else -> "0"
    }
    val badgeSize = if (text.length <= 2) 32.dp else 36.dp
    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
fun AtmosphereHeatmapLegend(maxAbs: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Шкала", fontSize = 10.sp, color = AtmosphereBrand.InkMuted, fontWeight = FontWeight.Medium)
        Text(
            atmosphereFormatSigned(-maxAbs),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AtmosphereBrand.Negative,
        )
        Row(
            Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
        ) {
            repeat(24) { i ->
                val fakeScore = ((i - 12) * maxAbs / 12f).toInt()
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(atmosphereHeatmapColor(fakeScore, maxAbs)),
                )
            }
        }
        Text(
            atmosphereFormatSigned(maxAbs),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AtmosphereBrand.Positive,
        )
    }
}

fun atmosphereFormatSigned(v: Int): String = if (v >= 0) "+$v" else "$v"
