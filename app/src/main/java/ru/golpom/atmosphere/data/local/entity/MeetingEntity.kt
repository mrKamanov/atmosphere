/**
 * Room-сущность Meeting.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "class_id") val classId: String,
    @ColumnInfo(name = "date_time_millis") val dateTimeMillis: Long,
    val topic: String,
    val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
