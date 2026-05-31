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

    private val classFromFilenameRegex = Regex("(\\d{1,2})\\s+([А-ЯЁ])\\s+\\d{4}")

    /**
     * Extracts classId from filename like "География 9 Л 2025-26_9ИЛМ классы.xlsx"
     * Returns "9-Л" or empty string if not found.
     */
    fun extractClassId(filename: String): String {
        val match = classFromFilenameRegex.find(filename)
        if (match != null) {
            val grade = match.groupValues[1]
            val letter = match.groupValues[2]
            return "$grade-$letter"
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
