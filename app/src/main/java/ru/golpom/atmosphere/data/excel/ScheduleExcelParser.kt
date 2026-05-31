/**
 * Парсер расписания из Excel.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.excel

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import ru.golpom.atmosphere.data.local.entity.ScheduleEntryEntity
import java.io.InputStream
import java.io.OutputStream

object ScheduleExcelParser {

    private val dayNames = mapOf(
        "пн" to 1, "пн." to 1, "1" to 1,
        "вт" to 2, "вт." to 2, "2" to 2,
        "ср" to 3, "ср." to 3, "3" to 3,
        "чт" to 4, "чт." to 4, "4" to 4,
        "пт" to 5, "пт." to 5, "5" to 5,
        "сб" to 6, "сб." to 6, "6" to 6,
    )

    data class ParsedRow(
        val dayOfWeek: Int,
        val startTimeMinutes: Int,
        val endTimeMinutes: Int,
        val subjectKey: String,
        val classId: String,
        val errors: List<String> = emptyList(),
    )

    data class ParseResult(
        val rows: List<ParsedRow>,
        val errors: List<String> = emptyList(),
    )

    private fun parseTime(value: String): Int? {
        val cleaned = value.trim().replace("–", "-").replace("—", "-")
        val parts = cleaned.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    private fun cellString(sheet: org.apache.poi.ss.usermodel.Sheet, row: Int, col: Int): String {
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

    fun parseTemplate(input: InputStream): ParseResult {
        val workbook = WorkbookFactory.create(input)
        val sheet = workbook.getSheetAt(0)
        val rows = mutableListOf<ParsedRow>()
        val errors = mutableListOf<String>()
        var consecutiveEmpty = 0

        for (i in 0..minOf(sheet.lastRowNum, 200)) {
            val r = sheet.getRow(i) ?: continue
            if (i == 0 && cellString(sheet, 0, 0).equals("день", ignoreCase = true)) continue
            val dayStr = cellString(sheet, i, 0)
            val timeStr = cellString(sheet, i, 1)
            val subject = cellString(sheet, i, 2)
            val classStr = cellString(sheet, i, 3)

            if (dayStr.isBlank() && timeStr.isBlank() && subject.isBlank()) {
                consecutiveEmpty++
                if (consecutiveEmpty >= 5) break
                continue
            }
            consecutiveEmpty = 0

            val rowErrors = mutableListOf<String>()
            val day = dayNames[dayStr.trim().lowercase()]
            if (day == null) { rowErrors.add("Строка ${i + 1}: неверный день «$dayStr»"); continue }
            val timeMin = parseTime(timeStr)
            if (timeMin == null) { rowErrors.add("Строка ${i + 1}: неверное время «$timeStr»"); continue }
            if (subject.isBlank()) { rowErrors.add("Строка ${i + 1}: пустой предмет"); continue }

            val classId = normalizeClassId(classStr)
            if (classId.isBlank()) { rowErrors.add("Строка ${i + 1}: пустой класс"); continue }

            if (rowErrors.isNotEmpty()) {
                errors.addAll(rowErrors)
                continue
            }

            rows.add(
                ParsedRow(
                    dayOfWeek = day,
                    startTimeMinutes = timeMin,
                    endTimeMinutes = timeMin + 40,
                    subjectKey = normalizeSubject(subject),
                    classId = classId,
                ),
            )
        }
        workbook.close()
        return ParseResult(rows, errors)
    }

    fun generateTemplate(output: OutputStream) {
        XSSFWorkbook().use { wb ->
            val sheet = wb.createSheet("Расписание")
            val header = sheet.createRow(0)
            header.createCell(0).setCellValue("День")
            header.createCell(1).setCellValue("Время")
            header.createCell(2).setCellValue("Предмет")
            header.createCell(3).setCellValue("Класс")
            header.createCell(4).setCellValue("Заполните таблицу. Каждая строка = один урок. День: Пн/Вт/Ср/Чт/Пт/Сб. Время начала в формате ЧЧ:ММ. Предмет и класс — текстом, например 6-А. Длительность урока = 40 мин.")

            val examples = listOf(
                arrayOf("Пн", "08:30", "География", "6-А"),
                arrayOf("Пн", "09:25", "Математика", "6-А"),
                arrayOf("Вт", "08:30", "Русский язык", "7-Б"),
                arrayOf("Ср", "10:30", "Биология", "5-Л"),
            )
            examples.forEachIndexed { idx, row ->
                val r = sheet.createRow(idx + 1)
                row.forEachIndexed { ci, v -> r.createCell(ci).setCellValue(v) }
            }
            for (c in 0..3) sheet.setColumnWidth(c, 14 * 256)
            sheet.setColumnWidth(4, 90 * 256)
            wb.write(output)
        }
    }

    private fun normalizeSubject(input: String): String {
        var t = input.trim()
        if (t.endsWith("внеурочная деятельность", ignoreCase = true)) {
            t = t.substring(0, t.length - "внеурочная деятельность".length).trim()
        }
        if (t.isEmpty()) return t
        return t.substring(0, 1).uppercase() + t.substring(1)
    }

    private fun normalizeClassId(input: String): String {
        val t = input.trim().replace(" ", "")
        val regex = Regex("^(\\d+)[-]?([а-яА-Яa-zA-Z]+)$")
        val m = regex.find(t)
        return if (m != null) "${m.groupValues[1]}-${m.groupValues[2].uppercase()}" else t.uppercase()
    }

    fun toEntities(rows: List<ParsedRow>): List<ScheduleEntryEntity> = rows.map {
        ScheduleEntryEntity(
            dayOfWeek = it.dayOfWeek,
            startTimeMinutes = it.startTimeMinutes,
            endTimeMinutes = it.endTimeMinutes,
            subjectKey = it.subjectKey,
            classId = it.classId,
        )
    }
}
