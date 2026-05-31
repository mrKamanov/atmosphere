/**
 * Ученики: активные по классу, вставка и обновление.
 * Data-слой (Room).
 */
package ru.golpom.atmosphere.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.golpom.atmosphere.data.local.entity.StudentEntity

@Dao
interface StudentDao {
    @Query(
        """
        SELECT * FROM students
        WHERE class_id = :classId AND status = 'ACTIVE'
        ORDER BY last_name, first_name
        """,
    )
    fun observeActiveByClass(classId: String): Flow<List<StudentEntity>>

    @Query(
        """
        SELECT * FROM students
        WHERE class_id = :classId AND status = 'ACTIVE'
          AND created_by_import_batch_id IS NULL
        ORDER BY last_name, first_name
        """,
    )
    fun observeTeacherActiveByClass(classId: String): Flow<List<StudentEntity>>

    @Query(
        """
        SELECT * FROM students
        WHERE status = 'ACTIVE' AND created_by_import_batch_id IS NULL
        ORDER BY class_id, last_name, first_name
        """,
    )
    fun observeTeacherAllActive(): Flow<List<StudentEntity>>

    @Query(
        """
        SELECT * FROM students
        WHERE status = 'ACTIVE' AND created_by_import_batch_id IS NULL AND (
            last_name LIKE '%' || :query || '%' OR first_name LIKE '%' || :query || '%'
            OR class_id LIKE '%' || :query || '%'
        )
        ORDER BY class_id, last_name, first_name
        """,
    )
    suspend fun searchTeacherActiveStudents(query: String): List<StudentEntity>

    @Query("DELETE FROM students WHERE created_by_import_batch_id = :batchId")
    suspend fun deleteCreatedByImportBatch(batchId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: StudentEntity): Long

    @Update
    suspend fun update(entity: StudentEntity)

    @Query("SELECT COUNT(*) FROM students WHERE status = 'ACTIVE'")
    fun observeCount(): kotlinx.coroutines.flow.Flow<Long>

    @Query("SELECT COUNT(*) FROM students WHERE status = 'ACTIVE' AND created_by_import_batch_id IS NULL")
    fun observeTeacherCount(): kotlinx.coroutines.flow.Flow<Long>

    @Query(
        """
        SELECT * FROM students
        WHERE created_by_import_batch_id IS NULL
        ORDER BY class_id, last_name, first_name
        """,
    )
    fun observeTeacherAll(): Flow<List<StudentEntity>>

    @Query("SELECT COUNT(*) FROM students WHERE class_id = :classId")
    suspend fun countByClass(classId: String): Long

    @Query("SELECT * FROM students ORDER BY class_id, last_name, first_name")
    suspend fun listAll(): List<StudentEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM students WHERE student_id = :studentId LIMIT 1)")
    suspend fun exists(studentId: String): Boolean

    @Query(
        """
        SELECT * FROM students
        WHERE class_id = :classId
          AND LOWER(TRIM(last_name)) = LOWER(TRIM(:lastName))
          AND LOWER(TRIM(first_name)) = LOWER(TRIM(:firstName))
        LIMIT 1
        """,
    )
    suspend fun findByClassAndName(classId: String, firstName: String, lastName: String): StudentEntity?

    @Query("SELECT * FROM students WHERE student_id IN (:ids)")
    suspend fun listByIds(ids: List<String>): List<StudentEntity>

    @Query("SELECT * FROM students WHERE student_id = :studentId LIMIT 1")
    fun observeStudent(studentId: String): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE status = 'ACTIVE' ORDER BY class_id, last_name, first_name")
    fun observeAllActive(): Flow<List<StudentEntity>>

    @Query("UPDATE students SET class_id = :newClassId WHERE student_id = :studentId")
    suspend fun moveStudent(studentId: String, newClassId: String)

    @Query("UPDATE students SET status = 'ARCHIVED' WHERE student_id = :studentId")
    suspend fun archiveStudent(studentId: String)

    @Query("DELETE FROM students WHERE student_id = :studentId")
    suspend fun hardDelete(studentId: String)

    @Query("UPDATE students SET status = 'ACTIVE' WHERE student_id = :studentId")
    suspend fun restoreStudent(studentId: String)

    @Query("SELECT * FROM students WHERE status = 'ARCHIVED' ORDER BY class_id, last_name, first_name")
    fun observeArchived(): Flow<List<StudentEntity>>

    @Query(
        """
        SELECT * FROM students
        WHERE status = 'ARCHIVED' AND created_by_import_batch_id IS NULL
        ORDER BY class_id, last_name, first_name
        """,
    )
    fun observeTeacherArchived(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY class_id, last_name, first_name")
    fun observeAll(): Flow<List<StudentEntity>>

    @Query("DELETE FROM students WHERE status = 'ARCHIVED'")
    suspend fun deleteArchived()

    @Query("""
        SELECT * FROM students WHERE status = 'ACTIVE' AND (
            last_name LIKE '%' || :query || '%' OR first_name LIKE '%' || :query || '%' OR class_id LIKE '%' || :query || '%'
        ) ORDER BY class_id, last_name, first_name
    """)
    suspend fun searchActiveStudents(query: String): List<StudentEntity>

    @Query("DELETE FROM students")
    suspend fun deleteAll()
}
