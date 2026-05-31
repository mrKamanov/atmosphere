/**
 * Модели аналитики ученика для UI завуча.
 * UI-слой.
 */
package ru.golpom.atmosphere.ui.deputy

import ru.golpom.atmosphere.data.local.entity.StudentEntity
import ru.golpom.atmosphere.data.local.model.DailyScore

data class StudentAnalytics(
    val student: StudentEntity,
    val periodLabel: String,
    val totalScore: Int,
    val eventCount: Int,
    val praiseCount: Int,
    val violationCount: Int,
    val dailyScores: List<DailyScore>,
    val insight: StudentInsightReport,
)

data class StudentInsightReport(
    val talkPoints: List<String>,
    val mainConcern: String?,
    val mainStrength: String?,
    val weekdayPatterns: List<StudentWeekdayPattern>,
    val weeklyLoads: List<StudentWeekLoad>,
    val subjectProblems: List<StudentSubjectInsight>,
    val subjectStrengths: List<StudentSubjectInsight>,
    val dayDetails: Map<Long, StudentDayDetail>,
)

data class StudentWeekLoad(
    val weekStartEpochDay: Long,
    val weekLabel: String,
    val violationCount: Int,
    val praiseCount: Int,
    val totalScore: Int,
)

data class StudentWeekdayPattern(
    val dayOfWeek: Int,
    val dayLabel: String,
    val dayLabelLong: String,
    val lateCount: Int,
    val unpreparedCount: Int,
    val disruptionCount: Int,
    val gadgetCount: Int,
    val fightCount: Int,
    val positiveCount: Int,
    val totalNegative: Int,
) {
    val hasActivity: Boolean = totalNegative > 0 || positiveCount > 0
}

data class StudentSubjectInsight(
    val subjectTitle: String,
    val subjectContext: String?,
    val summary: String,
    val eventCount: Int,
)

data class StudentDayDetail(
    val epochDay: Long,
    val dateLabel: String,
    val weekdayLabel: String,
    val summary: String,
    val events: List<StudentDayEvent>,
)

data class StudentDayEvent(
    val timeLabel: String,
    val subjectTitle: String,
    val subjectContext: String?,
    val behaviorLabel: String,
    val scoreImpact: Int,
    val isPositive: Boolean,
    /** Для отчёта по классу — фамилия и имя ученика. */
    val studentName: String? = null,
)

/** Сводка инсайтов (ученик или класс). */
typealias InsightReport = StudentInsightReport

data class ClassAnalytics(
    val classId: String,
    val periodLabel: String,
    val totalScore: Int,
    val eventCount: Int,
    val praiseCount: Int,
    val violationCount: Int,
    val activeStudentCount: Int,
    val dailyScores: List<DailyScore>,
    val insight: InsightReport,
    val praiseStudents: List<StudentRankingRow>,
    val watchStudents: List<StudentRankingRow>,
)
