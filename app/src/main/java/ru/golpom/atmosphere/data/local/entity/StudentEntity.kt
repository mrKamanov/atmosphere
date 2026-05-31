/**
 * Ученик: ФИО, класс, статус ACTIVE/ARCHIVED.
 * Data-слой (Room), см. §3.2 ТЗ.
 */
package ru.golpom.atmosphere.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["class_id"],
            childColumns = ["class_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("class_id"), Index("created_by_import_batch_id")],
)
data class StudentEntity(
    @PrimaryKey @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    @ColumnInfo(name = "class_id") val classId: String,
    @ColumnInfo(name = "status") val status: String,
    /** Если ученик появился только из отчёта другого учителя — скрыт в режиме «Учитель». */
    @ColumnInfo(name = "created_by_import_batch_id") val createdByImportBatchId: String? = null,
)
