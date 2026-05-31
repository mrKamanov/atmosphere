/**
 * Проекция Room-запроса: DailyScore.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.model

data class DailyScore(
    val day: Long,
    val score: Int,
)
