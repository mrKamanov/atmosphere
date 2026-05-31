/**
 * Шторка быстрых отметок поведения на уроке.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.golpom.atmosphere.domain.BehaviorPreset
import ru.golpom.atmosphere.ui.theme.TextPrimary

private val GreenBg = Color(0xFFDCFCE7)
private val GreenText = Color(0xFF166534)
private val RedBg = Color(0xFFFEE2E2)
private val RedText = Color(0xFF991B1B)

private data class PresetInfo(
    val icon: ImageVector,
    val bg: Color,
    val textColor: Color,
)

private fun presetStyle(preset: BehaviorPreset): PresetInfo = when (preset) {
    BehaviorPreset.ACTIVE_WORK -> PresetInfo(Icons.Default.ThumbUp, GreenBg, GreenText)
    BehaviorPreset.CLASS_HELP -> PresetInfo(Icons.Default.Groups, GreenBg, GreenText)
    BehaviorPreset.FOCUS -> PresetInfo(Icons.Default.TrendingUp, GreenBg, GreenText)
    BehaviorPreset.EXEMPLARY_BEHAVIOR -> PresetInfo(Icons.Default.Favorite, GreenBg, GreenText)
    BehaviorPreset.DISRUPTION -> PresetInfo(Icons.Default.Bolt, RedBg, RedText)
    BehaviorPreset.GADGET -> PresetInfo(Icons.Default.Warning, RedBg, RedText)
    BehaviorPreset.LATE -> PresetInfo(Icons.Default.Schedule, RedBg, RedText)
    BehaviorPreset.UNPREPARED -> PresetInfo(Icons.Default.Book, RedBg, RedText)
    BehaviorPreset.FIGHT -> PresetInfo(Icons.Default.PriorityHigh, RedBg, RedText)
    BehaviorPreset.PROFANITY -> PresetInfo(Icons.Default.Block, RedBg, RedText)
}

private fun presetShortLabel(preset: BehaviorPreset): String = when (preset) {
    BehaviorPreset.ACTIVE_WORK -> "Старается"
    BehaviorPreset.CLASS_HELP -> "Помощь"
    BehaviorPreset.FOCUS -> "Прогресс"
    BehaviorPreset.EXEMPLARY_BEHAVIOR -> "Поведение"
    BehaviorPreset.DISRUPTION -> "Срыв"
    BehaviorPreset.GADGET -> "Гаджет"
    BehaviorPreset.LATE -> "Опоздал"
    BehaviorPreset.UNPREPARED -> "Не готов"
    BehaviorPreset.FIGHT -> "Драка"
    BehaviorPreset.PROFANITY -> "Лексика"
}

private fun presetHint(preset: BehaviorPreset): String? = when (preset) {
    BehaviorPreset.ACTIVE_WORK -> "включается в работу"
    BehaviorPreset.FOCUS -> "улучшение за период"
    else -> null
}

private val positivePresets = listOf(
    BehaviorPreset.ACTIVE_WORK,
    BehaviorPreset.CLASS_HELP,
    BehaviorPreset.FOCUS,
    BehaviorPreset.EXEMPLARY_BEHAVIOR,
)

private val negativePresets = listOf(
    BehaviorPreset.DISRUPTION,
    BehaviorPreset.GADGET,
    BehaviorPreset.LATE,
    BehaviorPreset.UNPREPARED,
    BehaviorPreset.FIGHT,
    BehaviorPreset.PROFANITY,
)

@Composable
fun LessonQuickActionsSheet(
    studentLabel: String,
    onPick: (BehaviorPreset) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(studentLabel, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            TextButton(onClick = onDismiss) {
                Text("Готово", fontWeight = FontWeight.Medium, color = TextPrimary.copy(alpha = 0.6f))
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Положительные", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GreenText)
        Spacer(Modifier.height(8.dp))
        positivePresets.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { preset ->
                    CompactPresetCard(
                        preset = preset,
                        onClick = { onPick(preset) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        Text("Отрицательные", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = RedText)
        Spacer(Modifier.height(8.dp))
        negativePresets.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { preset ->
                    CompactPresetCard(
                        preset = preset,
                        onClick = { onPick(preset) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CompactPresetCard(preset: BehaviorPreset, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val style = presetStyle(preset)
    val hint = presetHint(preset)
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = style.bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(28.dp).background(style.textColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(style.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = style.textColor)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                presetShortLabel(preset),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = style.textColor,
                textAlign = TextAlign.Center,
            )
            Text(
                hint ?: " ",
                fontSize = 9.sp,
                color = style.textColor.copy(alpha = if (hint != null) 0.75f else 0f),
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 4.dp).padding(top = 2.dp),
            )
        }
    }
}
