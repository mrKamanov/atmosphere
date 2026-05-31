/**
 * Запись о поведении на уроке: время, ученик, предмет, тип, влияние на балл, комментарий.
 * Data-слой (Room), см. §3.3 ТЗ.
 */
package ru.golpom.atmosphere.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "behavior_logs",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["student_id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["class_id"],
            childColumns = ["class_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("student_id"),
        Index("class_id"),
        Index("import_batch_id"),
        Index(
            value = [
                "timestamp",
                "student_id",
                "class_id",
                "subject_key",
                "behavior_type",
                "score_impact",
                "comment",
            ],
            unique = true,
        ),
    ],
)
data class BehaviorLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "class_id") val classId: String,
    @ColumnInfo(name = "subject_key") val subjectKey: String,
    @ColumnInfo(name = "behavior_type") val behaviorType: String,
    @ColumnInfo(name = "score_impact") val scoreImpact: Int,
    val comment: String,
    /** null — локальные отметки на этом устройстве; иначе id пакета импорта. */
    @ColumnInfo(name = "import_batch_id") val importBatchId: String? = null,
)
