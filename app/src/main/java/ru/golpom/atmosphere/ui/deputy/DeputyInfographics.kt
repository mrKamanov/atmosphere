/**
 * Бизнес-инфографика завуча: donut, bullet-chart, тренд с осями, heatmap с маржами, таблицы.
 * UI-слой (Compose); §6.2 ТЗ.
 */
package ru.golpom.atmosphere.ui.deputy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.AtmosphereHeatmapLegend
import ru.golpom.atmosphere.ui.theme.atmosphereFormatSigned
import ru.golpom.atmosphere.ui.theme.atmosphereHeatmapColor
import ru.golpom.atmosphere.ui.theme.atmosphereHeatmapTextColor
import ru.golpom.atmosphere.ui.theme.atmosphereScoreColor

@Composable
fun ExecutiveSummaryPanel(
    summary: ExecutiveSummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AtmosphereBrand.Paper)
            .border(1.dp, AtmosphereBrand.Rule, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            CompositionDonut(
                positivePct = summary.positiveSharePct,
                negativePct = summary.negativeSharePct,
                modifier = Modifier.size(140.dp),
            )
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricCell(
                    label = "Событий за период",
                    value = "${summary.totalEvents}",
                    delta = summary.eventsDelta?.let { formatDelta(it) },
                )
                MetricCell(
                    label = "Индекс дисциплины",
                    value = "${summary.disciplineIndexPct}%",
                    hint = "доля «+» минус доля «−»",
                )
                MetricCell(
                    label = "Ср. балл / активный ученик",
                    value = "%.1f".format(summary.avgScorePerActiveStudent),
                    delta = summary.scoreDelta?.let { formatDelta(it, signed = true) },
                )
                MetricCell(
                    label = "Охват",
                    value = "${summary.activeStudents} из ${summary.studentCount}",
                    hint = "${summary.classCount} классов",
                )
            }
        }
        HorizontalDivider(color = AtmosphereBrand.Rule)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            BalanceTile("Баланс", atmosphereFormatSigned(summary.totalScore), atmosphereScoreColor(summary.totalScore))
            BalanceTile("Поощрения", "+${summary.totalPositive}", AtmosphereBrand.Positive)
            BalanceTile("Нарушения", summary.totalNegative.toString(), AtmosphereBrand.Negative)
        }
    }
}

@Composable
private fun BalanceTile(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), fontSize = 9.sp, letterSpacing = 1.sp, color = AtmosphereBrand.InkMuted)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Light, color = color)
    }
}

@Composable
private fun MetricCell(label: String, value: String, delta: String? = null, hint: String? = null) {
    Column {
        Text(label, fontSize = 11.sp, color = AtmosphereBrand.InkMuted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Ink)
            delta?.let {
                Spacer(Modifier.width(8.dp))
                Text(it, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = deltaColor(it))
            }
        }
        hint?.let { Text(it, fontSize = 10.sp, color = AtmosphereBrand.InkMuted) }
    }
}

private fun formatDelta(v: Int, signed: Boolean = false): String {
    val prefix = when {
        v > 0 -> "▲ "
        v < 0 -> "▼ "
        else -> "— "
    }
    val body = if (signed) atmosphereFormatSigned(v) else abs(v).toString()
    return prefix + body
}

private fun deltaColor(delta: String): Color = when {
    delta.startsWith("▲") -> AtmosphereBrand.Positive
    delta.startsWith("▼") -> AtmosphereBrand.Negative
    else -> AtmosphereBrand.InkMuted
}

@Composable
fun CompositionDonut(positivePct: Int, negativePct: Int, modifier: Modifier = Modifier) {
    val neutralPct = (100 - positivePct - negativePct).coerceAtLeast(0)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 22f
            val radius = size.minDimension / 2f - stroke
            val center = Offset(size.width / 2f, size.height / 2f)
            var start = -90f
            fun sweep(pct: Int, color: Color) {
                if (pct <= 0) return
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = 360f * pct / 100f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                start += 360f * pct / 100f
            }
            sweep(positivePct, AtmosphereBrand.Positive)
            sweep(negativePct, AtmosphereBrand.Negative)
            sweep(neutralPct, AtmosphereBrand.NeutralSoft)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$positivePct%", fontSize = 28.sp, fontWeight = FontWeight.Light, color = AtmosphereBrand.Ink)
            Text("поощрений", fontSize = 10.sp, color = AtmosphereBrand.InkMuted)
        }
    }
}

