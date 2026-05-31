/**
 * Индикатор загрузки данных в стиле «Атмосфера»: круговой индикатор по центру.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AtmosphereDataLoadingBar(
    visible: Boolean,
    modifier: Modifier = Modifier,
    message: String = "Обновляем данные…",
    containerColor: Color = AtmosphereBrand.Mist.copy(alpha = 0.88f),
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(containerColor),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    color = AtmosphereBrand.SkyMid,
                    trackColor = AtmosphereBrand.SkyGlow.copy(alpha = 0.45f),
                    strokeWidth = 4.dp,
                )
                if (message.isNotEmpty()) {
                    Text(
                        message,
                        modifier = Modifier.padding(top = 16.dp, start = 32.dp, end = 32.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AtmosphereBrand.InkSoft,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.2.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun AtmosphereInitialLoading(
    message: String = "Загружаем сводку по школе…",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(44.dp),
            color = AtmosphereBrand.SkyMid,
            trackColor = AtmosphereBrand.SkyGlow.copy(alpha = 0.5f),
            strokeWidth = 4.dp,
        )
        if (message.isNotEmpty()) {
            Text(
                message,
                modifier = Modifier.padding(top = 16.dp),
                fontSize = 13.sp,
                color = AtmosphereBrand.InkMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
