/**
 * Перевод классов в новый учебный год: 5-А → 6-А, выпускные классы выпускаются.
 * Domain-слой: чистая логика построения плана, без Room и Android.
 */
package ru.golpom.atmosphere.domain.schoolyear

import ru.golpom.atmosphere.domain.student.StudentIdentity

/** Что сделать с учениками выпускных классов. */
enum class GraduateAction {
    /** Ученики уходят в архив, история отметок сохраняется. */
    ARCHIVE,

    /** Ученики удаляются вместе с историей отметок. */
    DELETE,
}

/**
 * Перевод одного класса на ступень выше.
 *
 * [studentSourceIds] и [scheduleSourceIds] — исходные написания класса в БД
 * (например «9-Л» у учеников и «9 Л» в расписании из «Моя школа»): все они
 * переезжают в единый [toClassId].
 */
data class ClassPromotion(
    val fromClassId: String,
    val toClassId: String,
    val studentCount: Int,
    val studentSourceIds: Set<String>,
    val scheduleSourceIds: Set<String>,
)

data class ClassGraduation(
    val classId: String,
    val studentCount: Int,
    val studentSourceIds: Set<String>,
    val scheduleSourceIds: Set<String>,
)

data class SchoolYearRolloverPlan(
    val graduationGrade: Int,
    val graduations: List<ClassGraduation>,
    /** Отсортированы от старших к младшим — именно в таком порядке их надо применять. */
    val promotions: List<ClassPromotion>,
    /** Классы с учениками, номер которых не распознан: остаются без изменений. */
    val unrecognized: List<String>,
) {
    val isEmpty: Boolean get() = graduations.isEmpty() && promotions.isEmpty()
    val promotedStudents: Int get() = promotions.sumOf { it.studentCount }
    val graduatingStudents: Int get() = graduations.sumOf { it.studentCount }
}

object SchoolYearRollover {
    /** В российской школе последний класс — 11-й. */
    const val DEFAULT_GRADUATION_GRADE = 11

    /** Варианты, которые можно выбрать в диалоге (основная школа заканчивается 9-м). */
    val GRADUATION_GRADE_OPTIONS = listOf(9, 11)

    private val gradeRegex = Regex("^(\\d{1,2})-(.+)$")

    data class ParsedClass(val grade: Int, val suffix: String)

    /** «5-А», «5А», «5 а» → (5, «А»). Всё, что не начинается с номера 1–12, — null. */
    fun parse(classId: String): ParsedClass? {
        val normalized = StudentIdentity.normalizeClassId(classId)
        val m = gradeRegex.find(normalized) ?: return null
        val grade = m.groupValues[1].toIntOrNull() ?: return null
        if (grade !in 1..12) return null
        return ParsedClass(grade, m.groupValues[2])
    }

    /**
     * @param studentCountByClass активные ученики учителя по классам (как записано в БД).
     * @param scheduleClassIds классы, встречающиеся в расписании (как записано в БД).
     */
    fun buildPlan(
        studentCountByClass: Map<String, Int>,
        scheduleClassIds: Set<String>,
        graduationGrade: Int = DEFAULT_GRADUATION_GRADE,
    ): SchoolYearRolloverPlan {
        class Group(val parsed: ParsedClass) {
            var students = 0
            val studentSources = linkedSetOf<String>()
            val scheduleSources = linkedSetOf<String>()
        }

        val groups = linkedMapOf<String, Group>()
        val unrecognized = sortedSetOf<String>()

        fun groupFor(rawId: String): Group? {
            val parsed = parse(rawId) ?: return null
            val key = "${parsed.grade}-${parsed.suffix}"
            return groups.getOrPut(key) { Group(parsed) }
        }

        for ((rawId, count) in studentCountByClass) {
            if (count <= 0) continue
            val g = groupFor(rawId)
            if (g == null) {
                unrecognized += rawId
                continue
            }
            g.students += count
            g.studentSources += rawId
        }
        for (rawId in scheduleClassIds) {
            groupFor(rawId)?.scheduleSources?.add(rawId)
        }

        val graduations = mutableListOf<ClassGraduation>()
        val promotions = mutableListOf<ClassPromotion>()
        val ordered = groups.entries.sortedWith(
            compareByDescending<Map.Entry<String, Group>> { it.value.parsed.grade }
                .thenBy { it.value.parsed.suffix },
        )
        for ((key, g) in ordered) {
            if (g.parsed.grade >= graduationGrade) {
                graduations += ClassGraduation(
                    classId = key,
                    studentCount = g.students,
                    studentSourceIds = g.studentSources,
                    scheduleSourceIds = g.scheduleSources,
                )
            } else {
                promotions += ClassPromotion(
                    fromClassId = key,
                    toClassId = "${g.parsed.grade + 1}-${g.parsed.suffix}",
                    studentCount = g.students,
                    studentSourceIds = g.studentSources,
                    scheduleSourceIds = g.scheduleSources,
                )
            }
        }

        return SchoolYearRolloverPlan(
            graduationGrade = graduationGrade,
            graduations = graduations,
            promotions = promotions,
            unrecognized = unrecognized.toList(),
        )
    }
}
