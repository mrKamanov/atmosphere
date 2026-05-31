/**
 * Имя файла экспорта лога учителя: `[classId]_[subjectKey]_[фамилия]_[teacherId12]_[дата].csv`.
 * Data-слой; небезопасные для ФС символы заменяются на `_`.
 */
package ru.golpom.atmosphere.data.csv

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TeacherBehaviorExportFileName {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val unsafeChars = Regex("[\\\\/:*?\"<>|\\r\\n]")

    fun build(
        classId: String,
        subjectKey: String,
        teacherLastName: String,
        teacherProfileShortId: String,
        exportDate: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    ): String = buildForExport(
        scopeTag = sanitizeSegment(classId),
        periodTag = sanitizeSegment(subjectKey),
        teacherLastName = teacherLastName,
        teacherProfileShortId = teacherProfileShortId,
        exportDate = exportDate,
    )

    fun buildForExport(
        scopeTag: String,
        periodTag: String,
        teacherLastName: String,
        teacherProfileShortId: String,
        exportDate: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    ): String {
        val scope = sanitizeSegment(scopeTag)
        val period = sanitizeSegment(periodTag)
        val teacher = sanitizeSegment(teacherLastName.ifBlank { "Учитель" })
        val id = sanitizeSegment(teacherProfileShortId.ifBlank { "id" })
        val date = exportDate.format(dateFormatter)
        return "${scope}_${period}_${teacher}_${id}_$date.csv"
    }

    /** Нейтральное имя для мессенджеров и почты — без фамилии и меток класса. */
    fun buildNeutralReportName(
        teacherProfileShortId: String,
        exportDate: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    ): String {
        val id = sanitizeSegment(teacherProfileShortId.ifBlank { "id" })
        val date = exportDate.format(dateFormatter)
        return "atmosphere_${id}_$date.atmo"
    }

    fun buildDetailedReportName(
        scopeTag: String,
        periodTag: String,
        teacherLastName: String,
        teacherProfileShortId: String,
        exportDate: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    ): String =
        buildForExport(scopeTag, periodTag, teacherLastName, teacherProfileShortId, exportDate)
            .removeSuffix(".csv") + ".atmo"

    private fun sanitizeSegment(raw: String): String =
        raw.replace(unsafeChars, "_").trim().ifEmpty { "export" }
}
