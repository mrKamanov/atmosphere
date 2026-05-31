/**
 * Room DAO: Notification.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.golpom.atmosphere.data.local.entity.NotificationEntity

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE isDismissed = 0 ORDER BY timestampMillis DESC")
    fun observeActive(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isDismissed = 0 AND isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert
    suspend fun insert(entity: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notifications SET isDismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: Long)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notifications WHERE isDismissed = 1 AND timestampMillis < :threshold")
    suspend fun cleanDismissedOlderThan(threshold: Long)

    @Query("DELETE FROM notifications WHERE isDismissed = 0 AND timestampMillis < :threshold")
    suspend fun cleanActiveOlderThan(threshold: Long)

    @Query("DELETE FROM notifications WHERE isDismissed = 1")
    suspend fun deleteAllDismissed()

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM notifications WHERE isDismissed = 0")
    suspend fun countActive(): Int
}
