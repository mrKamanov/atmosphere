/**
 * Формирование зашифрованного отчёта (.atmo) для передачи завучу.
 */
package ru.golpom.atmosphere.data.export

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import ru.golpom.atmosphere.data.csv.BehaviorLogCsvSerializer
import ru.golpom.atmosphere.data.csv.TeacherBehaviorExportFileName
import ru.golpom.atmosphere.data.preferences.UserPreferencesRepository
import ru.golpom.atmosphere.data.repository.CatalogRepository
import ru.golpom.atmosphere.domain.export.ExportPeriodCalculator
import ru.golpom.atmosphere.domain.export.TeacherExportRequest
import ru.golpom.atmosphere.ui.export.ExportPayload

@Singleton
class TeacherBehaviorExporter @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {

    suspend fun export(request: TeacherExportRequest): ExportPayload {
        val (from, to) = ExportPeriodCalculator.calc(request.period)
        val logs = catalogRepository.getLogsForExport(request.scope, from, to)
        val teacherLastName = userPreferencesRepository.teacherLastName.first()
        val userName = userPreferencesRepository.userName.first()
        val teacherProfileId = userPreferencesRepository.getOrCreateTeacherProfileId()
        val teacherProfileShortId = userPreferencesRepository.getOrCreateTeacherProfileShortId()
        val periodTag = ExportPeriodCalculator.periodTag(request.period)
        val periodLabel = ExportPeriodCalculator.label(request.period)
        val fileName = if (request.options.neutralFileName) {
            TeacherBehaviorExportFileName.buildNeutralReportName(teacherProfileShortId)
        } else {
            TeacherBehaviorExportFileName.buildDetailedReportName(
                scopeTag = request.scope.scopeTag(),
                periodTag = periodTag,
                teacherLastName = teacherLastName,
                teacherProfileShortId = teacherProfileShortId,
            )
        }
        val exportRows = catalogRepository.buildBehaviorLogExportRows(logs)
        val csv = BehaviorLogCsvSerializer.serialize(exportRows)
        val manifest = ExportManifestDto(
            teacher_profile_id = teacherProfileId,
            teacher_profile_short_id = teacherProfileShortId,
            teacher_last_name = teacherLastName,
            teacher_display_name = userName.ifBlank { teacherLastName },
            exported_at_millis = System.currentTimeMillis(),
            period_kind = request.period.kind.name,
            period_label = periodLabel,
            period_from_millis = from,
            period_to_millis = to,
            scope_tag = request.scope.scopeTag(),
            record_count = logs.size,
            sealed = true,
            format_version = 2,
            format = "atmo_v2",
            identity_merge = true,
        )
        val manifestJson = ExportManifestCodec.encode(manifest)
        val sealedBytes = TeacherReportFile.seal(manifestJson, csv)
        return ExportPayload(
            fileName = fileName,
            utf8Bytes = sealedBytes,
            mimeType = TeacherReportFile.MIME_TYPE,
            recordCount = logs.size,
            periodLabel = periodLabel,
            appSealed = true,
        )
    }
}
