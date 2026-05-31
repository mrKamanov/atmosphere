/**
 * Человекочитаемые подписи типов поведения.
 * Domain-слой.
 */
package ru.golpom.atmosphere.domain

fun behaviorTypeLabelRu(key: String): String =
    BehaviorPreset.entries.find { it.behaviorType == key }?.labelRu
        ?: when (key) {
            "active_work" -> "Старается"
            "active_help" -> "Помощь классу"
            "focus" -> "Прогресс"
            "exemplary_behavior" -> "Примерное поведение"
            "disruption" -> "Срыв дисциплины"
            "gadget" -> "Гаджет на уроке"
            "late" -> "Опоздание"
            "unprepared" -> "Не готов к уроку"
            "fight" -> "Драка"
            "profanity" -> "Ненормативная лексика"
            else -> key
        }
