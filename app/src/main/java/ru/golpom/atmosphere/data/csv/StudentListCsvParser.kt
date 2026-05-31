/**
 * Разбор CSV списка учеников: полный формат с `student_id` или короткий «имя, фамилия, класс» с генерацией ID.
 * Data-слой; первая строка — заголовок (§5.2 ТЗ, упрощённый импорт без Excel).
 */
package ru.golpom.atmosphere.data.csv

import java.io.StringReader
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

data class ParsedStudentRow(
    val studentId: String?,
    val firstName: String,
    val lastName: String,
    val classId: String,
    val status: String,
)

sealed interface StudentListCsvParseOutcome {
    data class Success(val rows: List<ParsedStudentRow>, val warnings: List<ParseWarning>) : StudentListCsvParseOutcome
    data class Failure(val message: String) : StudentListCsvParseOutcome
}

object StudentListCsvParser {

    private val fullHeaderWithStatus = arrayOf("student_id", "first_name", "last_name", "class_id", "status")
    private val fullHeaderNoStatus = arrayOf("student_id", "first_name", "last_name", "class_id")
    private val shortHeader = arrayOf("first_name", "last_name", "class_id")

    fun parse(text: String): StudentListCsvParseOutcome {
        var body = text.trim()
        if (body.startsWith("\uFEFF")) body = body.removePrefix("\uFEFF").trim()
        if (body.isEmpty()) return StudentListCsvParseOutcome.Failure("Пустой файл")
        val headerLine = body.lineSequence().firstOrNull()?.trim()?.lowercase().orEmpty()
        val headerCells = headerLine.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val isFull = headerCells.firstOrNull() == "student_id"
        val isShort = headerCells.firstOrNull() == "first_name" && !isFull
        if (!isFull && !isShort) {
            return StudentListCsvParseOutcome.Failure(
                "Не удалось разобрать файл. Скачайте шаблон в разделе «Мои ученики» и заполните его.",
            )
        }
        val headerArray = when {
            isShort -> shortHeader
            headerCells.contains("status") -> fullHeaderWithStatus
            else -> fullHeaderNoStatus
        }
        val includeStatus = isFull && headerArray.size == 5
        val format = CSVFormat.RFC4180.builder()
            .setHeader(*headerArray)
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build()
        return try {
            CSVParser.parse(StringReader(body), format).use { parser ->
                val rows = mutableListOf<ParsedStudentRow>()
                val warnings = mutableListOf<ParseWarning>()
                var line = 1
                for (record in parser) {
                    line++
                    try {
                        if (isShort) {
                            val fn = record.get("first_name").trim()
                            val ln = record.get("last_name").trim()
                            val cid = record.get("class_id").trim()
                            if (fn.isEmpty() || ln.isEmpty() || cid.isEmpty()) {
                                warnings += ParseWarning("строка $line", "пустые поля, пропуск")
                                continue
                            }
                            rows += ParsedStudentRow(null, fn, ln, cid, "ACTIVE")
                        } else {
                            val sid = record.get("student_id").trim().ifEmpty { null }
                            val fn = record.get("first_name").trim()
                            val ln = record.get("last_name").trim()
                            val cid = record.get("class_id").trim()
                            val st = if (includeStatus) {
                                record.get("status").trim().ifEmpty { "ACTIVE" }.uppercase()
                            } else {
                                "ACTIVE"
                            }
                            if (fn.isEmpty() || ln.isEmpty() || cid.isEmpty()) {
                                warnings += ParseWarning("строка $line", "пустые поля, пропуск")
                                continue
                            }
                            rows += ParsedStudentRow(sid, fn, ln, cid, st)
                        }
                    } catch (e: Exception) {
                        warnings += ParseWarning("строка $line", e.message ?: "ошибка")
                    }
                }
                StudentListCsvParseOutcome.Success(rows, warnings)
            }
        } catch (e: Exception) {
            StudentListCsvParseOutcome.Failure(e.message ?: "Не удалось прочитать файл")
        }
    }
}
