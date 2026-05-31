/**
 * Панель выбора импортированных пакетов отметок.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.deputy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import ru.golpom.atmosphere.data.local.entity.ImportBatchEntity
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

@Composable
fun DeputyImportBatchesPanel(
    localDataEnabled: Boolean,
    onLocalDataToggle: (Boolean) -> Unit,
    batches: List<ImportBatchEntity>,
    onToggleEnabled: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    val zone = ZoneId.systemDefault()
    var filesExpanded by rememberSaveable {
        mutableStateOf(batches.size <= 2)
    }

    DeputyReportSection(title = "Какие отметки показывать") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Переключатели влияют только на графики и таблицы на этом экране. " +
                    "Сами отметки не удаляются — их можно снова включить в любой момент.",
                fontSize = 12.sp,
                color = TextSecondary,
            )

            Text(
                "Данные с этого телефона",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary.copy(alpha = 0.75f),
            )
            AnalyticsSourceCard(
                title = "Мои уроки",
                subtitle = "Отметки, которые вы ставили на этом устройстве",
                enabled = localDataEnabled,
                onToggle = onLocalDataToggle,
                highlighted = true,
            )

            CollapsibleTeacherReportsHeader(
                batchCount = batches.size,
                enabledCount = batches.count { it.enabled },
                expanded = filesExpanded,
                onToggle = { filesExpanded = !filesExpanded },
            )

            AnimatedVisibility(
                visible = filesExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                if (batches.isEmpty()) {
                    Text(
                        "Пока нет загруженных отчётов. Нажмите кнопку загрузки вверху экрана и выберите файл от учителя.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        batches.forEach { batch ->
                            TeacherReportCard(
                                batch = batch,
                                importedAtLabel = fmt.format(Instant.ofEpochMilli(batch.importedAtMillis).atZone(zone)),
                                onToggleEnabled = onToggleEnabled,
                                onDelete = onDelete,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleTeacherReportsHeader(
    batchCount: Int,
    enabledCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "Отчёты от учителей",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary.copy(alpha = 0.75f),
            )
            if (!expanded && batchCount > 0) {
                Text(
                    teacherReportsSummary(batchCount, enabledCount),
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            } else if (!expanded && batchCount == 0) {
                Text(
                    "Нет загруженных отчётов",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Свернуть список" else "Развернуть список",
            tint = TextSecondary,
        )
    }
}

private fun teacherReportsSummary(batchCount: Int, enabledCount: Int): String {
    val countLabel = when {
        batchCount % 10 == 1 && batchCount % 100 != 11 -> "$batchCount отчёт"
        batchCount % 10 in 2..4 && batchCount % 100 !in 12..14 -> "$batchCount отчёта"
        else -> "$batchCount отчётов"
    }
    return when {
        enabledCount == batchCount -> "$countLabel · все учитываются"
        enabledCount == 0 -> "$countLabel · все скрыты"
        else -> "$countLabel · $enabledCount в аналитике"
    }
}

@Composable
private fun AnalyticsSourceCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    highlighted: Boolean = false,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                AtmosphereBrand.SkyMid.copy(alpha = 0.08f)
            } else {
                CardBg
            },
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    if (enabled) "Учитывается" else "Скрыто",
                    fontSize = 10.sp,
                    color = if (enabled) AtmosphereBrand.SkyMid else TextSecondary,
                )
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить отчёт", tint = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun TeacherReportCard(
    batch: ImportBatchEntity,
    importedAtLabel: String,
    onToggleEnabled: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    AnalyticsSourceCard(
        title = batch.teacherDisplayName,
        subtitle = buildString {
            append(batch.periodLabel)
            append(" · ")
            append("${batch.insertedCount} отметок · загружено $importedAtLabel")
        },
        enabled = batch.enabled,
        onToggle = { onToggleEnabled(batch.batchId, it) },
        onDelete = { onDelete(batch.batchId) },
    )
}
