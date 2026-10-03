/**
 * Человекочитаемые подписи типов поведения.
 * Domain-слой.
 */
package ru.golpom.atmosphere.domain

/**
 * Подпись типа поведения. Основные типы берутся из [BehaviorPreset],
 * поэтому подписи нельзя дублировать здесь — они разъезжаются с пресетами.
 */
fun behaviorTypeLabelRu(key: String): String =
    BehaviorPreset.entries.find { it.behaviorType == key }?.labelRu
        ?: when (key) {
            // Типы из диалога массовой оценки в уведомлениях (GradePromptDialog).
            "praise" -> "Поощрение"
            "misconduct" -> "Нарушение дисциплины"
            else -> key
        }
