/**
 * Русские названия дней недели для UI и отчётов.
 * Domain-слой.
 */
package ru.golpom.atmosphere.domain

/**
 * Склонение дней недели для текстов отчётов (Java DayOfWeek: 1 = пн … 7 = вс).
 */
object WeekdayLabelsRu {

    private data class Forms(
        val short: String,
        val nominative: String,
        /** «в понедельник», «во вторник», «в среду» */
        val inDay: String,
        /** «по понедельникам», «по вторникам» */
        val onDays: String,
        val genitive: String,
    )

    private val byDow = mapOf(
        1 to Forms("Пн", "понедельник", "в понедельник", "по понедельникам", "понедельника"),
        2 to Forms("Вт", "вторник", "во вторник", "по вторникам", "вторника"),
        3 to Forms("Ср", "среда", "в среду", "по средам", "среды"),
        4 to Forms("Чт", "четверг", "в четверг", "по четвергам", "четверга"),
        5 to Forms("Пт", "пятница", "в пятницу", "по пятницам", "пятницы"),
        6 to Forms("Сб", "суббота", "в субботу", "по субботам", "субботы"),
        7 to Forms("Вс", "воскресенье", "в воскресенье", "по воскресеньям", "воскресенья"),
    )

    fun short(dow: Int): String = byDow[dow]?.short ?: "?"

    fun nominative(dow: Int): String = byDow[dow]?.nominative ?: "?"

    fun inDay(dow: Int): String = byDow[dow]?.inDay ?: "?"

    fun onDays(dow: Int): String = byDow[dow]?.onDays ?: "?"

    fun genitive(dow: Int): String = byDow[dow]?.genitive ?: "?"

    fun titled(dow: Int): String = nominative(dow).replaceFirstChar { it.titlecase() }
}
