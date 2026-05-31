/**
 * Агрегированная статистика периода для dashboard.
 * UI-слой.
 */
package ru.golpom.atmosphere.ui.deputy

import ru.golpom.atmosphere.data.local.model.ClassScore
import ru.golpom.atmosphere.data.local.model.DailyScore
import ru.golpom.atmosphere.data.local.model.DayOfWeekScore
import ru.golpom.atmosphere.data.local.model.HeatmapCell
import ru.golpom.atmosphere.data.local.model.StudentSubjectScore

data class DeputyStats(
    val totalScore: Int = 0,
    val totalPositive: Int = 0,
    val totalNegative: Int = 0,
    val activeStudentCount: Int = 0,
    val classCount: Int = 0,
    val studentCount: Long = 0,
    val classScores: List<ClassScore> = emptyList(),
    val topPositiveStudents: List<StudentWithScore> = emptyList(),
    val topNegativeStudents: List<StudentWithScore> = emptyList(),
    val dailyScores: List<DailyScore> = emptyList(),
    val heatmapData: List<HeatmapCell> = emptyList(),
    val parallelScores: List<ParallelScore> = emptyList(),
    val fatigueData: List<DayOfWeekScore> = emptyList(),
)

data class StudentWithScore(
    val studentId: String,
    val firstName: String,
    val lastName: String,
    val classId: String,
    val score: Int,
)

enum class PeriodType(
    val label: String,
) {
    WEEK("Неделя"),
    MONTH("Месяц"),
    YEAR("Год"),
    CUSTOM("Период"),
}

data class ClassDetailData(
    val totalScore: Int = 0,
    val totalPositive: Int = 0,
    val totalNegative: Int = 0,
    val dailyScores: List<DailyScore> = emptyList(),
    val positiveStudents: List<StudentWithScore> = emptyList(),
    val negativeStudents: List<StudentWithScore> = emptyList(),
)

data class PeriodConfig(
    val type: PeriodType = PeriodType.YEAR,
    val fromDate: Long? = null,
    val toDate: Long? = null,
)
