/**
 * Поиск ученика и аналитика по результатам.
 * UI-слой.
 */
package ru.golpom.atmosphere.ui.deputy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import ru.golpom.atmosphere.data.local.entity.StudentEntity
import ru.golpom.atmosphere.data.local.model.DailyScore
import ru.golpom.atmosphere.domain.WeekdayLabelsRu
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.atmosphereFormatSigned
import ru.golpom.atmosphere.ui.theme.atmosphereHeatmapColor
import ru.golpom.atmosphere.ui.theme.atmosphereHeatmapTextColor
import ru.golpom.atmosphere.ui.theme.atmosphereScoreColor

@Composable
fun StudentSearchBlock(
    searchQuery: String,
    searchResults: List<StudentEntity>,
    selectedStudentId: String?,
    analytics: StudentAnalytics?,
    periodType: PeriodType,
    onQueryChange: (String) -> Unit,
    onSelectStudent: (String) -> Unit,
    onClear: () -> Unit,
    onPeriodSelect: (PeriodType) -> Unit,
    onCustomPeriodClick: () -> Unit,
    onExport: ((StudentAnalytics) -> Unit)? = null,
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        placeholder = { Text("Фамилия, имя или класс") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AtmosphereBrand.SkyMid,
            unfocusedBorderColor = AtmosphereBrand.Rule,
        ),
    )
    if (searchResults.isNotEmpty()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AtmosphereBrand.Grid),
        ) {
            searchResults.take(6).forEach { student ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelectStudent(student.studentId) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${student.lastName} ${student.firstName}", fontSize = 13.sp)
                    Text(student.classId, fontSize = 11.sp, color = AtmosphereBrand.InkMuted)
                }
            }
        }
    }

    if (selectedStudentId != null && analytics != null) {
        Spacer(Modifier.height(16.dp))
        StudentAnalyticsPanel(
            analytics = analytics,
            periodType = periodType,
            onPeriodSelect = onPeriodSelect,
            onCustomPeriodClick = onCustomPeriodClick,
            onExport = onExport?.let { callback -> { callback(analytics) } },
        )
        TextButton(onClick = onClear) { Text("Сброс") }
    } else if (selectedStudentId != null) {
        Spacer(Modifier.height(12.dp))
        Text("Загрузка…", fontSize = 12.sp, color = AtmosphereBrand.InkMuted)
        TextButton(onClick = onClear) { Text("Сброс") }
    }
}

@Composable
private fun StudentAnalyticsPanel(
    analytics: StudentAnalytics,
    periodType: PeriodType,
    onPeriodSelect: (PeriodType) -> Unit,
    onCustomPeriodClick: () -> Unit,
    onExport: (() -> Unit)? = null,
) {
    InsightAnalyticsPanel(
        title = "${analytics.student.lastName} ${analytics.student.firstName}",
        subtitle = analytics.student.classId,
        totalScore = analytics.totalScore,
        periodLabel = analytics.periodLabel,
        periodType = periodType,
        eventCount = analytics.eventCount,
        praiseCount = analytics.praiseCount,
        violationCount = analytics.violationCount,
        insight = analytics.insight,
        dailyScores = analytics.dailyScores,
        talkSectionTitle = "О чём поговорить с учеником и родителями",
        strengthsSectionTitle = "Где ученик силён",
        onPeriodSelect = onPeriodSelect,
        onCustomPeriodClick = onCustomPeriodClick,
        onExport = onExport,
    )
}

@Composable
fun SubjectInsightCard(item: StudentSubjectInsight, positive: Boolean) {
    val accent = if (positive) AtmosphereBrand.Positive else AtmosphereBrand.Negative
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AtmosphereBrand.Grid)
            .padding(12.dp),
    ) {
        Text(item.subjectTitle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Ink)
        item.subjectContext?.let {
            Text(it, fontSize = 10.sp, color = AtmosphereBrand.InkMuted)
        }
        Spacer(Modifier.height(4.dp))
        Text(item.summary, fontSize = 12.sp, color = accent, lineHeight = 16.sp)
    }
}