@Composable
fun TrendAnalyticsPanel(trend: TrendAnalytics, modifier: Modifier = Modifier) {
    if (trend.points.isEmpty()) return
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd.MM") }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalloutChip("Сумма", atmosphereFormatSigned(trend.total), AtmosphereBrand.Ink)
            CalloutChip("Среднее", "%.1f".format(trend.average), AtmosphereBrand.SkyDeep)
            CalloutChip("Дней +", "${trend.positiveDays}", AtmosphereBrand.Positive)
            CalloutChip("Дней −", "${trend.negativeDays}", AtmosphereBrand.Negative)
        }
        trend.best?.let { best ->
            trend.worst?.let { worst ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalloutChip(
                        "Пик ↑ ${dateFmt.format(LocalDate.ofEpochDay(best.day))}",
                        atmosphereFormatSigned(best.score),
                        AtmosphereBrand.Positive,
                        Modifier.weight(1f),
                    )
                    CalloutChip(
                        "Пик ↓ ${dateFmt.format(LocalDate.ofEpochDay(worst.day))}",
                        atmosphereFormatSigned(worst.score),
                        AtmosphereBrand.Negative,
                        Modifier.weight(1f),
                    )
                }
            }
        }
        ReadableTrendChart(dailyPoints = trend.points)
    }
}

@Composable
private fun CalloutChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AtmosphereBrand.Grid)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 10.sp, color = AtmosphereBrand.InkMuted)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = accent)
    }
}

@Composable
fun ClassBulletRankingTable(
    rows: List<ClassRankingRow>,
    onClassClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    Column(modifier) {
        ClassRankingHeaderRow()
        rows.forEach { row ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onClassClick(row.classId) }
                    .padding(vertical = 8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(40.dp), contentAlignment = Alignment.CenterStart) {
                        RankBadge(row.rank)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        row.classId,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AtmosphereBrand.Ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        atmosphereFormatSigned(row.score),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = atmosphereScoreColor(row.score),
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.End,
                    )
                    Text(
                        "%.0f%%".format(row.shareOfTotalPct),
                        fontSize = 12.sp,
                        color = AtmosphereBrand.InkSoft,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.End,
                    )
                    val vsMedian = row.score - row.medianScore
                    Text(
                        formatDelta(vsMedian, signed = true),
                        fontSize = 11.sp,
                        color = deltaColor(formatDelta(vsMedian, signed = true)),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                    )
                }
                Spacer(Modifier.height(6.dp))
                BulletBar(
                    value = row.score,
                    median = row.medianScore,
                    max = row.maxScore,
                )
            }
            HorizontalDivider(color = AtmosphereBrand.Rule.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun BulletBar(value: Int, median: Int, max: Int) {
    val maxAbs = maxOf(abs(max), 1)
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val w = size.width
        val h = size.height
        val midX = w / 2f
        drawRoundRect(
            color = AtmosphereBrand.Rule,
            topLeft = Offset(0f, h * 0.25f),
            size = Size(w, h * 0.5f),
            cornerRadius = CornerRadius(4f, 4f),
        )
        val barW = (abs(value).toFloat() / maxAbs) * (w / 2f - 4f)
        val left = if (value >= 0) midX else midX - barW
        drawRoundRect(
            color = atmosphereScoreColor(value).copy(alpha = 0.85f),
            topLeft = Offset(left, h * 0.2f),
            size = Size(barW.coerceAtLeast(4f), h * 0.6f),
            cornerRadius = CornerRadius(3f, 3f),
        )
        val medOffset = (median.toFloat() / maxAbs).coerceIn(-1f, 1f) * (w / 2f - 4f)
        val medX = midX + medOffset
        drawLine(
            AtmosphereBrand.TealAccent,
            Offset(medX, 0f),
            Offset(medX, h),
            strokeWidth = 2f,
        )
        drawLine(AtmosphereBrand.InkMuted.copy(alpha = 0.5f), Offset(midX, 0f), Offset(midX, h), 1f)
    }
}

