/**
 * Производные метрики и UI-модели для бизнес-dashboard завуча (расчёт из [DeputyStats]).
 * UI-слой.
 */
package ru.golpom.atmosphere.ui.deputy

import ru.golpom.atmosphere.data.local.model.ClassScore
import ru.golpom.atmosphere.data.local.model.DailyScore
import ru.golpom.atmosphere.data.local.model.DayOfWeekScore
import ru.golpom.atmosphere.data.local.model.HeatmapCell
import ru.golpom.atmosphere.domain.WeekdayLabelsRu
import kotlin.math.abs
import kotlin.math.roundToInt

data class DeputyDashboardSnapshot(
    val stats: DeputyStats,
    val previous: DeputyStats?,
) {
    val summary: ExecutiveSummary = stats.toExecutiveSummary(previous)
    val trend: TrendAnalytics = stats.toTrendAnalytics()
    val classRanking: List<ClassRankingRow> = stats.toClassRanking()
    val weekdayPattern: List<WeekdayPatternRow> = stats.toWeekdayPattern()
    val heatmap: HeatmapMatrix = stats.toHeatmapMatrix()
    val praiseList: List<StudentRankingRow> = stats.toStudentRanking(positive = true)
    val watchList: List<StudentRankingRow> = stats.toStudentRanking(positive = false)
    val parallels: List<ParallelGroupRow> = stats.toParallelGroups()
    val brief: List<ExecutiveBriefPoint> = stats.toExecutiveBrief()
}

data class ExecutiveSummary(
    val totalScore: Int,
    val totalPositive: Int,
    val totalNegative: Int,
    val totalEvents: Int,
    val positiveSharePct: Int,
    val negativeSharePct: Int,
    val activeStudents: Int,
    val classCount: Int,
    val studentCount: Long,
    val avgScorePerActiveStudent: Float,
    val disciplineIndexPct: Int,
    val scoreDelta: Int?,
    val eventsDelta: Int?,
)

data class TrendAnalytics(
    val points: List<DailyScore>,
    val total: Int,
    val average: Float,
    val best: DailyScore?,
    val worst: DailyScore?,
    val positiveDays: Int,
    val negativeDays: Int,
)

data class ClassRankingRow(
    val classId: String,
    val score: Int,
    val shareOfTotalPct: Float,
    val rank: Int,
    val medianScore: Int,
    val maxScore: Int,
)

data class WeekdayPatternRow(
    val dayLabel: String,
    val dayOfWeek: Int,
    val totalScore: Int,
    val entryCount: Int,
    val avgPerEntry: Float,
)

data class HeatmapMatrix(
    val dayLabels: List<String>,
    val classIds: List<String>,
    val cells: Map<Pair<String, Int>, Int>,
    val rowTotals: Map<String, Int>,
    val colTotals: Map<Int, Int>,
    val maxAbs: Int,
    val dayKeys: List<Int> = SCHOOL_WEEKDAYS.map { it.first },
)

data class StudentRankingRow(
    val studentId: String,
    val name: String,
    val classId: String,
    val score: Int,
    val shareOfListPct: Int,
    val rank: Int,
)

data class ParallelGroupRow(
    val parallel: String,
    val classes: List<ClassScore>,
    val totalScore: Int,
)

data class ExecutiveBriefPoint(
    val number: Int,
    val headline: String,
    val detail: String,
    val metric: String,
    val tone: BriefTone,
)

enum class BriefTone { POSITIVE, NEGATIVE, NEUTRAL }

