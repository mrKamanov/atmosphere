/**
 * Проекция Room-запроса: StudentSubjectScore.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.model

data class StudentSubjectScore(
    val studentId: String,
    val totalScore: Int,
)
