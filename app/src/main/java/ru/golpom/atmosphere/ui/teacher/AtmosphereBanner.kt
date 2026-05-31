/**
 * Анимированный погодный баннер по суммарному баллу класса.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.teacher

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class WeatherState {
    CLEAR, PARTLY_CLOUDY, FAIR, CLOUDY, RAINY, STORMY
}

fun weatherFromScore(score: Int): WeatherState = when {
    score > 50 -> WeatherState.CLEAR
    score > 20 -> WeatherState.PARTLY_CLOUDY
    score >= -5 -> WeatherState.FAIR
    score >= -15 -> WeatherState.CLOUDY
    score >= -29 -> WeatherState.RAINY
    else -> WeatherState.STORMY
}

private data class CloudData(
    val cx: Float, val cy: Float, val radius: Float, val speed: Float, val phase: Float,
)

private data class Raindrop(
    val x: Float, val y: Float, val length: Float, val speed: Float,
)

@Composable
fun AtmosphereBanner(totalScore: Int, modifier: Modifier = Modifier, bgColor: Color = Color(0xFFF5F6F8)) {
    val state = remember(totalScore) { weatherFromScore(totalScore) }
    val gradient = remember(state, bgColor) { buildSkyGradient(state, bgColor) }

    val clouds = remember(state) { generateClouds(state) }
    val raindrops = remember(state) {
        if (state == WeatherState.RAINY || state == WeatherState.STORMY)
            List(40) { Raindrop(Random.nextFloat(), Random.nextFloat(), 6f + Random.nextFloat() * 8f, 0.3f + Random.nextFloat() * 0.4f) }
        else emptyList()
    }

    val infinite = rememberInfiniteTransition(label = "weather")

    val cloudProgress by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDrift",
    )

    val rainOffset by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Restart),
        label = "rainFall",
    )

    val sunGlow by infinite.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "sunGlow",
    )
    val sunRotate by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "sunRotate",
    )
    val sunPulse by infinite.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "sunPulse",
    )

    var lightningAlpha by remember { mutableStateOf(0f) }

    if (state == WeatherState.STORMY) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(3000L + Random.nextLong(4000))
                lightningAlpha = 0.9f
                delay(80L)
                lightningAlpha = 0f
                delay(120L)
                lightningAlpha = 0.7f
                delay(60L)
                lightningAlpha = 0f
            }
        }
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val contentH = 0.75f * h

            drawRect(brush = gradient, size = size)

            if (state == WeatherState.CLEAR || state == WeatherState.PARTLY_CLOUDY) {
                drawSun(w * 0.15f, contentH * 0.3f, contentH * 0.12f, sunGlow, sunRotate, sunPulse)
            }

            val angle = cloudProgress * 2f * PI.toFloat()
            clouds.forEach { c ->
                val phase = c.phase * 2f * PI.toFloat()
                val drift = (sin(angle + phase) * 0.5f + 0.5f) * w * c.speed
                val x = ((c.cx * w + drift) % (w + 200f)) - 100f
                drawCloud(x, c.cy * contentH, c.radius * h.coerceAtMost(w) / 400f, state)
            }

            if (state == WeatherState.RAINY || state == WeatherState.STORMY) {
                val alpha = if (state == WeatherState.STORMY) 0.7f else 0.4f
                raindrops.forEach { drop ->
                    val y = ((drop.y * contentH + rainOffset * contentH * drop.speed) % (contentH * 1.2f)) - contentH * 0.1f
                    val rainColor = if (state == WeatherState.STORMY) Color(0xFFB0C4DE) else Color(0xFFD0E8FF)
                    drawLine(
                        color = rainColor.copy(alpha = alpha),
                        start = Offset(drop.x * w, y),
                        end = Offset(drop.x * w, y + drop.length.dp.toPx()),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            if (lightningAlpha > 0f && state == WeatherState.STORMY) {
                val boltX = (0.3f + Random.nextFloat() * 0.4f) * w
                drawLightning(boltX, 0f, contentH * 0.9f, lightningAlpha)
            }
        }
    }
}

private fun buildSkyGradient(state: WeatherState, bgColor: Color): Brush = when (state) {
    WeatherState.CLEAR -> Brush.verticalGradient(0.0f to Color(0xFF1A5276), 0.45f to Color(0xFF4A90D9), 0.7f to Color(0xFF87CEEB), 0.85f to bgColor, 1.0f to bgColor)
    WeatherState.PARTLY_CLOUDY -> Brush.verticalGradient(0.0f to Color(0xFF2C3E50), 0.45f to Color(0xFF6B9BD2), 0.7f to Color(0xFFB0C4DE), 0.85f to bgColor, 1.0f to bgColor)
    WeatherState.FAIR -> Brush.verticalGradient(0.0f to Color(0xFFB8A9C9), 0.35f to Color(0xFFF2C4A0), 0.6f to Color(0xFFFCE4D6), 0.85f to bgColor, 1.0f to bgColor)
    WeatherState.CLOUDY -> Brush.verticalGradient(0.0f to Color(0xFF4A5568), 0.45f to Color(0xFF8F9AA7), 0.7f to Color(0xFFBFC9D5), 0.85f to bgColor, 1.0f to bgColor)
    WeatherState.RAINY -> Brush.verticalGradient(0.0f to Color(0xFF374151), 0.45f to Color(0xFF6B7280), 0.7f to Color(0xFF9CA3AF), 0.85f to bgColor, 1.0f to bgColor)
    WeatherState.STORMY -> Brush.verticalGradient(0.0f to Color(0xFF1F2937), 0.45f to Color(0xFF374151), 0.7f to Color(0xFF4B5563), 0.85f to bgColor, 1.0f to bgColor)
}

private fun generateClouds(state: WeatherState): List<CloudData> {
    val rng = Random(42)
    val count = when (state) {
        WeatherState.CLEAR -> 0
        WeatherState.PARTLY_CLOUDY -> 2
        WeatherState.FAIR -> 1
        WeatherState.CLOUDY -> 3
        WeatherState.RAINY -> 4
        WeatherState.STORMY -> 5
    }
    return List(count) {
        CloudData(
            cx = rng.nextFloat() * 1.4f - 0.1f,
            cy = 0.25f + rng.nextFloat() * 0.25f,
            radius = 14f + rng.nextFloat() * 18f,
            speed = 0.12f + rng.nextFloat() * 0.18f,
            phase = rng.nextFloat(),
        )
    }
}

private data class CloudBump(val x: Float, val y: Float, val radius: Float)

private fun DrawScope.drawCloud(cx: Float, cy: Float, r: Float, state: WeatherState) {
    val baseColor = when (state) {
        WeatherState.CLEAR, WeatherState.PARTLY_CLOUDY, WeatherState.FAIR -> Color(0xFFFFFFFF)
        WeatherState.CLOUDY -> Color(0xFFE5E7EB)
        WeatherState.RAINY -> Color(0xFFB0BEC5)
        WeatherState.STORMY -> Color(0xFF78909C)
    }
    val isDark = state == WeatherState.RAINY || state == WeatherState.STORMY || state == WeatherState.CLOUDY
    val shadowColor = baseColor.copy(alpha = if (isDark) 0.5f else 0.3f)
    val strokeAlpha = if (isDark) 0.6f else 0.35f
    val depthFactor = if (isDark) 0.3f else 0.15f

    val bumps = listOf(
        CloudBump(0f, 0f, r * 0.7f),
        CloudBump(-r * 0.7f, -r * 0.08f, r * 0.6f),
        CloudBump(r * 0.7f, -r * 0.08f, r * 0.6f),
        CloudBump(-r * 1.2f, r * 0.05f, r * 0.5f),
        CloudBump(r * 1.2f, r * 0.05f, r * 0.5f),
        CloudBump(-r * 0.45f, r * 0.22f, r * 0.45f),
        CloudBump(r * 0.45f, r * 0.22f, r * 0.45f),
        CloudBump(-r * 0.3f, -r * 0.32f, r * 0.45f),
        CloudBump(r * 0.3f, -r * 0.32f, r * 0.45f),
        CloudBump(0f, -r * 0.28f, r * 0.5f),
    )

    bumps.forEach { (bumpX, bumpY, rad) ->
        drawCircle(shadowColor, rad, Offset(cx + bumpX + 3f, cy + bumpY + 3f))
    }
    bumps.forEach { (bumpX, bumpY, rad) ->
        val depth = (bumpY / r * depthFactor).coerceIn(-0.25f, 0.25f)
        val alpha = (0.82f + depth).coerceIn(0.55f, 1.0f)
        drawCircle(baseColor.copy(alpha = alpha), rad, Offset(cx + bumpX, cy + bumpY))
        drawCircle(
            color = baseColor.copy(alpha = strokeAlpha),
            radius = rad,
            center = Offset(cx + bumpX, cy + bumpY),
            style = Stroke(width = 1.2f),
        )
    }
}

private fun DrawScope.drawSun(x: Float, y: Float, r: Float, glowAlpha: Float, rotation: Float, pulse: Float) {
    val glow = Color(0xFFFFF176)
    val deg = (PI / 180).toFloat()
    drawCircle(glow.copy(alpha = 0.06f * glowAlpha), r * 2.5f * pulse, Offset(x, y))
    drawCircle(glow.copy(alpha = 0.12f * glowAlpha), r * 1.8f * pulse, Offset(x, y))
    drawCircle(Color(0xFFFFF176), r * 1.1f * pulse, Offset(x, y))
    drawCircle(Color(0xFFFFF9C4), r * pulse, Offset(x, y))
    for (i in 0 until 12) {
        val angle = i * (PI / 6).toFloat() + rotation * deg
        val inner = r * 1.3f * pulse
        val outer = r * 1.9f * pulse
        val rayAlpha = if (i % 2 == 0) 0.5f * glowAlpha else 0.3f * glowAlpha
        drawLine(
            color = Color(0xFFFFF176).copy(alpha = rayAlpha),
            start = Offset(x + cos(angle) * inner, y + sin(angle) * inner),
            end = Offset(x + cos(angle) * outer, y + sin(angle) * outer),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawLightning(x: Float, startY: Float, endY: Float, alpha: Float) {
    val color = Color.White.copy(alpha = alpha)
    val segments = 6
    val segH = (endY - startY) / segments
    val path = Path()
    path.moveTo(x, startY)
    var px = x
    for (i in 1..segments) {
        val jx = ((Random.nextFloat() - 0.5f) * 40f).coerceIn(-30f, 30f)
        path.lineTo(px + jx, startY + segH * i)
        px += jx
    }
    drawPath(path, color, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    val glow = color.copy(alpha = alpha * 0.2f)
    drawPath(path, glow, style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
