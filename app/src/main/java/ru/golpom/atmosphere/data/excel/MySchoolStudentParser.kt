/**
 * Парсер списка учеников из Excel «Моя школа».
 * Data-слой.
 */
package ru.golpom.atmosphere.data.excel

import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

object MySchoolStudentParser {

    data class ParsedStudent(
        val firstName: String,
        val lastName: String,
    )

    data class ParseResult(
        val students: List<ParsedStudent>,
        val classId: String,
        val errors: List<String> = emptyList(),
    )

    private val classFromFilenameRegex = listOf(
        // "География 9-Л 9", "9-А", "11-Ж 2025"
        Regex("(\\d{1,2})\\s*[-–]\\s*([А-ЯЁа-яё])(?![А-ЯЁа-яё])"),
        // "География 9 Л 2025-26_9ИЛМ классы.xlsx"
        Regex("(\\d{1,2})\\s+([А-ЯЁа-яё])(?![А-ЯЁа-яё])\\s+\\d{1,4}"),
        // "Русский 5Б 2024-25"
        Regex("(\\d{1,2})\\s*([А-ЯЁа-яё])(?![А-ЯЁа-яё])\\s+\\d{1,4}[-–]\\d{1,4}"),
        // "9Б класс"
        Regex("(\\d{1,2})\\s*([А-ЯЁа-яё])(?![А-ЯЁа-яё])(?:\\s+класс)?"),
    )

    /**
     * Extracts classId from filename like "География 9-Л 9" or "География 9 Л 2025-26"
     * or "Математика 9Л" or "9-А.xlsx"
     * Returns "9-Л" or empty string if not found.
     */
    fun extractClassId(filename: String): String {
        for (regex in classFromFilenameRegex) {
            val match = regex.find(filename)
            if (match != null) {
                val grade = match.groupValues[1]
                val letter = match.groupValues[2].uppercase()
                return "$grade-$letter"
            }
        }
        return ""
    }

    fun parse(input: InputStream, dataStartRow: Int = 3): List<ParsedStudent> {
        val workbook = WorkbookFactory.create(input)
        val sheet = workbook.getSheetAt(0)
        val students = mutableListOf<ParsedStudent>()
        var consecutiveEmpty = 0

        for (i in dataStartRow..minOf(sheet.lastRowNum, dataStartRow + 50)) {
            val r = sheet.getRow(i) ?: continue
            val c = r.getCell(1)
            if (c == null) { consecutiveEmpty++; if (consecutiveEmpty >= 5) break; continue }
            val raw = when (c.cellType) {
                org.apache.poi.ss.usermodel.CellType.STRING -> c.stringCellValue.trim()
                else -> ""
            }
            if (raw.isBlank()) { consecutiveEmpty++; if (consecutiveEmpty >= 5) break; continue }
            consecutiveEmpty = 0

            val parts = raw.split("\\s+".toRegex())
            if (parts.size < 2) continue
            val lastName = StudentExcelParser.normalizeName(parts[0])
            val firstName = StudentExcelParser.normalizeName(parts[1])
            students.add(ParsedStudent(firstName, lastName))
        }
        workbook.close()
        return students
    }
}