private fun DeputyStats.toExecutiveSummary(previous: DeputyStats?): ExecutiveSummary {
    val events = totalPositive + abs(totalNegative)
    val posPct = if (events > 0) (totalPositive * 100f / events).roundToInt() else 0
    val negPct = if (events > 0) (abs(totalNegative) * 100f / events).roundToInt() else 0
    val avg = if (activeStudentCount > 0) totalScore.toFloat() / activeStudentCount else 0f
    val index = if (events > 0) ((totalPositive - abs(totalNegative)) * 100f / events).roundToInt() else 0
    val prevEvents = previous?.let { it.totalPositive + abs(it.totalNegative) }
    return ExecutiveSummary(
        totalScore = totalScore,
        totalPositive = totalPositive,
        totalNegative = totalNegative,
        totalEvents = events,
        positiveSharePct = posPct,
        negativeSharePct = negPct,
        activeStudents = activeStudentCount,
        classCount = classCount,
        studentCount = studentCount,
        avgScorePerActiveStudent = avg,
        disciplineIndexPct = index,
        scoreDelta = previous?.let { totalScore - it.totalScore },
        eventsDelta = previous?.let { events - (it.totalPositive + abs(it.totalNegative)) },
    )
}

private fun DeputyStats.toTrendAnalytics(): TrendAnalytics {
    val sorted = dailyScores.sortedBy { it.day }
    val total = sorted.sumOf { it.score }
    val avg = if (sorted.isNotEmpty()) total.toFloat() / sorted.size else 0f
    return TrendAnalytics(
        points = sorted,
        total = total,
        average = avg,
        best = sorted.maxByOrNull { it.score },
        worst = sorted.minByOrNull { it.score },
        positiveDays = sorted.count { it.score > 0 },
        negativeDays = sorted.count { it.score < 0 },
    )
}

private fun DeputyStats.toClassRanking(): List<ClassRankingRow> {
    val sorted = classScores.sortedByDescending { it.score }
    val totalAbs = sorted.sumOf { abs(it.score) }.coerceAtLeast(1)
    val median = sorted.map { it.score }.sorted().let { list ->
        if (list.isEmpty()) 0 else list[list.size / 2]
    }
    val max = sorted.maxOfOrNull { abs(it.score) } ?: 1
    return sorted.mapIndexed { idx, cs ->
        ClassRankingRow(
            classId = cs.classId,
            score = cs.score,
            shareOfTotalPct = abs(cs.score) * 100f / totalAbs,
            rank = idx + 1,
            medianScore = median,
            maxScore = max,
        )
    }
}

/** Учебные дни: пн–пт (1–5), без выходных. */
val SCHOOL_WEEKDAYS: List<Pair<Int, String>> = listOf(
    1 to "Пн",
    2 to "Вт",
    3 to "Ср",
    4 to "Чт",
    5 to "Пт",
)

private fun DeputyStats.toWeekdayPattern(): List<WeekdayPatternRow> {
    val labelByDay = SCHOOL_WEEKDAYS.toMap()
    return fatigueData
        .filter { it.dayOfWeek in 1..5 }
        .sortedBy { it.dayOfWeek }
        .map { f ->
            WeekdayPatternRow(
                dayLabel = labelByDay[f.dayOfWeek] ?: "?",
                dayOfWeek = f.dayOfWeek,
                totalScore = f.totalScore,
                entryCount = f.entryCount,
                avgPerEntry = if (f.entryCount > 0) f.totalScore.toFloat() / f.entryCount else 0f,
            )
        }
}

private fun DeputyStats.toHeatmapMatrix(): HeatmapMatrix {
    val classes = heatmapData.map { it.classId }.distinct().sorted()
    val cellMap = heatmapData
        .filter { it.dayOfWeek in 1..5 }
        .associate { (it.classId to it.dayOfWeek.toInt()) to it.score }
    val rowTotals = classes.associateWith { cid ->
        SCHOOL_WEEKDAYS.sumOf { (dow, _) -> cellMap[cid to dow] ?: 0 }
    }
    val colTotals = SCHOOL_WEEKDAYS.associate { (dow, _) ->
        dow to classes.sumOf { cid -> cellMap[cid to dow] ?: 0 }
    }
    val maxAbs = maxOf(heatmapData.maxOfOrNull { abs(it.score) } ?: 1, 1)
    return HeatmapMatrix(
        dayLabels = SCHOOL_WEEKDAYS.map { it.second },
        classIds = classes,
        cells = cellMap,
        rowTotals = rowTotals,
        colTotals = colTotals,
        maxAbs = maxAbs,
        dayKeys = SCHOOL_WEEKDAYS.map { it.first },
    )
}

