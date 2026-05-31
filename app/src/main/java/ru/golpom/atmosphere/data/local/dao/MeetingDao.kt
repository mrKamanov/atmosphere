/**
 * Room DAO: Meeting.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.golpom.atmosphere.data.local.entity.MeetingEntity

@Dao
interface MeetingDao {
    @Query("SELECT * FROM meetings ORDER BY date_time_millis ASC")
    fun observeAll(): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE date_time_millis > :fromMillis ORDER BY date_time_millis ASC LIMIT 1")
    fun observeNext(fromMillis: Long): Flow<MeetingEntity?>

    @Insert
    suspend fun insert(entity: MeetingEntity): Long

    @Query("UPDATE meetings SET class_id = :classId, date_time_millis = :dateTimeMillis, topic = :topic, notes = :notes WHERE id = :id")
    suspend fun update(id: Long, classId: String, dateTimeMillis: Long, topic: String, notes: String)

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM meetings")
    suspend fun deleteAll()
}
