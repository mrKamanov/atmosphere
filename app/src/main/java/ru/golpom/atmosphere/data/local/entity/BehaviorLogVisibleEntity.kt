/**
 * Room-сущность BehaviorLogVisible.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView(
    viewName = "behavior_logs_visible",
    value = """
        SELECT id, timestamp, student_id, class_id, subject_key, behavior_type, score_impact, comment, import_batch_id
        FROM behavior_logs
        WHERE import_batch_id IS NULL
           OR import_batch_id IN (SELECT batch_id FROM import_batches WHERE enabled = 1)
    """,
)
data class BehaviorLogVisibleEntity(
    val id: Long,
    val timestamp: Long,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "class_id") val classId: String,
    @ColumnInfo(name = "subject_key") val subjectKey: String,
    @ColumnInfo(name = "behavior_type") val behaviorType: String,
    @ColumnInfo(name = "score_impact") val scoreImpact: Int,
    val comment: String,
    @ColumnInfo(name = "import_batch_id") val importBatchId: String? = null,
)

fun BehaviorLogVisibleEntity.toBehaviorLogEntity(): BehaviorLogEntity = BehaviorLogEntity(
    id = id,
    timestamp = timestamp,
    studentId = studentId,
    classId = classId,
    subjectKey = subjectKey,
    behaviorType = behaviorType,
    scoreImpact = scoreImpact,
    comment = comment,
    importBatchId = importBatchId,
)
