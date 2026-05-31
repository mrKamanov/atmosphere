/**
 * Проекция Room-запроса: DayOfWeekScore.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.model

data class DayOfWeekScore(
    val dayOfWeek: Int,
    val totalScore: Int,
    val entryCount: Int,
)
