/**
 * Результат слияния списка учеников в Room (новые строки / уже существующие по `student_id`).
 * Data-слой.
 */
package ru.golpom.atmosphere.data.csv

data class StudentImportSummary(
    val inserted: Int,
    val skippedExisting: Int,
    val parseWarnings: Int,
)
