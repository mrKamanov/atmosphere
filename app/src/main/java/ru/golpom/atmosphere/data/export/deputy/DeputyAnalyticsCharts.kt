/**
 * SVG-графики и визуализации для HTML-отчётов завуча.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.export.deputy

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import ru.golpom.atmosphere.data.local.model.DailyScore
import ru.golpom.atmosphere.ui.deputy.ClassRankingRow
import ru.golpom.atmosphere.ui.deputy.ParallelGroupRow
import ru.golpom.atmosphere.ui.deputy.StudentWeekLoad
import ru.golpom.atmosphere.ui.deputy.WeekdayPatternRow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object DeputyAnalyticsCharts {

    private data class BalanceBucket(val label: String, val score: Int)

    /** Столбцов в одной строке графика для PDF (ширина A4). */
    private const val PDF_BARS_PER_ROW = 10

    private val dayLabelFormat = DateTimeFormatter.ofPattern("dd.MM")
    private val monthLabels = listOf(
        "янв", "фев", "мар", "апр", "май", "июн",
        "июл", "авг", "сен", "окт", "ноя", "дек",
    )

    fun compositionDonut(positivePct: Int, negativePct: Int): String {
        val neutral = (100 - positivePct - negativePct).coerceAtLeast(0)
        val r = 54.0
        val cx = 70.0
        val cy = 70.0
        val circum = 2 * Math.PI * r
        var offset = 0.0
        fun arc(pct: Int, color: String): String {
            if (pct <= 0) return ""
            val len = circum * pct / 100.0
            val dash = "$len ${circum - len}"
            val svg = """
                <circle cx="$cx" cy="$cy" r="$r" fill="none" stroke="$color" stroke-width="18"
                  stroke-dasharray="$dash" stroke-dashoffset="$offset"
                  transform="rotate(-90 $cx $cy)"/>
            """.trimIndent()
            offset -= len
            return svg
        }
        return """
            <div class="chart-box">
              <svg viewBox="0 0 140 140" class="chart-svg donut" role="img" aria-label="Состав отметок">
                <circle cx="$cx" cy="$cy" r="$r" fill="none" stroke="#F0F3F2" stroke-width="18"/>
                ${arc(positivePct, "#3D9A62")}
                ${arc(negativePct, "#C95555")}
                ${arc(neutral, "#E0EAE6")}
                <text x="$cx" y="${cy - 4}" text-anchor="middle" class="donut-main">$positivePct%</text>
                <text x="$cx" y="${cy + 14}" text-anchor="middle" class="donut-sub">поощрений</text>
              </svg>
              <div class="legend">
                <span class="legend-item"><i class="dot pos"></i>Поощрения $positivePct%</span>
                <span class="legend-item"><i class="dot neg"></i>Нарушения $negativePct%</span>
              </div>
            </div>
        """.trimIndent()
    }

    /** Столбчатая динамика баланса; длинные периоды агрегируются по неделям или месяцам. */
    fun dailyBalanceBars(points: List<DailyScore>, mode: ReportRenderMode = ReportRenderMode.SCREEN): String {
        if (points.isEmpty()) return ""
        val buckets = when (mode) {
            ReportRenderMode.SCREEN -> aggregatePoints(points)
            ReportRenderMode.PDF -> aggregatePointsForPdf(points)
        }
        if (buckets.isEmpty()) return ""
        val caption = when (mode) {
            ReportRenderMode.PDF -> if (points.size > 14) "По месяцам" else "По дням"
            ReportRenderMode.SCREEN -> when {
                points.size > 120 -> "По месяцам"
                points.size > 31 -> "По неделям"
                else -> "По дням"
            }
        }
        return when (mode) {
            ReportRenderMode.PDF -> htmlBalanceBarChartPdf(buckets, caption)
            ReportRenderMode.SCREEN -> {
                val slotW = when {
                    buckets.size > 26 -> 36
                    buckets.size > 12 -> 40
                    else -> 44
                }
                htmlBalanceBarChart(buckets, caption, slotW)
            }
        }
    }

    fun weeklyLoadChart(weeks: List<StudentWeekLoad>, mode: ReportRenderMode = ReportRenderMode.SCREEN): String {
        if (weeks.isEmpty()) return ""
        val maxViol = max(weeks.maxOf { it.violationCount }, 1)
        val slotW = 36
        val trackH = 72
        val slots = weeks.joinToString("") { weeklyLoadSlot(it, slotW, trackH, maxViol) }
        val totalW = weeks.size * (slotW + 6) + 8
        return when (mode) {
            ReportRenderMode.PDF -> {
                val rowsHtml = weeks.chunked(PDF_BARS_PER_ROW).joinToString("") { row ->
                    val rowSlots = row.joinToString("") { weeklyLoadSlot(it, slotW, trackH, maxViol) }
                    val rowW = row.size * (slotW + 6) + 8
                    """<div class="bar-chart bar-chart-pdf-row" style="width:${rowW}px">$rowSlots</div>"""
                }
                """
                    <div class="chart-box">
                      <div class="chart-caption">Высота столбца — нарушения, над столбцом — поощрения</div>
                      <div class="bar-chart-pdf">$rowsHtml</div>
                    </div>
                """.trimIndent()
            }
            ReportRenderMode.SCREEN -> """
            <div class="chart-box">
              <div class="chart-caption">Высота столбца — нарушения, над столбцом — поощрения</div>
              <div class="bar-chart-scroll">
                <div class="bar-chart" style="width:${totalW}px">$slots</div>
              </div>
            </div>
        """.trimIndent()
        }
    }

    private fun weeklyLoadSlot(week: StudentWeekLoad, slotW: Int, trackH: Int, maxViol: Int): String {
        val pct = if (week.violationCount > 0) {
            (week.violationCount.toFloat() / maxViol * 100f).roundToInt().coerceAtLeast(8)
        } else {
            0
        }
        val praise = if (week.praiseCount > 0) "+${week.praiseCount}" else "&nbsp;"
        val violIn = if (week.violationCount > 0) "${week.violationCount}" else ""
        val violInner = if (violIn.isNotEmpty()) {
            """<span style="color:#fff;font-size:9px;font-weight:600">$violIn</span>"""
        } else ""
        return """
            <div class="bar-slot" style="width:${slotW}px">
              <div class="bar-top-label pos">$praise</div>
              <div class="bar-track" style="height:${trackH}px">
                <div class="bar-fill neg" style="height:${pct}%;display:flex;align-items:center;justify-content:center">$violInner</div>
              </div>
              <div class="bar-x-label">${DeputyAnalyticsHtml.escape(week.weekLabel)}</div>
            </div>
            """.trimIndent()
    }

    fun weekdayBars(rows: List<WeekdayPatternRow>): String {
        if (rows.isEmpty()) return ""
        val maxAbs = max(rows.maxOf { abs(it.totalScore) }, 1)
        val barW = 36
        val gap = 10
        val chartH = 130
        val baseY = 100.0
        val maxBarH = 72.0
        val totalW = rows.size * (barW + gap) + 40
        val bars = rows.mapIndexed { i, row ->
            val x = 20 + i * (barW + gap)
            val h = (abs(row.totalScore).toDouble() / maxAbs * maxBarH)
                .coerceAtLeast(if (row.totalScore != 0) 6.0 else 2.0)
            val color = if (row.totalScore >= 0) "#3D9A62" else "#C95555"
            val y = baseY - h
            val label = DeputyAnalyticsHtml.formatSigned(row.totalScore)
            """
              <rect x="$x" y="$y" width="$barW" height="$h" rx="4" fill="$color"/>
              <text x="${x + barW / 2}" y="${y - 5}" text-anchor="middle" class="bar-value">$label</text>
              <text x="${x + barW / 2}" y="${baseY + 18}" text-anchor="middle" class="bar-label">${DeputyAnalyticsHtml.escape(row.dayLabel)}</text>
            """.trimIndent()
        }.joinToString("\n")
        return """
            <div class="chart-box">
              <svg viewBox="0 0 $totalW $chartH" class="chart-svg" preserveAspectRatio="xMidYMid meet">
                <line x1="10" y1="$baseY" x2="${totalW - 10}" y2="$baseY" stroke="#E0EAE6" stroke-width="1"/>
                $bars
              </svg>
            </div>
        """.trimIndent()
    }

    fun weekdayPatternBars(
        labels: List<String>,
        positiveCounts: List<Int>,
        negativeCounts: List<Int>,
    ): String {
        if (labels.isEmpty()) return ""
        val maxVal = max(max(positiveCounts.maxOrNull() ?: 0, negativeCounts.maxOrNull() ?: 0), 1)
        val barW = 14
        val groupW = barW * 2 + 6
        val gap = 14
        val chartH = 120
        val baseY = 92.0
        val maxBarH = 58.0
        val totalW = labels.size * (groupW + gap) + 40
        val bars = labels.mapIndexed { i, label ->
            val pos = positiveCounts.getOrElse(i) { 0 }
            val neg = negativeCounts.getOrElse(i) { 0 }
            val x = 20 + i * (groupW + gap)
            val posH = if (pos > 0) (pos.toDouble() / maxVal * maxBarH).roundToInt().coerceAtLeast(6) else 0
            val negH = if (neg > 0) (neg.toDouble() / maxVal * maxBarH).roundToInt().coerceAtLeast(6) else 0
            val posLabel = if (pos > 0) {
                """<text x="${x + barW / 2.0}" y="${baseY - posH - 4}" text-anchor="middle" class="bar-value">$pos</text>"""
            } else ""
            val negLabel = if (neg > 0) {
                """<text x="${x + barW + 6 + barW / 2.0}" y="${baseY - negH - 4}" text-anchor="middle" class="bar-value">$neg</text>"""
            } else ""
            """
              $posLabel
              <rect x="$x" y="${baseY - posH}" width="$barW" height="$posH" rx="3" fill="#3D9A62"/>
              $negLabel
              <rect x="${x + barW + 6}" y="${baseY - negH}" width="$barW" height="$negH" rx="3" fill="#C95555"/>
              <text x="${x + groupW / 2.0}" y="${baseY + 18}" text-anchor="middle" class="bar-label">${DeputyAnalyticsHtml.escape(label)}</text>
            """.trimIndent()
        }.joinToString("\n")
        return """
            <div class="chart-box">
              <div class="chart-caption">Зелёный — поощрения, красный — нарушения</div>
              <svg viewBox="0 0 $totalW $chartH" class="chart-svg" preserveAspectRatio="xMidYMid meet">
                <line x1="10" y1="$baseY" x2="${totalW - 10}" y2="$baseY" stroke="#E0EAE6"/>
                $bars
              </svg>
            </div>
        """.trimIndent()
    }

    fun classRankingBars(rows: List<ClassRankingRow>, maxItems: Int = 12): String {
        val slice = rows.take(maxItems)
        if (slice.isEmpty()) return ""
        val maxAbs = max(slice.maxOf { abs(it.score) }, 1)
        val rowH = 26
        val labelW = 52
        val barArea = 420
        val h = slice.size * rowH + 16
        val bars = slice.mapIndexed { i, row ->
            val y = 8 + i * rowH
            val barLen = (abs(row.score).toDouble() / maxAbs * barArea).roundToInt()
            val color = if (row.score >= 0) "#3D9A62" else "#C95555"
            """
              <text x="0" y="${y + 16}" class="rank-label">${DeputyAnalyticsHtml.escape(row.classId)}</text>
              <rect x="$labelW" y="$y" width="$barLen" height="18" rx="4" fill="$color" opacity="0.9"/>
              <text x="${labelW + barLen + 8}" y="${y + 14}" class="rank-value">${DeputyAnalyticsHtml.formatSigned(row.score)}</text>
            """.trimIndent()
        }.joinToString("\n")
        return """
            <div class="chart-box">
              <svg viewBox="0 0 ${labelW + barArea + 60} $h" class="chart-svg-fixed ranking" preserveAspectRatio="xMinYMid meet" style="width:${labelW + barArea + 60}px">
                $bars
              </svg>
            </div>
        """.trimIndent()
    }

    fun parallelComparisonPanel(groups: List<ParallelGroupRow>): String {
        if (groups.isEmpty()) return ""
        return groups.joinToString("") { group ->
            val maxInGroup = max(group.classes.maxOfOrNull { abs(it.score) } ?: 1, 1)
            val totalCls = if (group.totalScore >= 0) "pos" else "neg"
            val classRows = group.classes.sortedBy { it.classId }.joinToString("") { cs ->
                val pct = (abs(cs.score).toFloat() / maxInGroup * 100f).roundToInt().coerceIn(5, 100)
                val cls = if (cs.score >= 0) "pos" else "neg"
                """
                <div class="parallel-class-row">
                  <span class="parallel-class-id">${DeputyAnalyticsHtml.escape(cs.classId)}</span>
                  <div class="parallel-bar-track"><div class="parallel-bar-fill $cls" style="width:$pct%"></div></div>
                  <span class="parallel-class-score $cls">${DeputyAnalyticsHtml.formatSigned(cs.score)}</span>
                </div>
                """.trimIndent()
            }
            """
            <div class="parallel-card">
              <div class="parallel-card-head">
                <span class="parallel-card-title">${DeputyAnalyticsHtml.escape(group.parallel)} классы</span>
                <span class="parallel-card-total $totalCls">${DeputyAnalyticsHtml.formatSigned(group.totalScore)}</span>
              </div>
              $classRows
            </div>
            """.trimIndent()
        }
    }

    fun insightCard(title: String, body: String, tone: String): String {
        val border = when (tone) {
            "pos" -> "#3D9A62"
            "neg" -> "#C95555"
            else -> "#5BA68A"
        }
        return """
            <div class="card card-accent" style="border-left-color:$border">
              <div class="card-title">${DeputyAnalyticsHtml.escape(title)}</div>
              <div class="card-body $tone">${DeputyAnalyticsHtml.escape(body)}</div>
            </div>
        """.trimIndent()
    }

    fun talkPointsList(points: List<String>): String {
        if (points.isEmpty()) return ""
        val items = points.joinToString("") { """<li>${DeputyAnalyticsHtml.escape(it)}</li>""" }
        return """<ul class="talk-list">$items</ul>"""
    }

    private fun htmlBalanceBarChart(
        buckets: List<BalanceBucket>,
        caption: String,
        slotW: Int,
    ): String {
        val maxAbs = max(buckets.maxOf { abs(it.score) }, 1)
        val trackH = 100
        val slots = buckets.joinToString("") { balanceBarSlot(it, slotW, trackH, maxAbs) }
        val totalW = buckets.size * (slotW + 6) + 8
        return """
            <div class="chart-box">
              <div class="chart-caption">${DeputyAnalyticsHtml.escape(caption)}</div>
              <div class="bar-chart-scroll">
                <div class="bar-chart" style="width:${totalW}px">$slots</div>
              </div>
            </div>
        """.trimIndent()
    }

    private fun htmlBalanceBarChartPdf(buckets: List<BalanceBucket>, caption: String): String {
        val slotW = when {
            buckets.size > 24 -> 28
            buckets.size > 12 -> 32
            else -> 36
        }
        val maxAbs = max(buckets.maxOf { abs(it.score) }, 1)
        val trackH = 100
        val rowsHtml = buckets.chunked(PDF_BARS_PER_ROW).joinToString("") { row ->
            val rowSlots = row.joinToString("") { balanceBarSlot(it, slotW, trackH, maxAbs) }
            val rowW = row.size * (slotW + 6) + 8
            """<div class="bar-chart bar-chart-pdf-row" style="width:${rowW}px">$rowSlots</div>"""
        }
        return """
            <div class="chart-box">
              <div class="chart-caption">${DeputyAnalyticsHtml.escape(caption)}</div>
              <div class="bar-chart-pdf">$rowsHtml</div>
            </div>
        """.trimIndent()
    }

    private fun balanceBarSlot(bucket: BalanceBucket, slotW: Int, trackH: Int, maxAbs: Int): String {
        val pct = (abs(bucket.score).toFloat() / maxAbs * 100f).roundToInt()
            .coerceAtLeast(if (bucket.score != 0) 6 else 0)
        val cls = when {
            bucket.score > 0 -> "pos"
            bucket.score < 0 -> "neg"
            else -> "neu"
        }
        val topLabel = if (bucket.score != 0) DeputyAnalyticsHtml.formatSigned(bucket.score) else "&nbsp;"
        return """
            <div class="bar-slot" style="width:${slotW}px">
              <div class="bar-top-label $cls">$topLabel</div>
              <div class="bar-track" style="height:${trackH}px">
                <div class="bar-fill $cls" style="height:${pct}%"></div>
              </div>
              <div class="bar-x-label">${axisLabelHtml(bucket.label)}</div>
            </div>
            """.trimIndent()
    }

    private fun axisLabelHtml(label: String): String {
        val parts = label.trim().split(Regex("\\s+"), limit = 2)
        return if (parts.size == 2 && parts[1].matches(Regex("\\d{4}"))) {
            "${DeputyAnalyticsHtml.escape(parts[0])}<br>${DeputyAnalyticsHtml.escape(parts[1])}"
        } else {
            DeputyAnalyticsHtml.escape(label)
        }
    }

    private fun aggregatePoints(points: List<DailyScore>): List<BalanceBucket> = when {
        points.size > 120 -> groupByMonth(points)
        points.size > 31 -> groupByWeek(points)
        else -> points.sortedBy { it.day }.map {
            BalanceBucket(dayLabelFormat.format(LocalDate.ofEpochDay(it.day)), it.score)
        }
    }

    /** Для PDF: без недель — только дни или месяцы, чтобы график помещался на лист. */
    private fun aggregatePointsForPdf(points: List<DailyScore>): List<BalanceBucket> = when {
        points.size > 14 -> groupByMonth(points)
        else -> points.sortedBy { it.day }.map {
            BalanceBucket(dayLabelFormat.format(LocalDate.ofEpochDay(it.day)), it.score)
        }
    }

    private fun groupByWeek(points: List<DailyScore>): List<BalanceBucket> =
        points.groupBy { LocalDate.ofEpochDay(it.day).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
            .toSortedMap()
            .map { (monday, list) ->
                BalanceBucket(dayLabelFormat.format(monday), list.sumOf { it.score })
            }

    private fun groupByMonth(points: List<DailyScore>): List<BalanceBucket> =
        points.groupBy { LocalDate.ofEpochDay(it.day).withDayOfMonth(1) }
            .toSortedMap()
            .map { (first, list) ->
                val month = monthLabels.getOrElse(first.monthValue - 1) { "?" }
                BalanceBucket("$month ${first.year}", list.sumOf { it.score })
            }
}
