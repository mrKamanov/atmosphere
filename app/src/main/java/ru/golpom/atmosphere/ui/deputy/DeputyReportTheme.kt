/**
 * Карточки и секции отчёта завуча на базе бренда [AtmosphereBrand].
 * UI-слой (Compose); §6.2 ТЗ.
 */
package ru.golpom.atmosphere.ui.deputy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.atmosphereFormatSigned

fun deputyFormatSigned(v: Int) = atmosphereFormatSigned(v)

enum class DeputyPeriodSelectorStyle {
    Card,
    OnSky,
}

@Composable
fun DeputyPeriodSelector(
    currentType: PeriodType,
    onSelect: (PeriodType) -> Unit,
    modifier: Modifier = Modifier,
    style: DeputyPeriodSelectorStyle = DeputyPeriodSelectorStyle.Card,
) {
    val shape = RoundedCornerShape(14.dp)
    val trackModifier = when (style) {
        DeputyPeriodSelectorStyle.Card -> modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = AtmosphereBrand.SkyMid.copy(alpha = 0.08f))
            .clip(shape)
            .background(AtmosphereBrand.Paper)
            .border(1.dp, AtmosphereBrand.Rule, shape)
            .padding(4.dp)

        DeputyPeriodSelectorStyle.OnSky -> modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
            .padding(4.dp)
    }
    Row(modifier = trackModifier) {
        PeriodType.entries.forEach { type ->
            val selected = currentType == type
            val chipShape = RoundedCornerShape(11.dp)
            val chipBg: Color
            val labelColor: Color
            when (style) {
                DeputyPeriodSelectorStyle.Card -> {
                    chipBg = if (selected) AtmosphereBrand.SkyMid else Color.Transparent
                    labelColor = if (selected) AtmosphereBrand.OnSky else AtmosphereBrand.InkSoft
                }
                DeputyPeriodSelectorStyle.OnSky -> {
                    chipBg = if (selected) AtmosphereBrand.OnSky.copy(alpha = 0.95f) else Color.Transparent
                    labelColor = if (selected) AtmosphereBrand.SkyDeep else AtmosphereBrand.OnSky.copy(alpha = 0.9f)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(chipShape)
                    .background(chipBg)
                    .clickable { onSelect(type) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    type.label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = labelColor,
                )
            }
        }
    }
}

@Composable
fun DeputyReportSection(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp), ambientColor = AtmosphereBrand.SkyDeep.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(20.dp))
            .background(AtmosphereBrand.Paper)
            .border(1.dp, AtmosphereBrand.Rule, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Ink)
            Box(
                Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AtmosphereBrand.SkyMid),
            )
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = AtmosphereBrand.InkMuted, lineHeight = 17.sp)
            }
        }
        content()
    }
}
