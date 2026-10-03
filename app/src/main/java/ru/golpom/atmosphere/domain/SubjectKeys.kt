/**
 * Стабильные ключи предметов для логов и навигации (совпадают с `subject_key` в БД).
 * Domain-слой.
 */
package ru.golpom.atmosphere.domain

object SubjectKeys {
    const val GEOGRAPHY = "География"
    const val MATHEMATICS = "Математика"

    /**
     * Отметка, сделанная вне урока (перемена, коридор): предмет в записи не заполняется,
     * класс остаётся, чтобы отчёт завучу собрал такие нарушения по классу.
     */
    const val OUTSIDE_LESSON = "Вне урока"
}
