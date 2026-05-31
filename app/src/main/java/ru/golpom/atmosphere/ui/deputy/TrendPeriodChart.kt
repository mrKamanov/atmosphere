/**
 * Читаемый график «Динамика периода»: агрегация по дням/неделям/месяцам и горизонтальный скролл.
 * UI-слой (Compose); §6.2 ТЗ.
 */
package ru.golpom.atmosphere.ui.deputy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import ru.golpom.atmosphere.data.local.model.DailyScore
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.atmosphereFormatSigned
import ru.golpom.atmosphere.ui.theme.atmosphereScoreColor

enum class TrendGranularity(val label: String) {
    DAY("По дням"),
    WEEK("По неделям"),
    MONTH("По месяцам"),
}

data class TrendChartBucket(
    val key: Long,
    val score: Int,
    val label: String,
    val detail: String,
)

fun defaultTrendGranularity(dayCount: Int): TrendGranularity = when {
    dayCount <= 21 -> TrendGranularity.DAY
    dayCount <= 100 -> TrendGranularity.WEEK
    else -> TrendGranularity.MONTH
}

fun aggregateTrendBuckets(
    daily: List<DailyScore>,
    granularity: TrendGranularity,
): List<TrendChartBucket> {
    if (daily.isEmpty()) return emptyList()
    val sorted = daily.sortedBy { it.day }
    return when (granularity) {
        TrendGranularity.DAY -> sorted.map { d ->
            val date = LocalDate.ofEpochDay(d.day)
            TrendChartBucket(
                key = d.day,
                score = d.score,
                label = date.format(DateTimeFormatter.ofPattern("dd")),
                detail = date.format(DateTimeFormatter.ofPattern("dd.MM")),
            )
        }
        TrendGranularity.WEEK -> {
            sorted.groupBy { d ->
                val date = LocalDate.ofEpochDay(d.day)
                val monday = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                monday.toEpochDay()
            }.map { (mondayEpoch, scores) ->
                val start = LocalDate.ofEpochDay(mondayEpoch)
                val end = start.plusDays(6)
                val sum = scores.sumOf { it.score }
                TrendChartBucket(
                    key = mondayEpoch,
                    score = sum,
                    label = start.format(DateTimeFormatter.ofPattern("dd.MM")),
                    detail = "${start.format(DateTimeFormatter.ofPattern("dd.MM"))}–${end.format(DateTimeFormatter.ofPattern("dd.MM"))}",
                )
            }.sortedBy { it.key }
        }
        TrendGranularity.MONTH -> {
            val months = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
            sorted.groupBy { d ->
                val date = LocalDate.ofEpochDay(d.day)
                date.year * 100L + date.monthValue
            }.map { (ym, scores) ->
                val year = (ym / 100).toInt()
                val month = (ym % 100).toInt()
                val sum = scores.sumOf { it.score }
                TrendChartBucket(
                    key = ym,
                    score = sum,
                    label = months.getOrElse(month - 1) { "?" },
                    detail = "${months.getOrElse(month - 1) { "?" }} $year",
                )
            }.sortedBy { it.key }
        }
    }
}

@Composable
fun ReadableTrendChart(
    dailyPoints: List<DailyScore>,
    modifier: Modifier = Modifier,
) {
    if (dailyPoints.isEmpty()) return
    val dayCount = dailyPoints.size
    var granularity by remember(dayCount) { mutableStateOf(defaultTrendGranularity(dayCount)) }
    val buckets = remember(dailyPoints, granularity) { aggregateTrendBuckets(dailyPoints, granularity) }
    val showGranularityPicker = dayCount > 21

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showGranularityPicker) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AtmosphereBrand.NeutralSoft)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TrendGranularity.entries.forEach { g ->
                    val enabled = when (g) {
                        TrendGranularity.DAY -> dayCount <= 45
                        else -> true
                    }
                    val selected = granularity == g
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                when {
                                    selected -> AtmosphereBrand.SkyMid
                                    enabled -> Color.Transparent
                                    else -> Color.Transparent
                                },
                            )
                            .then(
                                if (enabled) {
                                    Modifier.clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = { granularity = g },
                                    )
                                } else Modifier,
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            g.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = when {
                                !enabled -> AtmosphereBrand.InkMuted.copy(alpha = 0.4f)
                                selected -> Color.White
                                else -> AtmosphereBrand.InkSoft
                            },
                        )
                    }
                }
            }
            Text(
                when (granularity) {
                    TrendGranularity.DAY -> "$dayCount дней — прокрутите график влево/вправо"
                    TrendGranularity.WEEK -> "Сумма баллов за каждую неделю"
                    TrendGranularity.MONTH -> "Сумма баллов за каждый месяц"
                },
                fontSize = 11.sp,
                color = AtmosphereBrand.InkMuted,
                lineHeight = 15.sp,
            )
        }

        val barWidth = when (granularity) {
            TrendGranularity.DAY -> if (buckets.size <= 21) 28.dp else 44.dp
            TrendGranularity.WEEK -> 52.dp
            TrendGranularity.MONTH -> 52.dp
        }
        TrendBarChartScrollable(
            buckets = buckets,
            chartHeight = 260.dp,
            barWidth = barWidth,
        )
    }
}

