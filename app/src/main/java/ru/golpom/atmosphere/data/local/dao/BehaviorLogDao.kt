/**
 * Доступ к логам поведения: вставка и выборка для экрана урока (баланс за день).
 * Data-слой (Room).
 */
package ru.golpom.atmosphere.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.golpom.atmosphere.data.local.entity.BehaviorLogEntity
import ru.golpom.atmosphere.data.local.model.BehaviorTypeCount
import ru.golpom.atmosphere.data.local.model.ClassScore
import ru.golpom.atmosphere.data.local.model.DailyScore
import ru.golpom.atmosphere.data.local.model.LessonStudentRow
import ru.golpom.atmosphere.data.local.model.StudentSubjectBreakdown
import ru.golpom.atmosphere.data.local.model.StudentSubjectScore
import ru.golpom.atmosphere.data.local.model.StudentTotalsInRange

@Dao
interface BehaviorLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: BehaviorLogEntity): Long

    @Query(
        """
        SELECT s.student_id AS studentId,
               s.first_name AS firstName,
               s.last_name AS lastName,
               COALESCE(SUM(l.score_impact), 0) AS balance
        FROM students s
        LEFT JOIN behavior_logs l ON l.student_id = s.student_id
            AND l.subject_key = :subjectKey
            AND l.timestamp >= :dayStartMillis
            AND l.timestamp < :dayEndMillis
        WHERE s.class_id = :classId AND s.status = 'ACTIVE'
        GROUP BY s.student_id, s.first_name, s.last_name
        ORDER BY s.last_name, s.first_name
        """,
    )
    fun observeLessonRows(
        classId: String,
        subjectKey: String,
        dayStartMillis: Long,
        dayEndMillis: Long,
    ): Flow<List<LessonStudentRow>>

    @Query(
        """
        SELECT s.student_id AS studentId,
               s.first_name AS firstName,
               s.last_name AS lastName,
               COALESCE(SUM(l.score_impact), 0) AS balance
        FROM students s
        LEFT JOIN behavior_logs l ON l.student_id = s.student_id
            AND l.subject_key = :subjectKey
            AND l.timestamp >= :dayStartMillis
            AND l.timestamp < :dayEndMillis
            AND l.import_batch_id IS NULL
        WHERE s.class_id = :classId AND s.status = 'ACTIVE'
          AND s.created_by_import_batch_id IS NULL
        GROUP BY s.student_id, s.first_name, s.last_name
        ORDER BY s.last_name, s.first_name
        """,
    )
    fun observeTeacherLessonRows(
        classId: String,
        subjectKey: String,
        dayStartMillis: Long,
        dayEndMillis: Long,
    ): Flow<List<LessonStudentRow>>

    @Query(
        """
        SELECT * FROM behavior_logs
        WHERE class_id = :classId AND subject_key = :subjectKey
        ORDER BY timestamp ASC
        """,
    )
    suspend fun listByClassAndSubject(classId: String, subjectKey: String): List<BehaviorLogEntity>

    @Query(
        """
        SELECT * FROM behavior_logs
        WHERE class_id = :classId AND subject_key = :subjectKey
          AND import_batch_id IS NULL
        ORDER BY timestamp ASC
        """,
    )
    suspend fun listTeacherByClassAndSubject(classId: String, subjectKey: String): List<BehaviorLogEntity>

    @Query(
        """
        SELECT * FROM behavior_logs
        WHERE student_id = :studentId
        ORDER BY timestamp DESC
        LIMIT 250
        """,
    )
    fun observeLogsForStudent(studentId: String): Flow<List<BehaviorLogEntity>>

    @Query(
        """
        SELECT * FROM behavior_logs
        WHERE student_id = :studentId AND import_batch_id IS NULL
        ORDER BY timestamp DESC
        LIMIT 250
        """,
    )
    fun observeTeacherLogsForStudent(studentId: String): Flow<List<BehaviorLogEntity>>

    @Query("""
        SELECT l.student_id AS studentId, COALESCE(SUM(l.score_impact), 0) AS totalScore
        FROM behavior_logs l
        WHERE l.class_id = :classId AND l.subject_key = :subjectKey
          AND l.import_batch_id IS NULL
        GROUP BY l.student_id
    """)
    suspend fun getScoresByClassAndSubject(classId: String, subjectKey: String): List<StudentSubjectScore>

    @Query("""
        SELECT l.student_id AS studentId, COALESCE(SUM(l.score_impact), 0) AS totalScore
        FROM behavior_logs l
        WHERE l.class_id = :classId AND l.import_batch_id IS NULL
        GROUP BY l.student_id
    """)
    suspend fun getTotalScoresForClass(classId: String): List<StudentSubjectScore>

    @Query("SELECT COALESCE(SUM(score_impact), 0) FROM behavior_logs")
    fun observeTotalScore(): Flow<Int>

    @Query("SELECT COALESCE(SUM(score_impact), 0) FROM behavior_logs WHERE import_batch_id IS NULL")
    fun observeTeacherTotalScore(): Flow<Int>

    @Query(
        """
        SELECT * FROM behavior_logs_visible
        WHERE student_id = :studentId
        ORDER BY timestamp DESC
        LIMIT 250
        """,
    )
    fun observeDeputyLogsForStudent(studentId: String): Flow<List<ru.golpom.atmosphere.data.local.entity.BehaviorLogVisibleEntity>>

    @Query("SELECT COALESCE(SUM(score_impact), 0) FROM behavior_logs_visible WHERE timestamp >= :fromMillis AND timestamp < :toMillis AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)")
    suspend fun totalScoreInRange(fromMillis: Long, toMillis: Long, includeLocal: Boolean): Int

    @Query("SELECT COALESCE(SUM(CASE WHEN score_impact > 0 THEN score_impact ELSE 0 END), 0) FROM behavior_logs_visible WHERE timestamp >= :fromMillis AND timestamp < :toMillis AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)")
    suspend fun totalPositiveInRange(fromMillis: Long, toMillis: Long, includeLocal: Boolean): Int

    @Query("SELECT COALESCE(SUM(CASE WHEN score_impact < 0 THEN score_impact ELSE 0 END), 0) FROM behavior_logs_visible WHERE timestamp >= :fromMillis AND timestamp < :toMillis AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)")
    suspend fun totalNegativeInRange(fromMillis: Long, toMillis: Long, includeLocal: Boolean): Int

    @Query("SELECT COUNT(DISTINCT student_id) FROM behavior_logs_visible WHERE timestamp >= :fromMillis AND timestamp < :toMillis AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)")
    suspend fun activeStudentCountInRange(fromMillis: Long, toMillis: Long, includeLocal: Boolean): Int

    @Query("""
        SELECT l.class_id AS classId, COALESCE(SUM(l.score_impact), 0) AS score
        FROM behavior_logs_visible l
        WHERE l.timestamp >= :fromMillis AND l.timestamp < :toMillis
          AND (:includeLocal = 1 OR l.import_batch_id IS NOT NULL)
        GROUP BY l.class_id
        ORDER BY score DESC
    """)
    suspend fun classScoresInRange(fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<ClassScore>

    @Query("""
        SELECT l.class_id AS classId, COALESCE(SUM(DISTINCT CASE WHEN l.score_impact > 0 THEN l.score_impact ELSE 0 END), 0) AS score
        FROM behavior_logs_visible l
        WHERE l.timestamp >= :fromMillis AND l.timestamp < :toMillis
          AND (:includeLocal = 1 OR l.import_batch_id IS NOT NULL)
        GROUP BY l.class_id
        ORDER BY score DESC
    """)
    suspend fun classPositiveScoresInRange(fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<ClassScore>

    @Query("""
        SELECT l.class_id AS classId, COALESCE(SUM(DISTINCT CASE WHEN l.score_impact < 0 THEN l.score_impact ELSE 0 END), 0) AS score
        FROM behavior_logs_visible l
        WHERE l.timestamp >= :fromMillis AND l.timestamp < :toMillis
          AND (:includeLocal = 1 OR l.import_batch_id IS NOT NULL)
        GROUP BY l.class_id
        ORDER BY score ASC
    """)
    suspend fun classNegativeScoresInRange(fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<ClassScore>

    @Query("""
        SELECT l.student_id AS studentId, COALESCE(SUM(l.score_impact), 0) AS totalScore
        FROM behavior_logs_visible l
        WHERE l.timestamp >= :fromMillis AND l.timestamp < :toMillis
          AND (:includeLocal = 1 OR l.import_batch_id IS NOT NULL)
        GROUP BY l.student_id
        ORDER BY totalScore DESC
    """)
    suspend fun studentScoresInRange(fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<StudentSubjectScore>

    @Query("""
        SELECT l.student_id AS studentId, COALESCE(SUM(l.score_impact), 0) AS totalScore
        FROM behavior_logs_visible l
        WHERE l.timestamp >= :fromMillis AND l.timestamp < :toMillis AND l.score_impact < 0
          AND (:includeLocal = 1 OR l.import_batch_id IS NOT NULL)
        GROUP BY l.student_id
        ORDER BY totalScore ASC
    """)
    suspend fun studentNegativeScoresInRange(fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<StudentSubjectScore>

    @Query("""
        -- dayOfWeek: 1=Пн .. 7=Вс (по UTC дате timestamp)
        SELECT l.class_id AS classId,
               ((CAST(strftime('%w', datetime(l.timestamp / 1000, 'unixepoch')) AS INTEGER) + 6) % 7) + 1 AS dayOfWeek,
               COALESCE(SUM(l.score_impact), 0) AS score
        FROM behavior_logs_visible l
        WHERE l.timestamp >= :fromMillis AND l.timestamp < :toMillis
          AND (:includeLocal = 1 OR l.import_batch_id IS NOT NULL)
        GROUP BY l.class_id, dayOfWeek
        ORDER BY l.class_id, dayOfWeek
    """)
    suspend fun heatmapByClassAndDay(fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<ru.golpom.atmosphere.data.local.model.HeatmapCell>

    @Query("""
        SELECT l.timestamp / 86400000 AS day, COALESCE(SUM(l.score_impact), 0) AS score
        FROM behavior_logs_visible l
        WHERE l.timestamp >= :fromMillis AND l.timestamp < :toMillis
          AND (:includeLocal = 1 OR l.import_batch_id IS NOT NULL)
        GROUP BY day
        ORDER BY day ASC
    """)
    suspend fun dailyScoresInRange(fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<DailyScore>

    @Query("""
        SELECT (timestamp / 86400000) AS day, COALESCE(SUM(score_impact), 0) AS score
        FROM behavior_logs_visible
        WHERE timestamp >= :fromMillis AND timestamp < :toMillis
          AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)
        GROUP BY day ORDER BY day ASC
    """)
    suspend fun dailyTotalsInRange(fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<DailyScore>

    @Query("""
        SELECT (timestamp / 86400000) AS day, COUNT(*) AS score
        FROM behavior_logs_visible
        WHERE timestamp >= :fromMillis AND timestamp < :toMillis
          AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)
        GROUP BY day ORDER BY day ASC
    """)
    suspend fun dailyCountsInRange(fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<ru.golpom.atmosphere.data.local.model.DailyScore>

    @Query("SELECT COALESCE(SUM(score_impact), 0) FROM behavior_logs_visible WHERE class_id = :classId AND timestamp >= :fromMillis AND timestamp < :toMillis AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)")
    suspend fun classTotalScoreInRange(classId: String, fromMillis: Long, toMillis: Long, includeLocal: Boolean): Int

    @Query("SELECT COALESCE(SUM(score_impact), 0) FROM behavior_logs_visible WHERE class_id = :classId AND timestamp >= :fromMillis AND timestamp < :toMillis AND score_impact > 0 AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)")
    suspend fun classTotalPositiveInRange(classId: String, fromMillis: Long, toMillis: Long, includeLocal: Boolean): Int

    @Query("SELECT COALESCE(SUM(score_impact), 0) FROM behavior_logs_visible WHERE class_id = :classId AND timestamp >= :fromMillis AND timestamp < :toMillis AND score_impact < 0 AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)")
    suspend fun classTotalNegativeInRange(classId: String, fromMillis: Long, toMillis: Long, includeLocal: Boolean): Int

    @Query("""
        SELECT (timestamp / 86400000) AS day, COALESCE(SUM(score_impact), 0) AS score
        FROM behavior_logs_visible
        WHERE class_id = :classId AND timestamp >= :fromMillis AND timestamp < :toMillis
          AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)
        GROUP BY day ORDER BY day ASC
    """)
    suspend fun classDailyScoresInRange(classId: String, fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<DailyScore>

    @Query("""
        SELECT s.student_id AS studentId, COALESCE(SUM(l.score_impact), 0) AS totalScore
        FROM students s
        LEFT JOIN behavior_logs_visible l ON l.student_id = s.student_id
            AND l.timestamp >= :fromMillis AND l.timestamp < :toMillis
            AND (:includeLocal = 1 OR l.import_batch_id IS NOT NULL)
        WHERE s.class_id = :classId AND s.status = 'ACTIVE'
        GROUP BY s.student_id
        ORDER BY totalScore DESC
    """)
    suspend fun classStudentScoresInRange(classId: String, fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<StudentSubjectScore>

    @Query("""
        SELECT s.student_id AS studentId, COALESCE(SUM(l.score_impact), 0) AS totalScore
        FROM students s
        LEFT JOIN behavior_logs_visible l ON l.student_id = s.student_id
            AND l.timestamp >= :fromMillis AND l.timestamp < :toMillis AND l.score_impact < 0
            AND (:includeLocal = 1 OR l.import_batch_id IS NOT NULL)
        WHERE s.class_id = :classId AND s.status = 'ACTIVE'
        GROUP BY s.student_id
        ORDER BY totalScore ASC
    """)
    suspend fun classStudentNegativeScoresInRange(classId: String, fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<StudentSubjectScore>

    @Query("""
        SELECT s.student_id AS studentId, s.first_name AS firstName, s.last_name AS lastName,
               l.subject_key AS subjectKey, COALESCE(SUM(l.score_impact), 0) AS score
        FROM students s
        INNER JOIN behavior_logs_visible l ON l.student_id = s.student_id
            AND l.timestamp >= :fromMillis AND l.timestamp < :toMillis
            AND (:includeLocal = 1 OR l.import_batch_id IS NOT NULL)
        WHERE s.class_id = :classId AND s.status = 'ACTIVE'
        GROUP BY s.student_id, l.subject_key
        ORDER BY s.last_name, s.first_name, l.subject_key
    """)
    suspend fun studentSubjectScoresInRange(classId: String, fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<ru.golpom.atmosphere.data.local.model.StudentSubjectCell>

    @Query("""
        SELECT (l.timestamp / 86400000) AS day, COALESCE(SUM(l.score_impact), 0) AS score
        FROM behavior_logs_visible l
        WHERE l.student_id = :studentId AND l.timestamp >= :fromMillis AND l.timestamp < :toMillis
          AND (:includeLocal = 1 OR l.import_batch_id IS NOT NULL)
        GROUP BY day ORDER BY day ASC
    """)
    suspend fun studentDailyScoresInRange(studentId: String, fromMillis: Long, toMillis: Long, includeLocal: Boolean): List<DailyScore>

    @Query("""
        SELECT
            COALESCE(SUM(score_impact), 0) AS totalScore,
            COALESCE(SUM(CASE WHEN score_impact > 0 THEN score_impact ELSE 0 END), 0) AS totalPositive,
            COALESCE(SUM(CASE WHEN score_impact < 0 THEN score_impact ELSE 0 END), 0) AS totalNegative,
            COUNT(*) AS eventCount
        FROM behavior_logs_visible
        WHERE student_id = :studentId AND timestamp >= :fromMillis AND timestamp < :toMillis
          AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)
    """)
    suspend fun studentTotalsInRange(studentId: String, fromMillis: Long, toMillis: Long, includeLocal: Boolean): StudentTotalsInRange?

    @Query("""
        SELECT subject_key AS subjectKey,
            COALESCE(SUM(score_impact), 0) AS totalScore,
            COALESCE(SUM(CASE WHEN score_impact > 0 THEN score_impact ELSE 0 END), 0) AS positiveScore,
            COALESCE(SUM(CASE WHEN score_impact < 0 THEN score_impact ELSE 0 END), 0) AS negativeScore,
            COALESCE(SUM(CASE WHEN score_impact > 0 THEN 1 ELSE 0 END), 0) AS positiveCount,
            COALESCE(SUM(CASE WHEN score_impact < 0 THEN 1 ELSE 0 END), 0) AS negativeCount
        FROM behavior_logs_visible
        WHERE student_id = :studentId AND timestamp >= :fromMillis AND timestamp < :toMillis
          AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)
        GROUP BY subject_key
        ORDER BY totalScore ASC
    """)
    suspend fun studentSubjectBreakdownInRange(
        studentId: String,
        fromMillis: Long,
        toMillis: Long,
        includeLocal: Boolean,
    ): List<StudentSubjectBreakdown>

    @Query("""
        SELECT behavior_type AS behaviorType, COUNT(*) AS count
        FROM behavior_logs_visible
        WHERE student_id = :studentId AND timestamp >= :fromMillis AND timestamp < :toMillis AND score_impact < 0
          AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)
        GROUP BY behavior_type
        ORDER BY count DESC
        LIMIT 8
    """)
    suspend fun studentViolationTypesInRange(
        studentId: String,
        fromMillis: Long,
        toMillis: Long,
        includeLocal: Boolean,
    ): List<BehaviorTypeCount>

    @Query("""
        SELECT behavior_type AS behaviorType, COUNT(*) AS count
        FROM behavior_logs_visible
        WHERE student_id = :studentId AND timestamp >= :fromMillis AND timestamp < :toMillis AND score_impact > 0
          AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)
        GROUP BY behavior_type
        ORDER BY count DESC
        LIMIT 8
    """)
    suspend fun studentMeritTypesInRange(
        studentId: String,
        fromMillis: Long,
        toMillis: Long,
        includeLocal: Boolean,
    ): List<BehaviorTypeCount>

    @Query("""
        SELECT * FROM behavior_logs_visible
        WHERE student_id = :studentId AND timestamp >= :fromMillis AND timestamp < :toMillis
          AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)
        ORDER BY timestamp DESC
        LIMIT 600
    """)
    suspend fun studentLogsInRangeVisible(
        studentId: String,
        fromMillis: Long,
        toMillis: Long,
        includeLocal: Boolean,
    ): List<ru.golpom.atmosphere.data.local.entity.BehaviorLogVisibleEntity>

    @Query("""
        SELECT * FROM behavior_logs_visible
        WHERE class_id = :classId AND timestamp >= :fromMillis AND timestamp < :toMillis
          AND (:includeLocal = 1 OR import_batch_id IS NOT NULL)
        ORDER BY timestamp DESC
        LIMIT 3000
    """)
    suspend fun classLogsInRangeVisible(
        classId: String,
        fromMillis: Long,
        toMillis: Long,
        includeLocal: Boolean,
    ): List<ru.golpom.atmosphere.data.local.entity.BehaviorLogVisibleEntity>

    @Query("DELETE FROM behavior_logs WHERE id = :logId")
    suspend fun deleteById(logId: Long)

    @Query("DELETE FROM behavior_logs WHERE student_id = :studentId")
    suspend fun deleteByStudentId(studentId: String)

    @Query("DELETE FROM behavior_logs")
    suspend fun deleteAll()

    @Query("DELETE FROM behavior_logs WHERE import_batch_id = :batchId")
    suspend fun deleteByImportBatchId(batchId: String)

    @Query("""
        SELECT * FROM behavior_logs
        WHERE student_id = :studentId
        AND timestamp >= :fromMillis AND timestamp < :toMillis
        AND import_batch_id IS NULL
        ORDER BY timestamp ASC
    """)
    suspend fun exportStudentLogs(
        studentId: String,
        fromMillis: Long,
        toMillis: Long,
    ): List<BehaviorLogEntity>

    @Query("""
        SELECT * FROM behavior_logs
        WHERE class_id = :classId
        AND timestamp >= :fromMillis AND timestamp < :toMillis
        AND import_batch_id IS NULL
        ORDER BY timestamp ASC
    """)
    suspend fun exportClassLogs(
        classId: String,
        fromMillis: Long,
        toMillis: Long,
    ): List<BehaviorLogEntity>

    @Query("""
        SELECT * FROM behavior_logs
        WHERE class_id IN (:classIds)
        AND timestamp >= :fromMillis AND timestamp < :toMillis
        AND import_batch_id IS NULL
        ORDER BY timestamp ASC
    """)
    suspend fun exportClassesLogs(
        classIds: List<String>,
        fromMillis: Long,
        toMillis: Long,
    ): List<BehaviorLogEntity>

    @Query("""
        SELECT * FROM behavior_logs
        WHERE subject_key = :subjectKey
        AND class_id IN (:classIds)
        AND timestamp >= :fromMillis AND timestamp < :toMillis
        AND import_batch_id IS NULL
        ORDER BY timestamp ASC
    """)
    suspend fun exportSubjectLogs(
        subjectKey: String,
        classIds: List<String>,
        fromMillis: Long,
        toMillis: Long,
    ): List<BehaviorLogEntity>

    @Query("""
        SELECT * FROM behavior_logs
        WHERE timestamp >= :fromMillis AND timestamp < :toMillis
        AND import_batch_id IS NULL
        ORDER BY timestamp ASC
    """)
    suspend fun exportAllLogs(
        fromMillis: Long,
        toMillis: Long,
    ): List<BehaviorLogEntity>
}
