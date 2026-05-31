/**
 * Сериализация лога в UTF-8 CSV (v2: с first_name / last_name для слияния на стороне завуча).
 */
package ru.golpom.atmosphere.data.csv

import java.io.StringWriter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter

object BehaviorLogCsvSerializer {

    fun serialize(rows: List<BehaviorLogImportRow>): String {
        val format = CSVFormat.RFC4180.builder().build()
        StringWriter().use { sw ->
            CSVPrinter(sw, format).use { printer ->
                printer.printRecord(*BehaviorLogCsvColumns.HEADER)
                rows.forEach { row ->
                    val e = row.log
                    printer.printRecord(
                        e.timestamp,
                        e.studentId,
                        e.classId,
                        row.firstName.orEmpty(),
                        row.lastName.orEmpty(),
                        e.subjectKey,
                        e.behaviorType,
                        e.scoreImpact,
                        e.comment,
                    )
                }
            }
            return sw.toString()
        }
    }
}