@Composable
private fun TrendBarChartScrollable(
    buckets: List<TrendChartBucket>,
    chartHeight: Dp,
    barWidth: Dp,
) {
    val maxAbs = max(buckets.maxOfOrNull { abs(it.score) } ?: 1, 1)
    val yAxisW = 44.dp
    val plotH = chartHeight - 48.dp

    Row(Modifier.fillMaxWidth().height(chartHeight)) {
        TrendYAxis(maxAbs = maxAbs, height = plotH, modifier = Modifier.width(yAxisW))
        Row(
            Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            buckets.forEach { bucket ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(barWidth),
                ) {
                    Text(
                        atmosphereFormatSigned(bucket.score),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = atmosphereScoreColor(bucket.score),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    TrendBar(
                        score = bucket.score,
                        maxAbs = maxAbs,
                        modifier = Modifier
                            .width(barWidth - 8.dp)
                            .height(plotH),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        bucket.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AtmosphereBrand.Ink,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        lineHeight = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendLineChartScrollable(
    buckets: List<TrendChartBucket>,
    chartHeight: Dp,
    minPointWidth: Dp,
) {
    val values = buckets.map { it.score.toFloat() }
    val maxV = maxOf(values.maxOrNull() ?: 1f, abs(values.minOrNull() ?: 0f), 1f)
    val minV = -maxV
    val yAxisW = 44.dp
    val plotH = chartHeight - 36.dp
    val contentW = minPointWidth * buckets.size.coerceAtLeast(1)

    Column {
        Row(Modifier.height(chartHeight)) {
            TrendYAxis(maxAbs = maxV.roundToInt(), height = plotH, modifier = Modifier.width(yAxisW))
            Box(
                Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
            ) {
                Column(Modifier.width(contentW)) {
                    Canvas(Modifier.width(contentW).height(plotH)) {
                        val padT = 12f
                        val padB = 12f
                        val w = size.width
                        val h = size.height - padT - padB
                        fun yFor(v: Float) = padT + h * ((maxV - v) / (maxV - minV))
                        val zeroY = yFor(0f)
                        drawLine(AtmosphereBrand.Rule, Offset(0f, padT), Offset(w, padT), 1f)
                        drawLine(AtmosphereBrand.Rule, Offset(0f, padT + h), Offset(w, padT + h), 1f)
                        drawLine(AtmosphereBrand.InkMuted.copy(alpha = 0.5f), Offset(0f, zeroY), Offset(w, zeroY), 2f)
                        if (values.size >= 2) {
                            val step = w / (values.size - 1)
                            val path = Path()
                            values.forEachIndexed { i, v ->
                                val x = step * i
                                val y = yFor(v)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, AtmosphereBrand.SkyDeep, style = Stroke(3f, cap = StrokeCap.Round))
                            values.forEachIndexed { i, v ->
                                drawCircle(
                                    atmosphereScoreColor(v.roundToInt()),
                                    5f,
                                    Offset(step * i, yFor(v)),
                                )
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        buckets.forEach { b ->
                            Text(
                                b.label,
                                fontSize = 11.sp,
                                color = AtmosphereBrand.InkSoft,
                                modifier = Modifier.width(minPointWidth),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendYAxis(maxAbs: Int, height: Dp, modifier: Modifier = Modifier) {
    val ticks = listOf(maxAbs, maxAbs / 2, 0, -maxAbs / 2, -maxAbs)
    Column(
        modifier.then(Modifier.height(height)),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ticks.forEach { t ->
            Text(
                atmosphereFormatSigned(t),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AtmosphereBrand.InkSoft,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun TrendBar(score: Int, maxAbs: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val mid = h / 2f
        val barH = (abs(score).toFloat() / maxAbs) * (h / 2f - 8f)
        drawLine(AtmosphereBrand.Rule, Offset(0f, mid), Offset(w, mid), 2f)
        val top = if (score >= 0) mid - barH else mid
        drawRoundRect(
            color = atmosphereScoreColor(score),
            topLeft = Offset(0f, top),
            size = Size(w, maxOf(barH, 6f)),
            cornerRadius = CornerRadius(6f, 6f),
        )
    }
}
