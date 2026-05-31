/**
 * Проекция Room-запроса: StudentSubjectCell.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.model

data class StudentSubjectCell(
    val studentId: String,
    val firstName: String,
    val lastName: String,
    val subjectKey: String,
    val score: Int,
)
