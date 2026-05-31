/**
 * Роль интерфейса приложения (учитель / завуч / ещё не выбрано).
 * Domain-слой; хранится в DataStore через строковое представление.
 */
package ru.golpom.atmosphere.domain

enum class AppRole {
    NOT_SET,
    TEACHER,
    DEPUTY,
}
