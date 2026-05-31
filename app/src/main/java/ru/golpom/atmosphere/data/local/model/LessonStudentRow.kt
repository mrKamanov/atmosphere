/**
 * Строка выборки для экрана урока: ученик и суммарный балл за календарный день по предмету.
 * Data-слой; имена полей совпадают с псевдонимами колонок в SQL Room-запросе логов урока.
 */
package ru.golpom.atmosphere.data.local.model

data class LessonStudentRow(
    val studentId: String,
    val firstName: String,
    val lastName: String,
    val balance: Int,
)
