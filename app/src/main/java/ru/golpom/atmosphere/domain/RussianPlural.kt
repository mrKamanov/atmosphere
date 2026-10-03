/**
 * Склонение числительных по правилам русского языка.
 * Domain-слой.
 */
package ru.golpom.atmosphere.domain

/**
 * Выбор формы существительного по числу: 1 ученик / 2 ученика / 5 учеников.
 *
 * Формы передаются в порядке: одна (1, 21, 31...), несколько (2-4, 22-24...), много (0, 5-20, 100...).
 */
fun pluralRu(count: Int, one: String, few: String, many: String): String = when {
    count % 100 in 11..14 -> many
    count % 10 == 1 -> one
    count % 10 in 2..4 -> few
    else -> many
}

/** То же, но вместе с числом: `3 отметки`. */
fun countRu(count: Int, one: String, few: String, many: String): String =
    "$count ${pluralRu(count, one, few, many)}"

/** Форма счёта для учеников: `1 ученик`, `2 ученика`, `5 учеников`. */
fun studentsRu(count: Int): String = countRu(count, "ученик", "ученика", "учеников")

/** То же для [Long]-счётчиков из SQL-агрегатов. */
fun studentsRu(count: Long): String = countRu(count.toInt(), "ученик", "ученика", "учеников")

/** Форма счёта учеников после предлога «у»: `у 1 ученика`, `у 3 учеников`. */
fun studentsGenitiveRu(count: Int): String = countRu(count, "ученика", "учеников", "учеников")
