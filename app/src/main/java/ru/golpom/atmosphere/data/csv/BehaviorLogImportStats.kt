/**
 * Сводка вставки импортированных строк лога (дубликаты по уникальному индексу Room, отсутствующие ученики).
 * Data-слой.
 */
package ru.golpom.atmosphere.data.csv

data class BehaviorLogImportStats(
    val inserted: Int,
    val skippedUnknownStudent: Int,
    val skippedDuplicate: Int,
    val studentsCreated: Int = 0,
)
