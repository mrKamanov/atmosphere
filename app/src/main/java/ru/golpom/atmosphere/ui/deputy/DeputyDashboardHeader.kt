/**
 * Шапка экрана завуча: бренд, роль и выбор периода в одном небесном блоке (без дублирующих подписей).
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.deputy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand

@Composable
fun DeputyDashboardHeader(
    topPadding: Dp,
    topEndInset: Dp,
    periodConfig: PeriodConfig,
    onPeriodSelect: (PeriodType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val customRangeHint = if (periodConfig.type == PeriodType.CUSTOM) {
        DeputyPeriodRange.label(periodConfig)
    } else {
        null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(topPadding + 188.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(brush = AtmosphereBrand.skyHeaderGradient)
            val w = size.width
            val h = size.height
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = h * 0.35f,
                center = Offset(w * 0.82f, h * 0.28f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = h * 0.22f,
                center = Offset(w * 0.72f, h * 0.32f),
            )
            val cloud = Path().apply {
                moveTo(w * 0.08f, h * 0.55f)
                cubicTo(w * 0.2f, h * 0.42f, w * 0.35f, h * 0.48f, w * 0.42f, h * 0.55f)
                cubicTo(w * 0.48f, h * 0.62f, w * 0.25f, h * 0.68f, w * 0.08f, h * 0.55f)
                close()
            }
            drawPath(cloud, Color.White.copy(alpha = 0.18f))
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                .padding(top = topPadding + 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                Modifier.padding(end = topEndInset),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Атмосфера",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = AtmosphereBrand.OnSky,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    "Режим завуча",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AtmosphereBrand.SkyDeep,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AtmosphereBrand.OnSky.copy(alpha = 0.92f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DeputyPeriodSelector(
                    currentType = periodConfig.type,
                    onSelect = onPeriodSelect,
                    style = DeputyPeriodSelectorStyle.OnSky,
                )
                if (customRangeHint != null) {
                    Text(
                        customRangeHint,
                        fontSize = 11.sp,
                        color = AtmosphereBrand.OnSky.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}
