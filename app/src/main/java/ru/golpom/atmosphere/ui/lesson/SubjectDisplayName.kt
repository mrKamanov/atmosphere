/**
 * Человекочитаемые названия предмета по ключу из расписания или журнала.
 */
package ru.golpom.atmosphere.ui.lesson

import ru.golpom.atmosphere.domain.SubjectKeys

data class SubjectLabel(
    val title: String,
    /** Например «параллели 5–6» или «класс 9-А» — если в ключе был «хвост». */
    val context: String? = null,
) {
    val fullLine: String
        get() = if (context.isNullOrBlank()) title else "$title · $context"
}

private val englishKeys = mapOf(
    "math" to "Математика",
    "mathematics" to "Математика",
    "physics" to "Физика",
    "chemistry" to "Химия",
    "biology" to "Биология",
    "history" to "История",
    "geography" to "География",
    "literature" to "Литература",
    "english" to "Английский язык",
    "russian" to "Русский язык",
    "physical_education" to "Физкультура",
    "informatics" to "Информатика",
    "art" to "ИЗО",
    "music" to "Музыка",
    "technology" to "Технология",
    "social" to "Обществознание",
    "algebra" to "Алгебра",
    "geometry" to "Геометрия",
)

/** Диапазон параллелей: «История 5х-6», «История 5-6». */
private val parallelRangeRegex =
    Regex("""^(.+?)\s+(\d+)\s*[хxXХ]?\s*[-–]\s*(\d+)$""", RegexOption.IGNORE_CASE)

/** Класс с буквой: «Математика 9 А», «Физика 10Б». */
private val classLetterRegex =
    Regex("""^(.+?)\s+(\d+)\s*([А-Яа-яA-Za-z])$""")

fun parseSubjectLabel(subjectKey: String): SubjectLabel {
    val raw = subjectKey.trim().replace('_', ' ').replace(Regex("\\s+"), " ")
    if (raw.isBlank()) return SubjectLabel("—")

    parallelRangeRegex.find(raw)?.let { m ->
        return SubjectLabel(
            title = formatSubjectTitle(m.groupValues[1].trim()),
            context = "параллели ${m.groupValues[2]}–${m.groupValues[3]}",
        )
    }
    classLetterRegex.find(raw)?.let { m ->
        val letter = m.groupValues[3].uppercase()
        return SubjectLabel(
            title = formatSubjectTitle(m.groupValues[1].trim()),
            context = "класс ${m.groupValues[2]}-$letter",
        )
    }

    return SubjectLabel(title = formatSubjectTitle(raw))
}

fun subjectDisplayName(subjectKey: String): String = parseSubjectLabel(subjectKey).fullLine

/** Предложный падеж после «на» (на уроке …): «на алгебре», «на русском языке». */
fun subjectTitlePrepositional(title: String): String {
    val t = title.trim()
    if (t.isBlank()) return t
    prepositionalByTitle[t]?.let { return it }
    val lower = t.lowercase()
    return when {
        lower.endsWith("ский язык") -> {
            val stem = lower.removeSuffix("ский язык")
            "${stem}ском языке"
        }
        lower.endsWith("ный язык") -> {
            val stem = lower.removeSuffix("ный язык")
            "${stem}ном языке"
        }
        lower.endsWith("ия") -> lower.dropLast(2) + "ии"
        lower.endsWith("ика") -> lower.dropLast(1) + "е"
        lower.endsWith("ь") -> lower.dropLast(1) + "и"
        lower.endsWith("й") -> lower.dropLast(1) + "е"
        else -> lower
    }
}

/** Родительный падеж: «на уроках алгебры», «по математике» в связках. */
fun subjectTitleGenitive(title: String): String {
    val t = title.trim()
    if (t.isBlank()) return t
    genitiveByTitle[t]?.let { return it }
    val lower = t.lowercase()
    return when {
        lower.endsWith("ский язык") -> {
            val stem = lower.removeSuffix("ский язык")
            "${stem}ского языка"
        }
        lower.endsWith("ный язык") -> {
            val stem = lower.removeSuffix("ный язык")
            "${stem}ного языка"
        }
        lower.endsWith("ия") -> lower.dropLast(2) + "ии"
        lower.endsWith("ика") -> lower.dropLast(1) + "и"
        lower.endsWith("ь") -> lower.dropLast(1) + "и"
        else -> lower
    }
}

private val prepositionalByTitle = mapOf(
    "Алгебра" to "алгебре",
    "Геометрия" to "геометрии",
    "Математика" to "математике",
    "Физика" to "физике",
    "Химия" to "химии",
    "Биология" to "биологии",
    "География" to "географии",
    "История" to "истории",
    "Литература" to "литературе",
    "Русский язык" to "русском языке",
    "Иностранный язык" to "иностранном языке",
    "Английский язык" to "английском языке",
    "Информатика" to "информатике",
    "Обществознание" to "обществознании",
    "Физкультура" to "физкультуре",
    "Музыка" to "музыке",
    "ИЗО" to "ИЗО",
    "Технология" to "технологии",
    "ОДНКНР" to "ОДНКНР",
    "Разговоры о важном" to "разговорах о важном",
)

private val genitiveByTitle = mapOf(
    "Алгебра" to "алгебры",
    "Геометрия" to "геометрии",
    "Математика" to "математики",
    "Физика" to "физики",
    "Химия" to "химии",
    "Биология" to "биологии",
    "География" to "географии",
    "История" to "истории",
    "Литература" to "литературы",
    "Русский язык" to "русского языка",
    "Иностранный язык" to "иностранного языка",
    "Английский язык" to "английского языка",
    "Информатика" to "информатики",
    "Обществознание" to "обществознания",
    "Физкультура" to "физкультуры",
    "Музыка" to "музыки",
    "ИЗО" to "ИЗО",
    "Технология" to "технологии",
)

private fun formatSubjectTitle(name: String): String {
    val key = name.trim()
    englishKeys[key.lowercase()]?.let { return it }
    when (key) {
        SubjectKeys.GEOGRAPHY, "География" -> return "География"
        SubjectKeys.MATHEMATICS, "Математика" -> return "Математика"
        "Русский язык", "русский язык" -> return "Русский язык"
        "Английский язык" -> return "Английский язык"
        "Обществознание" -> return "Обществознание"
        "Физкультура", "Физ-ра" -> return "Физкультура"
    }
    if (key.length == 1) return key.uppercase()
    return key.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
