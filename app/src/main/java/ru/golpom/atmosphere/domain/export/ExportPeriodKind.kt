/**
 * Тип периода для выгрузки данных учителя.
 */
package ru.golpom.atmosphere.domain.export

enum class ExportPeriodKind(val label: String) {
    DAY("День"),
    WEEK("Неделя"),
    MONTH("Месяц"),
    SCHOOL_YEAR("Учебный год"),
}

/**
 * @param useCurrent true — текущий день / неделя / месяц / учебный год до сегодня.
 * @param anchorDateMillis для DAY/WEEK — выбранная дата; для MONTH — любой день месяца;
 *   для SCHOOL_YEAR — дата окончания периода.
 */
data class ExportPeriodSelection(
    val kind: ExportPeriodKind,
    val useCurrent: Boolean = true,
    val anchorDateMillis: Long? = null,
)
