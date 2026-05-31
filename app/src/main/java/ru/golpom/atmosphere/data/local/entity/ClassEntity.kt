/**
 * Сущность класса: первичный ключ — строковый код класса (например «6-А»).
 * Data-слой (Room), см. §3.1 ТЗ.
 */
package ru.golpom.atmosphere.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey @ColumnInfo(name = "class_id") val classId: String,
)
