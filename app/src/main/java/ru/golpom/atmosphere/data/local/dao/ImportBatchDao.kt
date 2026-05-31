/**
 * Room DAO: ImportBatch.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.golpom.atmosphere.data.local.entity.ImportBatchEntity

@Dao
interface ImportBatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ImportBatchEntity)

    @Query("SELECT * FROM import_batches ORDER BY imported_at_millis DESC")
    fun observeAll(): Flow<List<ImportBatchEntity>>

    @Query("SELECT * FROM import_batches ORDER BY imported_at_millis DESC")
    suspend fun listAll(): List<ImportBatchEntity>

    @Query("SELECT * FROM import_batches WHERE batch_id = :batchId LIMIT 1")
    suspend fun getById(batchId: String): ImportBatchEntity?

    @Query("UPDATE import_batches SET enabled = :enabled WHERE batch_id = :batchId")
    suspend fun setEnabled(batchId: String, enabled: Boolean)

    @Query("DELETE FROM import_batches WHERE batch_id = :batchId")
    suspend fun deleteById(batchId: String)

    @Query("DELETE FROM import_batches")
    suspend fun deleteAll()
}
