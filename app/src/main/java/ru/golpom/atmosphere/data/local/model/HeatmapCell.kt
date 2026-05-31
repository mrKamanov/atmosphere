/**
 * Проекция Room-запроса: HeatmapCell.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.model

data class HeatmapCell(
    val classId: String,
    val dayOfWeek: Long,
    val score: Int,
)
