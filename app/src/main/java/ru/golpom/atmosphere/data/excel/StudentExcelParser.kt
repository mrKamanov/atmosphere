/**
 * Парсер списка учеников из Excel.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.excel

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import ru.golpom.atmosphere.data.local.entity.StudentEntity
import java.io.InputStream
import java.io.OutputStream

object StudentExcelParser {

    data class ParsedStudent(
        val firstName: String,
        val lastName: String,
        val classId: String,
        val errors: List<String> = emptyList(),
    )

    data class ParseResult(
        val students: List<ParsedStudent>,
        val errors: List<String> = emptyList(),
    )

    fun parse(input: InputStream): ParseResult {
        val workbook = WorkbookFactory.create(input)
        val sheet = workbook.getSheetAt(0)
        val students = mutableListOf<ParsedStudent>()
        val errors = mutableListOf<String>()
        var consecutiveEmpty = 0

        for (i in 0..minOf(sheet.lastRowNum, 500)) {
            val r = sheet.getRow(i) ?: continue
            if (i == 0) {
                val h0 = cellStr(r, 0)
                val h1 = cellStr(r, 1)
                val h2 = cellStr(r, 2)
                if (h0.equals("фамилия", ignoreCase = true) || h1.equals("имя", ignoreCase = true)) continue
            }
            val lastName = cellStr(r, 0)
            val firstName = cellStr(r, 1)
            val classId = cellStr(r, 2)

            if (lastName.isBlank() && firstName.isBlank() && classId.isBlank()) {
                consecutiveEmpty++
                if (consecutiveEmpty >= 5) break
                continue
            }
            consecutiveEmpty = 0
            if (lastName.isBlank()) { errors.add("Строка ${i + 1}: пустая фамилия"); continue }
            if (firstName.isBlank()) { errors.add("Строка ${i + 1}: пустое имя"); continue }
            if (classId.isBlank()) { errors.add("Строка ${i + 1}: пустой класс"); continue }

            students.add(
                ParsedStudent(
                    lastName = normalizeName(lastName),
                    firstName = normalizeName(firstName),
                    classId = normalizeClassId(classId),
                ),
            )
        }
        workbook.close()
        return ParseResult(students, errors)
    }

    fun generateTemplate(output: OutputStream) {
        XSSFWorkbook().use { wb ->
            val sheet = wb.createSheet("Ученики")
            val header = sheet.createRow(0)
            header.createCell(0).setCellValue("Фамилия")
            header.createCell(1).setCellValue("Имя")
            header.createCell(2).setCellValue("Класс")

            listOf(
                arrayOf("Иванов", "Сергей", "6-А"),
                arrayOf("Петров", "Алексей", "6-А"),
            ).forEachIndexed { idx, row ->
                val r = sheet.createRow(idx + 1)
                row.forEachIndexed { ci, v -> r.createCell(ci).setCellValue(v) }
            }
            for (c in 0..2) sheet.setColumnWidth(c, 16 * 256)
            wb.write(output)
        }
    }

    private fun cellStr(row: org.apache.poi.ss.usermodel.Row, col: Int): String {
        val c = row.getCell(col) ?: return ""
        return when (c.cellType) {
            org.apache.poi.ss.usermodel.CellType.STRING -> c.stringCellValue.trim()
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                val v = c.numericCellValue
                if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
            }
            else -> ""
        }
    }

    fun normalizeName(raw: String): String {
        val parts = raw.trim().split("\\s+".toRegex())
        val name = parts[0]
        if (name.isEmpty()) return name
        return name.substring(0, 1).uppercase() + name.substring(1).lowercase()
    }

    private fun normalizeClassId(input: String): String {
        val t = input.trim().replace(" ", "")
        val regex = Regex("^(\\d+)[-]?([а-яА-Яa-zA-Z]+)$")
        val m = regex.find(t)
        return if (m != null) "${m.groupValues[1]}-${m.groupValues[2].uppercase()}" else t.uppercase()
    }

    fun toEntities(students: List<ParsedStudent>, year: Int = java.time.Year.now().value, seqStart: Int = 0): List<StudentEntity> {
        val seq = java.util.concurrent.atomic.AtomicInteger(seqStart)
        return students.mapIndexed { idx, s ->
            StudentEntity(
                studentId = "$year-${s.classId}-${System.currentTimeMillis() % 10000 + idx}",
                firstName = s.firstName,
                lastName = s.lastName,
                classId = s.classId,
                status = "ACTIVE",
            )
        }
    }
}
