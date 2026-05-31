/**
 * Room DAO: ScheduleEntry.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.golpom.atmosphere.data.local.entity.ScheduleEntryEntity

@Dao
interface ScheduleEntryDao {
    @Query("SELECT * FROM schedule_entries WHERE dayOfWeek = :dayOfWeek ORDER BY startTimeMinutes")
    fun observeByDay(dayOfWeek: Int): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries ORDER BY dayOfWeek, startTimeMinutes")
    fun observeAll(): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT DISTINCT subjectKey FROM schedule_entries WHERE classId = :classId ORDER BY subjectKey")
    fun observeSubjectsByClass(classId: String): Flow<List<String>>

    @Query("SELECT DISTINCT classId FROM schedule_entries ORDER BY classId")
    suspend fun listDistinctClassIds(): List<String>

    @Query("SELECT DISTINCT subjectKey FROM schedule_entries ORDER BY subjectKey")
    suspend fun listDistinctSubjects(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScheduleEntryEntity): Long

    @Update
    suspend fun update(entity: ScheduleEntryEntity)

    @Delete
    suspend fun delete(entity: ScheduleEntryEntity)

    @Query("DELETE FROM schedule_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM schedule_entries")
    suspend fun deleteAll()
}
