/**
 * Парсер расписания из Excel «Моя школа».
 * Data-слой.
 */
package ru.golpom.atmosphere.data.excel

import org.apache.poi.ss.usermodel.WorkbookFactory
import ru.golpom.atmosphere.data.local.entity.ScheduleEntryEntity
import java.io.InputStream

object MySchoolExcelParser {

    data class MySchoolRow(
        val dayOfWeek: Int,
        val startTimeMinutes: Int,
        val subjectKey: String,
        val classId: String,
    )

    data class MySchoolResult(
        val rows: List<MySchoolRow>,
        val errors: List<String>,
    )

    /**
     * Column mapping for Моя школа export:
     * Mon -> B(time), C(subject)  => col 1, 2
     * Tue -> F(time), G(subject)  => col 5, 6
     * Wed -> J(time), K(subject)  => col 9, 10
     * Thu -> N(time), O(subject)  => col 13, 14
     * Fri -> R(time), S(subject)  => col 17, 18
     * Sat -> V(time), W(subject)  => col 21, 22
     */
    private val dayColumns = listOf(
        1 to 2,    // Mon
        5 to 6,    // Tue
        9 to 10,   // Wed
        13 to 14,  // Thu
        17 to 18,  // Fri
        21 to 22,  // Sat
    )

    private val subjectRegex = Regex("^(.+?)\\s+(\\d{1,2})\\s+([А-ЯЁ])\\s+\\d{4}")

    private fun parseTime(value: String): Int? {
        val cleaned = value.trim().replace("–", "-").replace("—", "-")
        val parts = cleaned.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    private fun cleanSubject(raw: String): String {
        var s = raw.trim()
        if (s.endsWith("внеурочная деятельность", ignoreCase = true)) {
            s = s.substring(0, s.length - "внеурочная деятельность".length).trim()
        }
        return s
    }

    private fun parseSubject(raw: String): Pair<String, String> {
        val match = subjectRegex.find(raw.trim())
        if (match != null) {
            val subject = cleanSubject(match.groupValues[1].trim())
            val grade = match.groupValues[2]
            val letter = match.groupValues[3]
            return subject to "$grade-$letter"
        }
        return cleanSubject(raw.trim()) to ""
    }

    private fun cellString(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        row: Int,
        col: Int,
    ): String {
        val r = sheet.getRow(row) ?: return ""
        val c = r.getCell(col) ?: return ""
        return when (c.cellType) {
            org.apache.poi.ss.usermodel.CellType.STRING -> c.stringCellValue.trim()
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                val v = c.numericCellValue
                if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
            }
            else -> ""
        }
    }

    private fun isProbablyHeader(sheet: org.apache.poi.ss.usermodel.Sheet, row: Int): Boolean {
        val val1 = cellString(sheet, row, 1)
        val val2 = cellString(sheet, row, 2)
        if (val1.contains("время", ignoreCase = true) && val2.contains("предмет", ignoreCase = true)) return true
        if (val1.contains("time", ignoreCase = true) && val2.contains("subject", ignoreCase = true)) return true
        return false
    }

    private fun findDataStartRow(sheet: org.apache.poi.ss.usermodel.Sheet): Int {
        for (i in 0..sheet.lastRowNum) {
            for (dayIdx in 0 until 6) {
                val (timeCol, _) = dayColumns[dayIdx]
                val timeStr = cellString(sheet, i, timeCol)
                if (parseTime(timeStr) != null) return i
            }
        }
        return 0
    }

    fun parse(input: InputStream, defaultDurationMinutes: Int = 40): MySchoolResult {
        val workbook = WorkbookFactory.create(input)
        val sheet = workbook.getSheetAt(0)
        val rows = mutableListOf<MySchoolRow>()
        val errors = mutableListOf<String>()
        val startRow = findDataStartRow(sheet)
        var consecutiveEmpty = 0

        for (i in startRow..(startRow + 30)) {
            if (i > sheet.lastRowNum) break
            var hasAny = false
            for (dayIdx in 0 until 6) {
                val (timeCol, subjCol) = dayColumns[dayIdx]
                val timeStr = cellString(sheet, i, timeCol)
                val subjStr = cellString(sheet, i, subjCol)
                if (timeStr.isBlank() && subjStr.isBlank()) continue
                hasAny = true
                if (timeStr.isBlank()) {
                    errors.add("Строка ${i + 1}, день ${dayIdx + 1}: время пустое")
                    continue
                }
                val timeMin = parseTime(timeStr)
                if (timeMin == null) {
                    errors.add("Строка ${i + 1}, день ${dayIdx + 1}: неверное время «$timeStr»")
                    continue
                }
                if (subjStr.isBlank()) {
                    errors.add("Строка ${i + 1}, день ${dayIdx + 1}: пустой предмет")
                    continue
                }
                val (subjectKey, classId) = parseSubject(subjStr)
                rows.add(
                    MySchoolRow(
                        dayOfWeek = dayIdx + 1,
                        startTimeMinutes = timeMin,
                        subjectKey = subjectKey,
                        classId = classId,
                    ),
                )
            }
            if (hasAny) {
                consecutiveEmpty = 0
            } else {
                consecutiveEmpty++
                if (consecutiveEmpty >= 3) break
            }
        }
        workbook.close()
        return MySchoolResult(rows, errors)
    }

    fun toEntities(rows: List<MySchoolRow>, defaultDuration: Int = 40): List<ScheduleEntryEntity> = rows.map {
        ScheduleEntryEntity(
            dayOfWeek = it.dayOfWeek,
            startTimeMinutes = it.startTimeMinutes,
            endTimeMinutes = it.startTimeMinutes + defaultDuration,
            subjectKey = it.subjectKey,
            classId = it.classId,
        )
    }
}
