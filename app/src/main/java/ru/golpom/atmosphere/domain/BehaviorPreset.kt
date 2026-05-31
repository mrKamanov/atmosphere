/**
 * Пресеты быстрых отметок на уроке: тип события, влияние на балл, подпись для UI.
 * Domain-слой; соответствует сценарию §5.1 ТЗ.
 */
package ru.golpom.atmosphere.domain

enum class BehaviorPreset(
    val behaviorType: String,
    val scoreImpact: Int,
    val labelRu: String,
) {
    ACTIVE_WORK("active_work", 1, "Старается"),
    CLASS_HELP("active_help", 1, "Помощь классу"),
    FOCUS("focus", 1, "Прогресс"),
    EXEMPLARY_BEHAVIOR("exemplary_behavior", 1, "Примерное поведение"),
    DISRUPTION("disruption", -1, "Срыв дисциплины"),
    GADGET("gadget", -1, "Гаджет на уроке"),
    LATE("late", -1, "Опоздание"),
    UNPREPARED("unprepared", -1, "Не готов"),
    FIGHT("fight", -1, "Драка"),
    PROFANITY("profanity", -1, "Ненормативная лексика"),
}