@Composable
private fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    val bg = when (rank) {
        1 -> AtmosphereBrand.SkyMid
        2 -> AtmosphereBrand.InkMuted
        3 -> Color(0xFF94A3B8)
        else -> AtmosphereBrand.Grid
    }
    val fg = if (rank <= 3) Color.White else AtmosphereBrand.InkSoft
    Box(
        modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text("$rank", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
fun BusinessHeatmapMatrix(matrix: HeatmapMatrix, modifier: Modifier = Modifier) {
    if (matrix.classIds.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AtmosphereHeatmapLegend(matrix.maxAbs)
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(48.dp))
            matrix.dayLabels.forEach { day ->
                Text(
                    day,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AtmosphereBrand.InkSoft,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
            Text("Итого", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
        }
        matrix.classIds.forEach { classId ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    classId,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(48.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                matrix.dayKeys.forEach { dow ->
                    val score = matrix.cells[classId to dow] ?: 0
                    HeatmapCellBox(score, matrix.maxAbs, Modifier.weight(1f))
                }
                Text(
                    atmosphereFormatSigned(matrix.rowTotals[classId] ?: 0),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center,
                    color = atmosphereScoreColor(matrix.rowTotals[classId] ?: 0),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("Итого за день", fontSize = 9.sp, color = AtmosphereBrand.InkMuted, modifier = Modifier.width(48.dp))
            matrix.dayKeys.forEach { dow ->
                val total = matrix.colTotals[dow] ?: 0
                Text(
                    atmosphereFormatSigned(total),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = atmosphereScoreColor(total),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun HeatmapCellBox(score: Int, maxAbs: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(atmosphereHeatmapColor(score, maxAbs)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (score == 0) "·" else atmosphereFormatSigned(score),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = atmosphereHeatmapTextColor(score, maxAbs),
        )
    }
}

private val WeekdaySliceColors = listOf(
    AtmosphereBrand.SkyDeep,
    AtmosphereBrand.SkyMid,
    AtmosphereBrand.TealAccent,
    Color(0xFF5BA88E),
    Color(0xFF7EC4A8),
)

@Composable
fun WeekdayDonutPanel(rows: List<WeekdayPatternRow>, modifier: Modifier = Modifier) {
    val days = rows.filter { it.dayOfWeek in 1..5 }.sortedBy { it.dayOfWeek }
    if (days.isEmpty()) return
    val totalEvents = days.sumOf { it.entryCount }.coerceAtLeast(1)
    val peak = days.maxByOrNull { it.entryCount }
    val peakPct = peak?.let { it.entryCount * 100 / totalEvents } ?: 0

    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(Modifier.size(148.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 20f
                val radius = size.minDimension / 2f - stroke
                val center = Offset(size.width / 2f, size.height / 2f)
                var start = -90f
                days.forEachIndexed { i, row ->
                    val pct = row.entryCount * 100f / totalEvents
                    if (pct <= 0f) return@forEachIndexed
                    val color = WeekdaySliceColors.getOrElse(i) { AtmosphereBrand.InkMuted }
                    drawArc(
                        color = color,
                        startAngle = start,
                        sweepAngle = 360f * pct / 100f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                    start += 360f * pct / 100f
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    peak?.dayLabel ?: "—",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AtmosphereBrand.Ink,
                )
                Text("пик · $peakPct%", fontSize = 10.sp, color = AtmosphereBrand.InkMuted)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            days.forEachIndexed { i, row ->
                val pct = row.entryCount * 100 / totalEvents
                val color = WeekdaySliceColors.getOrElse(i) { AtmosphereBrand.InkMuted }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(row.dayLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Ink, modifier = Modifier.width(28.dp))
                    Text("$pct%", fontSize = 12.sp, color = AtmosphereBrand.InkSoft, modifier = Modifier.width(36.dp))
                    Text(
                        atmosphereFormatSigned(row.totalScore),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = atmosphereScoreColor(row.totalScore),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                    )
                }
                Text(
                    "${row.entryCount} событий",
                    fontSize = 10.sp,
                    color = AtmosphereBrand.InkMuted,
                    modifier = Modifier.padding(start = 18.dp),
                )
            }
        }
    }
}

@Composable
fun StudentRankingTable(
    title: String,
    rows: List<StudentRankingRow>,
    onStudentClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    Column(modifier) {
        if (title.isNotBlank()) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.InkSoft)
            Spacer(Modifier.height(8.dp))
        }
        TableHeaderRow(listOf("#", "Ученик", "Класс", "Балл", "%"))
        rows.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onStudentClick(row.studentId) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RankBadge(row.rank, Modifier.width(32.dp))
                Text(
                    row.name,
                    fontSize = 13.sp,
                    color = AtmosphereBrand.Ink,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(row.classId, fontSize = 11.sp, color = AtmosphereBrand.InkMuted, modifier = Modifier.width(44.dp))
                Text(
                    atmosphereFormatSigned(row.score),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = atmosphereScoreColor(row.score),
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.End,
                )
                Text(
                    "${row.shareOfListPct}%",
                    fontSize = 11.sp,
                    color = AtmosphereBrand.InkSoft,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End,
                )
            }
            ShareStrip(row.shareOfListPct, atmosphereScoreColor(row.score))
            HorizontalDivider(color = AtmosphereBrand.Rule.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun ShareStrip(pct: Int, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(AtmosphereBrand.Rule),
    ) {
        Box(
            Modifier
                .fillMaxWidth(pct / 100f)
                .fillMaxHeight()
                .background(color.copy(alpha = 0.7f)),
        )
    }
}

@Composable
private fun ClassRankingHeaderRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AtmosphereBrand.Grid)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("#", fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.InkMuted, modifier = Modifier.width(40.dp))
        Spacer(Modifier.width(10.dp))
        Text("КЛАСС", fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.InkMuted, modifier = Modifier.weight(1f))
        Text("БАЛЛ", fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.InkMuted, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
        Text("ДОЛЯ", fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.InkMuted, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
        Text("К СРЕДНЕМУ", fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.InkMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
private fun TableHeaderRow(columns: List<String>) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AtmosphereBrand.Grid)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        columns.forEachIndexed { i, col ->
            Text(
                col.uppercase(),
                fontSize = 9.sp,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.SemiBold,
                color = AtmosphereBrand.InkMuted,
                modifier = when (i) {
                    0 -> Modifier.width(32.dp)
                    1 -> Modifier.weight(1f)
                    2 -> Modifier.width(44.dp)
                    3 -> Modifier.width(44.dp)
                    else -> Modifier.width(36.dp)
                },
                textAlign = if (i >= 2) TextAlign.End else TextAlign.Start,
            )
        }
    }
}

@Composable
fun ParallelComparisonPanel(groups: List<ParallelGroupRow>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        groups.forEach { group ->
            val maxInGroup = maxOf(group.classes.maxOfOrNull { abs(it.score) } ?: 1, 1)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AtmosphereBrand.Grid)
                    .padding(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${group.parallel} классы",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AtmosphereBrand.Ink,
                    )
                    Text(
                        atmosphereFormatSigned(group.totalScore),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = atmosphereScoreColor(group.totalScore),
                    )
                }
                Spacer(Modifier.height(10.dp))
                group.classes.forEach { cs ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(cs.classId, fontSize = 12.sp, modifier = Modifier.width(44.dp))
                        Canvas(Modifier.weight(1f).height(12.dp)) {
                            val frac = abs(cs.score).toFloat() / maxInGroup
                            drawRoundRect(
                                AtmosphereBrand.Rule,
                                topLeft = Offset.Zero,
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(3f, 3f),
                            )
                            drawRoundRect(
                                atmosphereScoreColor(cs.score),
                                topLeft = Offset.Zero,
                                size = Size(size.width * frac.coerceIn(0.05f, 1f), size.height),
                                cornerRadius = CornerRadius(3f, 3f),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            atmosphereFormatSigned(cs.score),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = atmosphereScoreColor(cs.score),
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeputyMatrixHeatmap(
    rowLabels: List<Pair<String, String>>,
    columnLabels: List<String>,
    cellScore: (rowKey: String, colKey: String) -> Int,
    modifier: Modifier = Modifier,
    rowKey: (Pair<String, String>) -> String = { it.first },
    colKey: (String) -> String = { it },
    minColWidth: Dp = 72.dp,
    labelWidth: Dp = 120.dp,
) {
    if (rowLabels.isEmpty() || columnLabels.isEmpty()) return
    val allScores = remember(rowLabels, columnLabels) {
        rowLabels.flatMap { row -> columnLabels.map { col -> cellScore(rowKey(row), colKey(col)) } }
    }
    val maxAbs = remember(allScores) { maxOf(allScores.maxOfOrNull { abs(it) } ?: 1, 1) }

    Column(modifier) {
        AtmosphereHeatmapLegend(maxAbs)
        Spacer(Modifier.height(12.dp))
        val totalW = labelWidth + minColWidth * columnLabels.size
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Row(Modifier.width(totalW)) {
                Box(Modifier.width(labelWidth))
                columnLabels.forEach { col ->
                    Text(
                        col,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = AtmosphereBrand.InkMuted,
                        modifier = Modifier.width(minColWidth),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            rowLabels.forEach { row ->
                Row(Modifier.width(totalW).height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.second,
                        fontSize = 11.sp,
                        color = AtmosphereBrand.Ink,
                        modifier = Modifier.width(labelWidth).padding(end = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    columnLabels.forEach { col ->
                        val score = cellScore(rowKey(row), colKey(col))
                        HeatmapCellBox(score, maxAbs, Modifier.width(minColWidth))
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutiveBriefPanel(points: List<ExecutiveBriefPoint>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        points.forEach { p ->
            val accent = when (p.tone) {
                BriefTone.POSITIVE -> AtmosphereBrand.Positive
                BriefTone.NEGATIVE -> AtmosphereBrand.Negative
                BriefTone.NEUTRAL -> AtmosphereBrand.SkyDeep
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, AtmosphereBrand.Rule, RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Text(
                    "%02d".format(p.number),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    color = AtmosphereBrand.TealAccent,
                    modifier = Modifier.width(36.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(p.headline, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Ink)
                    Text(p.detail, fontSize = 12.sp, color = AtmosphereBrand.InkMuted, lineHeight = 16.sp)
                }
                Text(p.metric, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent)
            }
        }
    }
}