@Composable
fun WeekdayRiskTable(patterns: List<StudentWeekdayPattern>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AtmosphereBrand.Grid)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        patterns.forEach { row ->
            if (!row.hasActivity) return@forEach
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(WeekdayLabelsRu.titled(row.dayOfWeek), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (row.positiveCount > 0) {
                        Text("+${row.positiveCount} хорошо", fontSize = 10.sp, color = AtmosphereBrand.Positive)
                    }
                }
                val parts = buildList {
                    if (row.lateCount > 0) add("опозданий ${row.lateCount}")
                    if (row.unpreparedCount > 0) add("не готов ${row.unpreparedCount}")
                    if (row.disruptionCount > 0) add("срыв ${row.disruptionCount}")
                    if (row.gadgetCount > 0) add("гаджет ${row.gadgetCount}")
                    if (row.fightCount > 0) add("грубость ${row.fightCount}")
                }
                if (parts.isNotEmpty()) {
                    Text(parts.joinToString(" · "), fontSize = 11.sp, color = AtmosphereBrand.Negative)
                } else if (row.positiveCount > 0) {
                    Text("только поощрения", fontSize = 11.sp, color = AtmosphereBrand.Positive)
                }
            }
            HorizontalDivider(color = AtmosphereBrand.Rule.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun StudentWeeklyFatigueChart(
    weeks: List<StudentWeekLoad>,
    selectedWeekStart: Long?,
    onWeekClick: (Long) -> Unit,
) {
    val maxViol = maxOf(weeks.maxOfOrNull { it.violationCount } ?: 1, 1)
    Row(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        weeks.forEach { week ->
            val selected = selectedWeekStart == week.weekStartEpochDay
            Column(
                Modifier
                    .width(44.dp)
                    .clickable { onWeekClick(week.weekStartEpochDay) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (week.praiseCount > 0) {
                    Text(
                        "+${week.praiseCount}",
                        fontSize = 8.sp,
                        color = AtmosphereBrand.Positive,
                    )
                }
                val barH = (week.violationCount.toFloat() / maxViol * 72f).coerceAtLeast(if (week.violationCount > 0) 8f else 4f)
                Box(
                    Modifier
                        .width(28.dp)
                        .height(barH.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (selected) AtmosphereBrand.Negative
                            else AtmosphereBrand.Negative.copy(alpha = 0.65f),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (week.violationCount > 0) {
                        Text(
                            "${week.violationCount}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
                Text(
                    week.weekLabel,
                    fontSize = 9.sp,
                    color = if (selected) AtmosphereBrand.SkyDeep else AtmosphereBrand.InkMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun StudentActivityCalendar(
    dailyScores: List<DailyScore>,
    selectedEpochDay: Long?,
    onDayClick: (Long) -> Unit,
) {
    val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd.MM") }
    val scoreByDay = dailyScores.associate { it.day to it.score }
    val weekStarts = dailyScores
        .map { LocalDate.ofEpochDay(it.day).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)) }
        .distinct()
        .sorted()
        .takeLast(12)

    if (weekStarts.isEmpty()) return
    val maxAbs = maxOf(scoreByDay.values.maxOfOrNull { abs(it) } ?: 1, 1)

    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 52.dp)) {
            dayLabels.forEach { d ->
                Text(d, fontSize = 10.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
            }
        }
        weekStarts.forEach { monday ->
            Row(Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(dateFormat.format(monday), fontSize = 9.sp, modifier = Modifier.width(52.dp))
                (0..6).forEach { offset ->
                    val date = monday.plusDays(offset.toLong())
                    val epochDay = date.toEpochDay()
                    val score = scoreByDay[epochDay] ?: 0
                    val hasData = scoreByDay.containsKey(epochDay)
                    val selected = selectedEpochDay == epochDay
                    Box(
                        Modifier
                            .size(36.dp)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .then(
                                if (selected) Modifier.border(2.dp, AtmosphereBrand.SkyDeep, RoundedCornerShape(4.dp))
                                else Modifier,
                            )
                            .background(
                                if (hasData) atmosphereHeatmapColor(score, maxAbs)
                                else AtmosphereBrand.Grid,
                            )
                            .clickable(enabled = hasData) { onDayClick(epochDay) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            when {
                                !hasData -> ""
                                score == 0 -> "·"
                                else -> atmosphereFormatSigned(score)
                            },
                            fontSize = 9.sp,
                            color = if (hasData) atmosphereHeatmapTextColor(score, maxAbs) else AtmosphereBrand.InkMuted,
                        )
                    }
                }
            }
        }
    }
}
