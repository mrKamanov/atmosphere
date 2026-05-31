/**
 * Строка CSV при импорте отметок поведения.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.csv

import ru.golpom.atmosphere.data.local.entity.BehaviorLogEntity

data class BehaviorLogImportRow(
    val log: BehaviorLogEntity,
    val firstName: String?,
    val lastName: String?,
)
