/**
 * Material 3 тема и цветовые токены экранов.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val SurfaceBg = AtmosphereBrand.Mist
val CardBg = AtmosphereBrand.Paper
val PrimaryBlue = AtmosphereBrand.TealAccent
val TextPrimary = AtmosphereBrand.Ink
val TextSecondary = AtmosphereBrand.InkSoft
val ChipBg = AtmosphereBrand.ChipBg
val LessonGreen = AtmosphereBrand.PositiveDeep

private val LightColors = lightColorScheme(
    primary = AtmosphereBrand.TealAccent,
    onPrimary = Color.White,
    primaryContainer = AtmosphereBrand.PositiveSoft,
    onPrimaryContainer = AtmosphereBrand.Ink,
    surface = AtmosphereBrand.Mist,
    onSurface = AtmosphereBrand.Ink,
    onSurfaceVariant = AtmosphereBrand.InkSoft,
    surfaceContainerLow = AtmosphereBrand.Paper,
    surfaceContainer = AtmosphereBrand.Paper,
    surfaceContainerHigh = AtmosphereBrand.Paper,
    secondary = AtmosphereBrand.ChipBg,
    onSecondary = AtmosphereBrand.Ink,
    error = AtmosphereBrand.Negative,
)

@Composable
fun AtmosphereTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SurfaceBg,
            content = content,
        )
    }
}
