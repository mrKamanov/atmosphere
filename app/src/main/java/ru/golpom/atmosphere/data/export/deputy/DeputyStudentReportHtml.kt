/**
 * HTML-отчёт по одному ученику для завуча.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.export.deputy

import ru.golpom.atmosphere.ui.deputy.StudentAnalytics
import ru.golpom.atmosphere.ui.deputy.StudentSubjectInsight
import ru.golpom.atmosphere.domain.WeekdayLabelsRu

object DeputyStudentReportHtml {

    fun render(analytics: StudentAnalytics, mode: ReportRenderMode = ReportRenderMode.SCREEN): String {
        val s = analytics.student
        val title = "${s.lastName} ${s.firstName}"
        val meta = "Класс ${s.classId} · ${analytics.periodLabel}"
        val body = buildString {
            append(
                DeputyAnalyticsHtml.titlePage(
                    level = "Ученик",
                    headline = title,
                    periodLabel = analytics.periodLabel,
                    details = listOf("Класс ${s.classId}"),
                ),
            )
            append("""<div class="content">""")
            append(DeputyAnalyticsHtml.sectionIntro(title, meta))

            append("""<div class="two-col">""")
            append(DeputyAnalyticsCharts.compositionDonut(
                positivePct = pct(analytics.praiseCount, analytics.eventCount),
                negativePct = pct(analytics.violationCount, analytics.eventCount),
            ))
            append("""<div><div class="stats">""")
            append(DeputyAnalyticsHtml.statBlock("Баланс", DeputyAnalyticsHtml.formatSigned(analytics.totalScore)))
            append(DeputyAnalyticsHtml.statBlock("Отметок", "${analytics.eventCount}"))
            append(DeputyAnalyticsHtml.statBlock("Поощрений", "${analytics.praiseCount}", "pos"))
            append(DeputyAnalyticsHtml.statBlock("Нарушений", "${analytics.violationCount}", "neg"))
            append("</div></div></div>")

            if (analytics.dailyScores.size >= 2) {
                append("<h2>Динамика периода</h2>")
                append(DeputyAnalyticsCharts.dailyBalanceBars(analytics.dailyScores, mode))
            }

            val insight = analytics.insight
            if (insight.talkPoints.isNotEmpty()) {
                append("<h2>О чём поговорить с учеником и родителями</h2>")
                append(DeputyAnalyticsCharts.talkPointsList(insight.talkPoints))
            }
            insight.mainStrength?.let {
                append(DeputyAnalyticsCharts.insightCard("Сильная сторона", it, "pos"))
            }
            insight.mainConcern?.let {
                append(DeputyAnalyticsCharts.insightCard("Зона внимания", it, "neg"))
            }

            if (insight.subjectStrengths.isNotEmpty()) {
                append("<h2>Где ученик силён</h2>")
                insight.subjectStrengths.forEach { append(subjectCard(it, positive = true)) }
            }
            if (insight.subjectProblems.isNotEmpty()) {
                append("<h2>Где нужна поддержка</h2>")
                insight.subjectProblems.forEach { append(subjectCard(it, positive = false)) }
            }
            if (insight.weeklyLoads.isNotEmpty()) {
                append("<h2>Нагрузка по неделям</h2>")
                append(DeputyAnalyticsCharts.weeklyLoadChart(insight.weeklyLoads, mode))
            }
            val activeDays = insight.weekdayPatterns.filter { it.hasActivity }
            if (activeDays.isNotEmpty()) {
                append("<h2>По дням недели</h2>")
                append(
                    DeputyAnalyticsCharts.weekdayPatternBars(
                        labels = activeDays.map { WeekdayLabelsRu.titled(it.dayOfWeek) },
                        positiveCounts = activeDays.map { it.positiveCount },
                        negativeCounts = activeDays.map { it.totalNegative },
                    ),
                )
            }
            val dayDetails = insight.dayDetails.values.sortedByDescending { it.epochDay }.take(15)
            if (dayDetails.isNotEmpty()) {
                append("<h2>Последние значимые дни</h2>")
                dayDetails.forEach { day ->
                    append("""<div class="card card-accent"><div class="card-title">${DeputyAnalyticsHtml.escape(day.dateLabel)} · ${DeputyAnalyticsHtml.escape(day.weekdayLabel)}</div>""")
                    append("""<div class="card-body">${DeputyAnalyticsHtml.escape(day.summary)}</div>""")
                    if (day.events.isNotEmpty()) {
                        append("<ul>")
                        day.events.take(8).forEach { ev ->
                            val sign = if (ev.isPositive) "+" else ""
                            val cls = if (ev.isPositive) "pos" else "neg"
                            append(
                                """<li><span class="$cls">${DeputyAnalyticsHtml.escape(ev.timeLabel)} · ${DeputyAnalyticsHtml.escape(ev.subjectTitle)} · """ +
                                    "${DeputyAnalyticsHtml.escape(ev.behaviorLabel)} ($sign${ev.scoreImpact})</span></li>",
                            )
                        }
                        append("</ul>")
                    }
                    append("</div>")
                }
            }
            append("</div>")
        }
        return DeputyAnalyticsHtml.document("Ученик $title", body, mode)
    }

    fun fileName(analytics: StudentAnalytics): String {
        val s = analytics.student
        val part = DeputyAnalyticsHtml.safeFilePart("${s.lastName}_${s.firstName}")
        return "Атмосфера_ученик_$part.html"
    }

    private fun pct(part: Int, total: Int): Int =
        if (total > 0) (part * 100f / total).toInt() else 0

    private fun subjectCard(item: StudentSubjectInsight, positive: Boolean): String {
        val cls = if (positive) "pos" else "neg"
        val ctx = item.subjectContext?.let { """<div class="card-sub">${DeputyAnalyticsHtml.escape(it)}</div>""" } ?: ""
        return """<div class="card card-accent" style="border-left-color:${if (positive) "#3D9A62" else "#C95555"}"><div class="card-title">${DeputyAnalyticsHtml.escape(item.subjectTitle)}</div>$ctx<div class="card-body $cls">${DeputyAnalyticsHtml.escape(item.summary)}</div></div>"""
    }
}
