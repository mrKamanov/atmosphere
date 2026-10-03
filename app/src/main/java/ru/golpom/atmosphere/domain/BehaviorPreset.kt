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
    UNPREPARED("unprepared", -1, "Без подготовки"),
    FIGHT("fight", -1, "Драка"),
    PROFANITY("profanity", -1, "Ненормативная лексика"),
    CHITCHAT("chitchat", -1, "Разговаривает"),
    INTERRUPTS("interrupts", -1, "Перебивает"),
    CHEATING("cheating", -1, "Списывает"),
    FORGOT_GEAR("forgot_gear", -1, "Забыл(а)"),
    NO_UNIFORM("no_uniform", -1, "Без формы"),
    PROPERTY_DAMAGE("property_damage", -1, "Портит имущество"),
}

/**
 * Типы, для которых в аналитике завучa нет отдельного пункта:
 * они собираются в сводное замечание «Замечания по поведению».
 */
val RoutineBehaviorTypes: List<String> = listOf(
    BehaviorPreset.CHITCHAT,
    BehaviorPreset.INTERRUPTS,
    BehaviorPreset.CHEATING,
    BehaviorPreset.FORGOT_GEAR,
    BehaviorPreset.NO_UNIFORM,
    BehaviorPreset.PROPERTY_DAMAGE,
).map { it.behaviorType }
