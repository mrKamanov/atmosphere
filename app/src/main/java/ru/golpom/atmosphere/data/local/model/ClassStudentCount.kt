/**
 * Число активных учеников в классе.
 * Data-слой (Room POJO).
 */
package ru.golpom.atmosphere.data.local.model

import androidx.room.ColumnInfo

data class ClassStudentCount(
    @ColumnInfo(name = "class_id") val classId: String,
    @ColumnInfo(name = "student_count") val studentCount: Int,
)
