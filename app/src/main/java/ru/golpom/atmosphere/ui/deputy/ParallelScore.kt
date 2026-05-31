/**
 * Сравнение параллелей по суммарному баллу.
 * UI-слой.
 */
package ru.golpom.atmosphere.ui.deputy

data class ParallelScore(
    val parallel: String,
    val totalScore: Int = 0,
    val totalPositive: Int = 0,
    val totalNegative: Int = 0,
    val classCount: Int = 0,
    val studentCount: Int = 0,
)
