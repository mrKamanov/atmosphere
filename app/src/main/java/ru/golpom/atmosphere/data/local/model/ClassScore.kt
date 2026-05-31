/**
 * Проекция Room-запроса: ClassScore.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.model

data class ClassScore(
    val classId: String,
    val score: Int,
)
