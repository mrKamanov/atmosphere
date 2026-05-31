/**
 * Разбор CSV лога поведения (v2 с ФИО или legacy без имён).
 */
package ru.golpom.atmosphere.data.csv

import java.io.StringReader
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import ru.golpom.atmosphere.data.local.entity.BehaviorLogEntity

data class ParseWarning(val lineHint: String, val detail: String)

sealed interface BehaviorLogCsvParseOutcome {
    data class Success(
        val rows: List<BehaviorLogImportRow>,
        val warnings: List<ParseWarning>,
    ) : BehaviorLogCsvParseOutcome

    data class Failure(val message: String) : BehaviorLogCsvParseOutcome
}

object BehaviorLogCsvParser {

    fun parse(text: String): BehaviorLogCsvParseOutcome {
        var body = text.trim()
        if (body.startsWith("\uFEFF")) {
            body = body.removePrefix("\uFEFF").trim()
        }
        if (body.isEmpty()) {
            return BehaviorLogCsvParseOutcome.Failure("Пустой файл")
        }
        val headerLine = body.lineSequence().firstOrNull()?.lowercase().orEmpty()
        val hasNames = BehaviorLogCsvColumns.FIRST_NAME in headerLine &&
            BehaviorLogCsvColumns.LAST_NAME in headerLine
        val header = if (hasNames) BehaviorLogCsvColumns.HEADER else BehaviorLogCsvColumns.HEADER_LEGACY
        val format = CSVFormat.RFC4180.builder()
            .setHeader(*header)
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build()
        return try {
            CSVParser.parse(StringReader(body), format).use { parser ->
                val rows = mutableListOf<BehaviorLogImportRow>()
                val warnings = mutableListOf<ParseWarning>()
                var index = 1
                for (record in parser) {
                    index++
                    try {
                        val ts = record.get(BehaviorLogCsvColumns.TIMESTAMP).toLong()
                        val studentId = record.get(BehaviorLogCsvColumns.STUDENT_ID)
                        val classId = record.get(BehaviorLogCsvColumns.CLASS_ID)
                        val firstName = if (hasNames) record.get(BehaviorLogCsvColumns.FIRST_NAME) else null
                        val lastName = if (hasNames) record.get(BehaviorLogCsvColumns.LAST_NAME) else null
                        val subjectKey = record.get(BehaviorLogCsvColumns.SUBJECT_KEY)
                        val behaviorType = record.get(BehaviorLogCsvColumns.BEHAVIOR_TYPE)
                        val scoreImpact = record.get(BehaviorLogCsvColumns.SCORE_IMPACT).toInt()
                        val comment = record.get(BehaviorLogCsvColumns.COMMENT) ?: ""
                        if (scoreImpact != -1 && scoreImpact != 1) {
                            warnings += ParseWarning("строка $index", "score_impact вне ±1, строка пропущена")
                            continue
                        }
                        rows += BehaviorLogImportRow(
                            log = BehaviorLogEntity(
                                id = 0L,
                                timestamp = ts,
                                studentId = studentId,
                                classId = classId,
                                subjectKey = subjectKey,
                                behaviorType = behaviorType,
                                scoreImpact = scoreImpact,
                                comment = comment,
                            ),
                            firstName = firstName?.takeIf { it.isNotBlank() },
                            lastName = lastName?.takeIf { it.isNotBlank() },
                        )
                    } catch (e: Exception) {
                        warnings += ParseWarning("строка $index", e.message ?: "ошибка разбора")
                    }
                }
                BehaviorLogCsvParseOutcome.Success(rows, warnings)
            }
        } catch (e: Exception) {
            BehaviorLogCsvParseOutcome.Failure(e.message ?: "Ошибка чтения CSV")
        }
    }
}
