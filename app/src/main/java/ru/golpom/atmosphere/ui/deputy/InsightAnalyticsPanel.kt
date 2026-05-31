/**
 * Панель аналитики и выбора периода на dashboard завуча.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.deputy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import ru.golpom.atmosphere.data.local.model.DailyScore
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.atmosphereFormatSigned
import ru.golpom.atmosphere.ui.theme.atmosphereScoreColor

@Composable
fun InsightAnalyticsPanel(
    title: String,
    subtitle: String,
    totalScore: Int,
    periodLabel: String,
    periodType: PeriodType,
    eventCount: Int,
    praiseCount: Int,
    violationCount: Int,
    insight: InsightReport,
    dailyScores: List<DailyScore>,
    talkSectionTitle: String,
    strengthsSectionTitle: String = "Где коллектив силён",
    onPeriodSelect: (PeriodType) -> Unit,
    onCustomPeriodClick: () -> Unit,
    footer: @Composable () -> Unit = {},
    onExport: (() -> Unit)? = null,
) {
    var selectedEpochDay by remember(title) { mutableStateOf<Long?>(null) }
    var selectedWeekStart by remember(title) { mutableStateOf<Long?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Ink)
                Text("$subtitle · $periodLabel", fontSize = 11.sp, color = AtmosphereBrand.InkMuted)
            }
            Text(
                atmosphereFormatSigned(totalScore),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = atmosphereScoreColor(totalScore),
            )
        }

        Text("Период анализа", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AtmosphereBrand.InkSoft)
        DeputyPeriodSelector(
            currentType = periodType,
            onSelect = { type ->
                if (type == PeriodType.CUSTOM) onCustomPeriodClick() else onPeriodSelect(type)
            },
        )

        if (onExport != null) {
            DeputyExportButton(onClick = onExport)
        }

        if (eventCount == 0) {
            Text("За выбранный период отметок нет", fontSize = 13.sp, color = AtmosphereBrand.InkMuted)
            return
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InsightStatChip("Отметок", "$eventCount", AtmosphereBrand.Ink, Modifier.weight(1f))
            InsightStatChip("Поощрений", "$praiseCount", AtmosphereBrand.Positive, Modifier.weight(1f))
            InsightStatChip("Нарушений", "$violationCount", AtmosphereBrand.Negative, Modifier.weight(1f))
        }

        if (insight.talkPoints.isNotEmpty()) {
            InsightTalkCard(talkSectionTitle, insight.talkPoints)
        }

        if (insight.weeklyLoads.isNotEmpty()) {
            Text("По неделям", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Ink)
            Text(
                "Высота столбца — нарушения за неделю. Нажмите неделю, чтобы отфильтровать календарь ниже.",
                fontSize = 10.sp,
                color = AtmosphereBrand.InkMuted,
                lineHeight = 14.sp,
            )
            StudentWeeklyFatigueChart(
                weeks = insight.weeklyLoads,
                selectedWeekStart = selectedWeekStart,
                onWeekClick = { week ->
                    selectedWeekStart = if (selectedWeekStart == week) null else week
                    selectedEpochDay = null
                },
            )
        }

        if (insight.weekdayPatterns.any { it.hasActivity }) {
            Text("По дням недели", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Ink)
            Text(
                "Сколько раз встречаются нарушения в каждый учебный день",
                fontSize = 10.sp,
                color = AtmosphereBrand.InkMuted,
            )
            WeekdayRiskTable(insight.weekdayPatterns)
        }

        if (dailyScores.isNotEmpty()) {
            Text("Календарь — нажмите день", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Ink)
            val filteredDaily = if (selectedWeekStart != null) {
                dailyScores.filter {
                    LocalDate.ofEpochDay(it.day)
                        .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                        .toEpochDay() == selectedWeekStart
                }
            } else {
                dailyScores
            }
            StudentActivityCalendar(
                dailyScores = filteredDaily,
                selectedEpochDay = selectedEpochDay,
                onDayClick = { day ->
                    selectedEpochDay = if (selectedEpochDay == day) null else day
                },
            )
            selectedEpochDay?.let { day ->
                insight.dayDetails[day]?.let { InsightDayDetailCard(it) }
            }
        }

        if (insight.subjectProblems.isNotEmpty()) {
            Text("На каких уроках проблемы", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Negative)
            insight.subjectProblems.take(8).forEach { SubjectInsightCard(it, positive = false) }
        }

        if (insight.subjectStrengths.isNotEmpty()) {
            Text(strengthsSectionTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Positive)
            insight.subjectStrengths.take(8).forEach { SubjectInsightCard(it, positive = true) }
        }

        footer()
    }
}

@Composable
fun InsightStatChip(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AtmosphereBrand.Grid)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 10.sp, color = AtmosphereBrand.InkMuted)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = accent)
    }
}

@Composable
fun InsightTalkCard(title: String, points: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AtmosphereBrand.SkyDeep.copy(alpha = 0.08f))
            .border(1.dp, AtmosphereBrand.SkyDeep.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.SkyDeep)
        points.forEach { point ->
            Row {
                Text("• ", fontSize = 12.sp, color = AtmosphereBrand.Ink)
                Text(point, fontSize = 12.sp, color = AtmosphereBrand.Ink, lineHeight = 17.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun InsightDayDetailCard(detail: StudentDayDetail) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, AtmosphereBrand.SkyMid.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .background(AtmosphereBrand.Mist)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "${detail.dateLabel}, ${detail.weekdayLabel}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AtmosphereBrand.Ink,
        )
        Text(detail.summary, fontSize = 12.sp, color = AtmosphereBrand.InkSoft)
        detail.events.forEach { event ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(event.timeLabel, fontSize = 11.sp, color = AtmosphereBrand.InkMuted, modifier = Modifier.width(40.dp))
                Column(Modifier.weight(1f)) {
                    event.studentName?.let {
                        Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Ink)
                    }
                    val subjLine = if (event.subjectContext != null) {
                        "${event.subjectTitle} (${event.subjectContext})"
                    } else {
                        event.subjectTitle
                    }
                    Text(subjLine, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AtmosphereBrand.InkSoft)
                    Text(event.behaviorLabel, fontSize = 11.sp, color = AtmosphereBrand.InkMuted)
                }
                Text(
                    atmosphereFormatSigned(event.scoreImpact),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = atmosphereScoreColor(event.scoreImpact),
                )
            }
            HorizontalDivider(color = AtmosphereBrand.Rule.copy(alpha = 0.35f))
        }
    }
}
