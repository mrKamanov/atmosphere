/**
 * Импорт зашифрованных отчётов учителя для завуча.
 * Data-слой; § экспорт/импорт ТЗ.
 */
package ru.golpom.atmosphere.data.export

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import ru.golpom.atmosphere.data.csv.BehaviorLogCsvParseOutcome
import ru.golpom.atmosphere.data.csv.BehaviorLogCsvParser
import ru.golpom.atmosphere.data.csv.BehaviorLogImportStats
import ru.golpom.atmosphere.data.local.entity.ImportBatchEntity
import ru.golpom.atmosphere.data.repository.CatalogRepository

data class DeputyImportResult(
    val label: String,
    val batchId: String?,
    val stats: BehaviorLogImportStats?,
    val parseWarnings: Int,
    val errorMessage: String? = null,
)

@Singleton
class DeputyReportImporter @Inject constructor(
    private val catalogRepository: CatalogRepository,
) {

    suspend fun importFile(
        fileName: String,
        bytes: ByteArray,
    ): DeputyImportResult {
        return when {
            TeacherReportFile.isSealed(bytes) || TeacherReportZip.isZip(bytes) ->
                importReportArchive(fileName, bytes)
            else -> importCsvLegacy(fileName, String(bytes, Charsets.UTF_8))
        }
    }

    private suspend fun importReportArchive(fileName: String, bytes: ByteArray): DeputyImportResult {
        val opened = TeacherReportFile.open(bytes)
        if (opened.unrecognized) {
            return DeputyImportResult(
                fileName,
                null,
                null,
                0,
                "Такой файл не подходит. Попросите учителя отправить отчёт из «Атмосферы» (Настройки → Отправить отчёт завучу).",
            )
        }
        val csv = opened.csv
            ?: return DeputyImportResult(fileName, null, null, 0, "В файле нет данных об отметках.")
        val manifest = opened.manifest
        val parsed = when (val outcome = BehaviorLogCsvParser.parse(csv)) {
            is BehaviorLogCsvParseOutcome.Failure ->
                return DeputyImportResult(fileName, null, null, 0, outcome.message)
            is BehaviorLogCsvParseOutcome.Success -> outcome
        }
        // ВАЖНО: каждый импорт — отдельный пакет, иначе разные файлы "склеиваются" в один тумблер.
        val batchId = UUID.randomUUID().toString()
        val teacherProfileId = manifest?.teacher_profile_id?.takeIf { it.isNotBlank() } ?: batchId
        val fileParsed = TeacherReportFileNameParser.parse(fileName)
        val lastName = manifest?.teacher_last_name?.takeIf { it.isNotBlank() }
            ?: fileParsed?.teacherLastName
            ?: "Учитель"
        val shortId = manifest?.teacher_profile_short_id?.takeIf { it.isNotBlank() }
            ?: fileParsed?.teacherProfileShortId
            ?: batchId.replace("-", "").take(12)
        val displayName = manifest?.teacher_display_name?.takeIf { it.isNotBlank() }
            ?: lastName
        val stats = catalogRepository.importBehaviorLogs(parsed.rows, batchId)
        val batch = ImportBatchEntity(
            batchId = batchId,
            teacherProfileId = teacherProfileId,
            teacherProfileShortId = shortId,
            teacherLastName = lastName,
            teacherDisplayName = displayName,
            sourceFileName = fileName,
            importedAtMillis = System.currentTimeMillis(),
            periodLabel = manifest?.period_label ?: "Период не указан",
            periodFromMillis = manifest?.period_from_millis,
            periodToMillis = manifest?.period_to_millis,
            recordsInFile = parsed.rows.size,
            insertedCount = stats.inserted,
            skippedDuplicate = stats.skippedDuplicate,
            skippedUnknownStudent = stats.skippedUnknownStudent,
            enabled = true,
        )
        catalogRepository.upsertImportBatch(batch)
        return DeputyImportResult(
            label = "$displayName ($shortId)",
            batchId = batchId,
            stats = stats,
            parseWarnings = parsed.warnings.size,
        )
    }

    private suspend fun importCsvLegacy(fileName: String, text: String): DeputyImportResult {
        when (val outcome = BehaviorLogCsvParser.parse(text)) {
            is BehaviorLogCsvParseOutcome.Failure ->
                return DeputyImportResult(fileName, null, null, 0, outcome.message)
            is BehaviorLogCsvParseOutcome.Success -> {
                val parsedName = TeacherReportFileNameParser.parse(fileName)
                val batchId = UUID.randomUUID().toString()
                val lastName = parsedName?.teacherLastName ?: "Учитель"
                val shortId = parsedName?.teacherProfileShortId ?: batchId.replace("-", "").take(12)
                val stats = catalogRepository.importBehaviorLogs(outcome.rows, batchId)
                catalogRepository.upsertImportBatch(
                    ImportBatchEntity(
                        batchId = batchId,
                        teacherProfileId = batchId,
                        teacherProfileShortId = shortId,
                        teacherLastName = lastName,
                        teacherDisplayName = lastName,
                        sourceFileName = fileName,
                        importedAtMillis = System.currentTimeMillis(),
                        periodLabel = "Старый отчёт (таблица)",
                        periodFromMillis = null,
                        periodToMillis = null,
                        recordsInFile = outcome.rows.size,
                        insertedCount = stats.inserted,
                        skippedDuplicate = stats.skippedDuplicate,
                        skippedUnknownStudent = stats.skippedUnknownStudent,
                        enabled = true,
                    ),
                )
                return DeputyImportResult(
                    label = "$lastName ($shortId)",
                    batchId = batchId,
                    stats = stats,
                    parseWarnings = outcome.warnings.size,
                )
            }
        }
    }
}
