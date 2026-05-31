/**
 * Проекция Room-запроса: StudentSubjectBreakdown.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.model

data class StudentSubjectBreakdown(
    val subjectKey: String,
    val totalScore: Int,
    val positiveScore: Int,
    val negativeScore: Int,
    val positiveCount: Int,
    val negativeCount: Int,
)

data class BehaviorTypeCount(
    val behaviorType: String,
    val count: Int,
)

data class StudentTotalsInRange(
    val totalScore: Int,
    val totalPositive: Int,
    val totalNegative: Int,
    val eventCount: Int,
)