private fun DeputyStats.toStudentRanking(positive: Boolean): List<StudentRankingRow> {
    val source = if (positive) topPositiveStudents else topNegativeStudents
    val totalAbs = source.sumOf { abs(it.score) }.coerceAtLeast(1)
    return source.mapIndexed { idx, s ->
        StudentRankingRow(
            studentId = s.studentId,
            name = "${s.lastName} ${s.firstName}",
            classId = s.classId,
            score = s.score,
            shareOfListPct = (abs(s.score) * 100f / totalAbs).roundToInt(),
            rank = idx + 1,
        )
    }
}

private fun DeputyStats.toParallelGroups(): List<ParallelGroupRow> {
    fun parallelOf(classId: String) = classId.takeWhile { it.isDigit() }.ifEmpty { classId }
    return classScores
        .groupBy { parallelOf(it.classId) }
        .map { (par, list) ->
            ParallelGroupRow(
                parallel = par,
                classes = list.sortedBy { it.classId },
                totalScore = list.sumOf { it.score },
            )
        }
        .sortedBy { it.parallel.toIntOrNull() ?: Int.MAX_VALUE }
}

private fun DeputyStats.toExecutiveBrief(): List<ExecutiveBriefPoint> {
    val points = mutableListOf<ExecutiveBriefPoint>()
    var n = 1
    val events = totalPositive + abs(totalNegative)
    val negAbs = abs(totalNegative)

    if (events > 0) {
        val posPct = (totalPositive * 100f / events).roundToInt()
        val tone = when {
            posPct >= 60 -> BriefTone.POSITIVE
            posPct < 40 -> BriefTone.NEGATIVE
            else -> BriefTone.NEUTRAL
        }
        points.add(
            ExecutiveBriefPoint(
                n++,
                "Итог за период",
                "Зафиксировано $events отметок: поощрений $totalPositive, нарушений $negAbs. " +
                    "Сводный баланс ${deputyFormatSigned(totalScore)}.",
                deputyFormatSigned(totalScore),
                tone,
            ),
        )
    }

    classScores.maxByOrNull { it.score }?.let { best ->
        points.add(
            ExecutiveBriefPoint(
                n++,
                "Лучший класс",
                "Класс ${best.classId} — наибольший положительный вклад за период.",
                deputyFormatSigned(best.score),
                BriefTone.POSITIVE,
            ),
        )
    }

    classScores.minByOrNull { it.score }?.takeIf { it.score < 0 }?.let { worst ->
        points.add(
            ExecutiveBriefPoint(
                n++,
                "Класс под контролем",
                "Класс ${worst.classId} — запланировать классный час или беседу с коллективом.",
                deputyFormatSigned(worst.score),
                BriefTone.NEGATIVE,
            ),
        )
    }

    topPositiveStudents.firstOrNull()?.let { s ->
        points.add(
            ExecutiveBriefPoint(
                n++,
                "Поощрить публично",
                "${s.lastName} ${s.firstName}, ${s.classId}.",
                deputyFormatSigned(s.score),
                BriefTone.POSITIVE,
            ),
        )
    }

    topNegativeStudents.firstOrNull()?.let { s ->
        points.add(
            ExecutiveBriefPoint(
                n++,
                "Индивидуальная беседа",
                "${s.lastName} ${s.firstName}, ${s.classId} — пригласить родителей при необходимости.",
                deputyFormatSigned(s.score),
                BriefTone.NEGATIVE,
            ),
        )
    }

    fatigueData
        .filter { it.dayOfWeek in 1..5 }
        .maxByOrNull { abs(it.totalScore) }
        ?.let { peak ->
            val dayIn = WeekdayLabelsRu.inDay(peak.dayOfWeek)
            val dayTitle = WeekdayLabelsRu.titled(peak.dayOfWeek)
            points.add(
                ExecutiveBriefPoint(
                    n++,
                    "Напряжённый день недели",
                    "$dayIn больше всего отметок (${peak.entryCount}), баланс ${deputyFormatSigned(peak.totalScore)}.",
                    dayTitle,
                    BriefTone.NEUTRAL,
                ),
            )
        }

    return points
}
