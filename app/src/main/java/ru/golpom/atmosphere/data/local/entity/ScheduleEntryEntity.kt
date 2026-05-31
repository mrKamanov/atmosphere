/**
 * Room-сущность ScheduleEntry.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_entries")
data class ScheduleEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,
    val startTimeMinutes: Int,
    val endTimeMinutes: Int,
    val subjectKey: String,
    val classId: String,
)
