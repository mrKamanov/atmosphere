/**
 * Диалог выбора периода выгрузки (день / неделя / месяц / учебный год).
 */
@file:OptIn(ExperimentalLayoutApi::class)

package ru.golpom.atmosphere.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import ru.golpom.atmosphere.domain.export.ExportPeriodCalculator
import ru.golpom.atmosphere.domain.export.ExportPeriodKind
import ru.golpom.atmosphere.domain.export.ExportPeriodSelection
import ru.golpom.atmosphere.domain.export.TeacherExportOptions
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
data class ExportSecurityDefaults(
    val neutralFileName: Boolean = true,
)

@Composable
fun ExportPeriodDialog(
    title: String,
    securityDefaults: ExportSecurityDefaults = ExportSecurityDefaults(),
    onConfirm: (ExportPeriodSelection, TeacherExportOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(ExportPeriodKind.WEEK) }
    var useCurrent by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }

    val previewSelection = ExportPeriodSelection(
        kind = kind,
        useCurrent = useCurrent,
        anchorDateMillis = if (useCurrent) null else selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )
    val previewLabel = ExportPeriodCalculator.label(previewSelection)

    val dateFormat = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val months = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
    )
    val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = TextPrimary)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportPeriodKind.entries.forEach { option ->
                        FilterChip(
                            selected = kind == option,
                            onClick = { kind = option },
                            label = { Text(option.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AtmosphereBrand.SkyMid.copy(alpha = 0.2f),
                            ),
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (kind) {
                                ExportPeriodKind.DAY -> "За сегодня"
                                ExportPeriodKind.WEEK -> "За эту неделю (пн–сб)"
                                ExportPeriodKind.MONTH -> "За этот месяц (до сегодня)"
                                ExportPeriodKind.SCHOOL_YEAR -> "За учебный год (до сегодня)"
                            },
                            fontSize = 14.sp,
                            color = TextPrimary,
                        )
                    }
                    Switch(checked = useCurrent, onCheckedChange = { useCurrent = it })
                }

                if (!useCurrent) {
                    Text(
                        when (kind) {
                            ExportPeriodKind.DAY -> "Выберите день в календаре"
                            ExportPeriodKind.WEEK -> "Выберите любой день нужной недели"
                            ExportPeriodKind.MONTH -> "Выберите любой день нужного месяца"
                            ExportPeriodKind.SCHOOL_YEAR -> "Выберите дату, до которой включить отметки"
                        },
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                        Text("${months[currentMonth.monthValue - 1]} ${currentMonth.year}", fontWeight = FontWeight.Medium)
                        TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        dayLabels.forEach {
                            Text(it, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                    val firstDay = currentMonth.withDayOfMonth(1)
                    val startDow = firstDay.dayOfWeek.value % 7
                    val daysInMonth = currentMonth.lengthOfMonth()
                    val rows = (startDow + daysInMonth + 6) / 7
                    Column {
                        (0 until rows).forEach { row ->
                            Row(Modifier.fillMaxWidth()) {
                                (0 until 7).forEach { col ->
                                    val dayNum = row * 7 + col - startDow + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val date = currentMonth.withDayOfMonth(dayNum)
                                        val selected = date == selectedDate
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) AtmosphereBrand.SkyMid else Color.Transparent)
                                                .clickable { selectedDate = date },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                "$dayNum",
                                                color = if (selected) Color.White else TextPrimary,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        }
                                    } else {
                                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                Text("В отчёт попадут отметки за: $previewLabel", fontSize = 13.sp, color = TextSecondary)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(onClick = {
                        onConfirm(
                            ExportPeriodSelection(
                                kind = kind,
                                useCurrent = useCurrent,
                                anchorDateMillis = if (useCurrent) {
                                    null
                                } else {
                                    selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                },
                            ),
                            TeacherExportOptions(neutralFileName = true),
                        )
                    }) {
                        Text("Создать отчёт", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
