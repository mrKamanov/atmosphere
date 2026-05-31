/**
 * HTML-отчёт по классу для завуча.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.export.deputy

import ru.golpom.atmosphere.data.local.model.StudentSubjectCell
import ru.golpom.atmosphere.ui.deputy.ClassAnalytics
import ru.golpom.atmosphere.ui.deputy.StudentRankingRow
import ru.golpom.atmosphere.domain.WeekdayLabelsRu

object DeputyClassReportHtml {

    fun render(
        analytics: ClassAnalytics,
        heatmap: List<StudentSubjectCell>,
        subjectLabels: Map<String, String>,
        mode: ReportRenderMode = ReportRenderMode.SCREEN,
    ): String {
        val title = "Класс ${analytics.classId}"
        val meta = "${analytics.activeStudentCount} учеников · ${analytics.periodLabel}"
        val body = buildString {
            append(
                DeputyAnalyticsHtml.titlePage(
                    level = "Класс",
                    headline = analytics.classId,
                    periodLabel = analytics.periodLabel,
                    details = listOf("${analytics.activeStudentCount} учеников"),
                ),
            )
            append("""<div class="content">""")
            append(DeputyAnalyticsHtml.sectionIntro("Класс ${analytics.classId}", meta))

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
                append("<h2>О чём говорить на классном часе</h2>")
                append(DeputyAnalyticsCharts.talkPointsList(insight.talkPoints))
            }
            insight.mainStrength?.let {
                append(DeputyAnalyticsCharts.insightCard("Сильная сторона коллектива", it, "pos"))
            }
            insight.mainConcern?.let {
                append(DeputyAnalyticsCharts.insightCard("Зона внимания", it, "neg"))
            }

            if (analytics.praiseStudents.isNotEmpty()) {
                append("<h2>Кого поощрить</h2>")
                append(rankingTable(analytics.praiseStudents))
            }
            if (analytics.watchStudents.isNotEmpty()) {
                append("<h2>Кому уделить внимание</h2>")
                append(rankingTable(analytics.watchStudents))
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

            if (heatmap.isNotEmpty() && subjectLabels.isNotEmpty()) {
                val subjects = subjectLabels.keys.sorted()
                val students = heatmap
                    .map { it.studentId to "${it.lastName} ${it.firstName}" }
                    .distinct()
                    .sortedBy { it.second }
                val scoreMap = heatmap.associate { (it.studentId to it.subjectKey) to it.score }
                append(
                    DeputyAnalyticsHtml.chunkedStudentSubjectHeatmap(
                        students = students,
                        subjectKeys = subjects,
                        subjectLabels = subjectLabels,
                        scores = scoreMap,
                    ),
                )
            }
            append("</div>")
        }
        return DeputyAnalyticsHtml.document(title, body, mode)
    }

    fun fileName(analytics: ClassAnalytics): String {
        val part = DeputyAnalyticsHtml.safeFilePart(analytics.classId)
        return "Атмосфера_класс_$part.html"
    }

    private fun pct(part: Int, total: Int): Int =
        if (total > 0) (part * 100f / total).toInt() else 0

    private fun rankingTable(rows: List<StudentRankingRow>): String {
        val sb = StringBuilder("""<table><tr><th>#</th><th>Ученик</th><th>Баланс</th></tr>""")
        rows.take(10).forEach { row ->
            val cls = if (row.score >= 0) "pos" else "neg"
            sb.append(
                "<tr><td>${row.rank}</td><td>${DeputyAnalyticsHtml.escape(row.name)}</td>" +
                    """<td class="$cls">${DeputyAnalyticsHtml.formatSigned(row.score)}</td></tr>""",
            )
        }
        sb.append("</table>")
        return sb.toString()
    }
}
