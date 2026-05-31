/**
 * Имена колонок CSV лога поведения (§4 ТЗ); порядок совпадает с полями выгрузки без `id`.
 */
package ru.golpom.atmosphere.data.csv

object BehaviorLogCsvColumns {
    const val TIMESTAMP = "timestamp"
    const val STUDENT_ID = "student_id"
    const val CLASS_ID = "class_id"
    const val FIRST_NAME = "first_name"
    const val LAST_NAME = "last_name"
    const val SUBJECT_KEY = "subject_key"
    const val BEHAVIOR_TYPE = "behavior_type"
    const val SCORE_IMPACT = "score_impact"
    const val COMMENT = "comment"

    /** Текущая выгрузка: с ФИО для слияния на устройстве завуча. */
    val HEADER: Array<String> = arrayOf(
        TIMESTAMP,
        STUDENT_ID,
        CLASS_ID,
        FIRST_NAME,
        LAST_NAME,
        SUBJECT_KEY,
        BEHAVIOR_TYPE,
        SCORE_IMPACT,
        COMMENT,
    )

    /** Старые файлы без колонок имён. */
    val HEADER_LEGACY: Array<String> = arrayOf(
        TIMESTAMP,
        STUDENT_ID,
        CLASS_ID,
        SUBJECT_KEY,
        BEHAVIOR_TYPE,
        SCORE_IMPACT,
        COMMENT,
    )
}
