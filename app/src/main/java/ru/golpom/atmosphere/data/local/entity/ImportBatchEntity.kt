/**
 * Пакет импортированных данных от учителя (метаданные для режима завуча).
 */
package ru.golpom.atmosphere.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "import_batches")
data class ImportBatchEntity(
    @PrimaryKey
    @ColumnInfo(name = "batch_id") val batchId: String,
    @ColumnInfo(name = "teacher_profile_id") val teacherProfileId: String,
    @ColumnInfo(name = "teacher_profile_short_id") val teacherProfileShortId: String,
    @ColumnInfo(name = "teacher_last_name") val teacherLastName: String,
    @ColumnInfo(name = "teacher_display_name") val teacherDisplayName: String,
    @ColumnInfo(name = "source_file_name") val sourceFileName: String,
    @ColumnInfo(name = "imported_at_millis") val importedAtMillis: Long,
    @ColumnInfo(name = "period_label") val periodLabel: String,
    @ColumnInfo(name = "period_from_millis") val periodFromMillis: Long?,
    @ColumnInfo(name = "period_to_millis") val periodToMillis: Long?,
    @ColumnInfo(name = "records_in_file") val recordsInFile: Int,
    @ColumnInfo(name = "inserted_count") val insertedCount: Int,
    @ColumnInfo(name = "skipped_duplicate") val skippedDuplicate: Int,
    @ColumnInfo(name = "skipped_unknown_student") val skippedUnknownStudent: Int,
    @ColumnInfo(name = "enabled") val enabled: Boolean = true,
)
