/**
 * HTML-отчёт по школе для завуча.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.export.deputy

import ru.golpom.atmosphere.ui.deputy.BriefTone
import ru.golpom.atmosphere.ui.deputy.DeputyDashboardSnapshot
import ru.golpom.atmosphere.ui.deputy.ExecutiveBriefPoint
import ru.golpom.atmosphere.ui.deputy.HeatmapMatrix
import ru.golpom.atmosphere.ui.deputy.StudentRankingRow

object DeputySchoolReportHtml {

    fun render(snapshot: DeputyDashboardSnapshot, periodLabel: String, mode: ReportRenderMode = ReportRenderMode.SCREEN): String {
        val summary = snapshot.summary
        val body = buildString {
            append(
                DeputyAnalyticsHtml.titlePage(
                    level = "Школа",
                    headline = "Аналитика школы",
                    periodLabel = periodLabel,
                    details = listOf(
                        "${summary.classCount} классов",
                        "${summary.studentCount} учеников",
                    ),
                ),
            )
            append("""<div class="content">""")
            append(DeputyAnalyticsHtml.sectionIntro("Школа", periodLabel))

            append("""<div class="two-col">""")
            append(DeputyAnalyticsCharts.compositionDonut(summary.positiveSharePct, summary.negativeSharePct))
            append("""<div><div class="stats">""")
            append(DeputyAnalyticsHtml.statBlock("Баланс", DeputyAnalyticsHtml.formatSigned(summary.totalScore)))
            append(DeputyAnalyticsHtml.statBlock("Отметок", "${summary.totalEvents}"))
            append(DeputyAnalyticsHtml.statBlock("Классов", "${summary.classCount}"))
            append(DeputyAnalyticsHtml.statBlock("Учеников", "${summary.studentCount}"))
            append("</div>")
            append("""<div class="stats">""")
            append(DeputyAnalyticsHtml.statBlock("Индекс", "${summary.disciplineIndexPct}%"))
            summary.scoreDelta?.let { delta ->
                val cls = if (delta >= 0) "pos" else "neg"
                append(DeputyAnalyticsHtml.statBlock("К прошл. пер.", DeputyAnalyticsHtml.formatSigned(delta), cls))
            }
            append("</div></div></div>")

            if (snapshot.trend.points.size >= 2) {
                append("<h2>Динамика периода</h2>")
                append(DeputyAnalyticsCharts.dailyBalanceBars(snapshot.trend.points, mode))
            }

            if (snapshot.brief.isNotEmpty()) {
                append("<h2>Тезисы для педсовета</h2>")
                snapshot.brief.forEach { append(briefCard(it)) }
            }

            if (snapshot.classRanking.isNotEmpty()) {
                append("<h2>Рейтинг классов</h2>")
                append(DeputyAnalyticsCharts.classRankingBars(snapshot.classRanking))
            }

            if (snapshot.parallels.size >= 2) {
                append("<h2>Сравнение параллелей</h2>")
                append(DeputyAnalyticsCharts.parallelComparisonPanel(snapshot.parallels))
            }

            if (snapshot.weekdayPattern.isNotEmpty()) {
                append("<h2>Ритм недели</h2>")
                append(DeputyAnalyticsCharts.weekdayBars(snapshot.weekdayPattern))
            }

            if (snapshot.heatmap.classIds.isNotEmpty()) {
                append("<h2>По классам и дням</h2>")
                append(DeputyAnalyticsHtml.heatmapLegend())
                append(schoolHeatmap(snapshot.heatmap))
            }

            if (snapshot.praiseList.isNotEmpty()) {
                append("<h2>Кого поощрить</h2>")
                append(studentRankingTable(snapshot.praiseList))
            }
            if (snapshot.watchList.isNotEmpty()) {
                append("<h2>Кому уделить внимание</h2>")
                append(studentRankingTable(snapshot.watchList))
            }
            append("</div>")
        }
        return DeputyAnalyticsHtml.document("Школа · $periodLabel", body, mode)
    }

    fun fileName(periodLabel: String): String {
        val part = DeputyAnalyticsHtml.safeFilePart(periodLabel.replace(' ', '_'))
        return "Атмосфера_школа_$part.html"
    }

    private fun briefCard(point: ExecutiveBriefPoint): String {
        val tone = when (point.tone) {
            BriefTone.POSITIVE -> "pos"
            BriefTone.NEGATIVE -> "neg"
            BriefTone.NEUTRAL -> "neu"
        }
        return DeputyAnalyticsCharts.insightCard(
            "${point.number}. ${point.headline} · ${point.metric}",
            point.detail,
            tone,
        )
    }

    private fun schoolHeatmap(matrix: HeatmapMatrix): String {
        val colLabels = matrix.dayLabels + "Итого"
        return DeputyAnalyticsHtml.rowColumnHeatmap(
            rowLabels = matrix.classIds,
            colLabels = colLabels,
            cellScore = { classId, col ->
                if (col == "Итого") {
                    matrix.rowTotals[classId] ?: 0
                } else {
                    val idx = matrix.dayLabels.indexOf(col)
                    if (idx < 0) 0 else matrix.cells[classId to matrix.dayKeys[idx]] ?: 0
                }
            },
            rowHeader = "Класс",
        )
    }

    private fun studentRankingTable(rows: List<StudentRankingRow>): String {
        val sb = StringBuilder("""<table><tr><th>#</th><th>Ученик</th><th>Класс</th><th>Баланс</th></tr>""")
        rows.take(10).forEach { row ->
            val cls = if (row.score >= 0) "pos" else "neg"
            sb.append(
                "<tr><td>${row.rank}</td><td>${DeputyAnalyticsHtml.escape(row.name)}</td>" +
                    "<td>${DeputyAnalyticsHtml.escape(row.classId)}</td>" +
                    """<td class="$cls">${DeputyAnalyticsHtml.formatSigned(row.score)}</td></tr>""",
            )
        }
        sb.append("</table>")
        return sb.toString()
    }
}
