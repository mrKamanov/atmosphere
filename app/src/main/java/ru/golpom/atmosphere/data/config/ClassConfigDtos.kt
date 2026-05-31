/**
 * DTO файла `Class_Config.json`: список учеников с ID для обмена между учителями (§5.2 ТЗ).
 * Data-слой; сериализация через [ClassConfigJsonCodec].
 */
package ru.golpom.atmosphere.data.config

import kotlinx.serialization.Serializable

@Serializable
data class ClassConfigFileDto(
    val version: Int = 1,
    val students: List<ClassConfigStudentDto> = emptyList(),
)

@Serializable
data class ClassConfigStudentDto(
    val student_id: String,
    val first_name: String,
    val last_name: String,
    val class_id: String,
    val status: String = "ACTIVE",
)
