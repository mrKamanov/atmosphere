/**
 * Разбор имени файла отчёта учителя.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.export

import ru.golpom.atmosphere.data.preferences.UserPreferencesRepository

/**
 * Разбор legacy-имени CSV: `{scope}_{period}_{фамилия}_{id12}_{дата}.csv`
 */
object TeacherReportFileNameParser {

    data class Parsed(
        val teacherLastName: String,
        val teacherProfileShortId: String,
    )

    fun parse(fileName: String): Parsed? {
        val base = fileName.substringBeforeLast('.').ifBlank { fileName }
        val parts = base.split('_')
        if (parts.size < 3) return null
        val datePart = parts.lastOrNull()?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
        val shortId = if (datePart != null && parts.size >= 2) {
            parts[parts.size - 2].takeIf { it.length == UserPreferencesRepository.TEACHER_PROFILE_SHORT_ID_LEN }
        } else {
            null
        }
        val teacherIndex = when {
            shortId != null && parts.size >= 3 -> parts.size - 3
            else -> parts.lastIndex
        }
        val teacher = parts.getOrNull(teacherIndex)?.takeIf { it.isNotBlank() } ?: return null
        return Parsed(
            teacherLastName = teacher,
            teacherProfileShortId = shortId ?: "unknown",
        )
    }
}
