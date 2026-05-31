/**
 * Хранение справочника классов (код класса).
 * Data-слой (Room).
 */
package ru.golpom.atmosphere.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.golpom.atmosphere.data.local.entity.ClassEntity

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes ORDER BY class_id")
    fun observeAll(): Flow<List<ClassEntity>>

    /** Классы учителя: есть свои ученики, расписание или локальные отметки. */
    @Query(
        """
        SELECT DISTINCT c.* FROM classes c
        WHERE EXISTS (
            SELECT 1 FROM students s
            WHERE s.class_id = c.class_id AND s.status = 'ACTIVE'
              AND s.created_by_import_batch_id IS NULL
        )
        OR EXISTS (
            SELECT 1 FROM schedule_entries e WHERE e.classId = c.class_id
        )
        OR EXISTS (
            SELECT 1 FROM behavior_logs l
            WHERE l.class_id = c.class_id AND l.import_batch_id IS NULL
        )
        OR NOT EXISTS (
            SELECT 1 FROM students s WHERE s.class_id = c.class_id
        )
        ORDER BY c.class_id
        """,
    )
    fun observeForTeacher(): Flow<List<ClassEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ClassEntity)

    @Query("DELETE FROM classes WHERE class_id = :classId")
    suspend fun deleteByClassId(classId: String)

    @Query("DELETE FROM classes")
    suspend fun deleteAll()
}
