/**
 * Перевод классов в новый учебный год одной транзакцией.
 * Data-слой (Room).
 *
 * Затрагиваются только собственные активные ученики учителя
 * (created_by_import_batch_id IS NULL): ученики из отчётов других учителей
 * и архив остаются на месте.
 *
 * Отметки (behavior_logs) не переписываются: у них остаётся класс,
 * в котором ученик учился в момент отметки, поэтому отчёты за прошлый год не меняются.
 */
package ru.golpom.atmosphere.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ru.golpom.atmosphere.data.local.entity.ClassEntity
import ru.golpom.atmosphere.data.local.model.ClassStudentCount
import ru.golpom.atmosphere.domain.schoolyear.GraduateAction
import ru.golpom.atmosphere.domain.schoolyear.SchoolYearRolloverPlan

@Dao
abstract class SchoolYearDao {

    @Query(
        """
        SELECT class_id, COUNT(*) AS student_count FROM students
        WHERE status = 'ACTIVE' AND created_by_import_batch_id IS NULL
        GROUP BY class_id
        """,
    )
    abstract suspend fun countTeacherActiveByClass(): List<ClassStudentCount>

    @Query("SELECT DISTINCT classId FROM schedule_entries")
    abstract suspend fun listScheduleClassIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertClass(entity: ClassEntity)

    @Query(
        """
        UPDATE students SET class_id = :toClassId
        WHERE class_id = :fromClassId AND status = 'ACTIVE' AND created_by_import_batch_id IS NULL
        """,
    )
    abstract suspend fun moveTeacherActiveStudents(fromClassId: String, toClassId: String): Int

    @Query(
        """
        UPDATE students SET status = 'ARCHIVED'
        WHERE class_id = :classId AND status = 'ACTIVE' AND created_by_import_batch_id IS NULL
        """,
    )
    abstract suspend fun archiveTeacherActiveStudents(classId: String): Int

    /** Отметки удалённых учеников уходят каскадом (FK behavior_logs.student_id). */
    @Query(
        """
        DELETE FROM students
        WHERE class_id = :classId AND status = 'ACTIVE' AND created_by_import_batch_id IS NULL
        """,
    )
    abstract suspend fun deleteTeacherActiveStudents(classId: String): Int

    @Query("UPDATE schedule_entries SET classId = :toClassId WHERE classId = :fromClassId")
    abstract suspend fun moveSchedule(fromClassId: String, toClassId: String): Int

    @Query("DELETE FROM schedule_entries WHERE classId = :classId")
    abstract suspend fun deleteSchedule(classId: String): Int

    /**
     * Сначала выпуск, затем перевод от старших к младшим (план уже так отсортирован):
     * 10-А уезжает в 11-А раньше, чем 9-А займёт место 10-А, поэтому классы не смешиваются.
     */
    @Transaction
    open suspend fun applyRollover(
        plan: SchoolYearRolloverPlan,
        graduateAction: GraduateAction,
        shiftSchedule: Boolean,
    ): RolloverResult {
        var graduated = 0
        var promoted = 0

        for (g in plan.graduations) {
            for (src in g.studentSourceIds) {
                graduated += when (graduateAction) {
                    GraduateAction.ARCHIVE -> archiveTeacherActiveStudents(src)
                    GraduateAction.DELETE -> deleteTeacherActiveStudents(src)
                }
            }
            if (shiftSchedule) {
                for (src in g.scheduleSourceIds) deleteSchedule(src)
            }
        }

        for (p in plan.promotions) {
            insertClass(ClassEntity(classId = p.toClassId))
            for (src in p.studentSourceIds) {
                promoted += moveTeacherActiveStudents(src, p.toClassId)
            }
            if (shiftSchedule) {
                for (src in p.scheduleSourceIds) moveSchedule(src, p.toClassId)
            }
        }

        return RolloverResult(promotedStudents = promoted, graduatedStudents = graduated)
    }

    data class RolloverResult(val promotedStudents: Int, val graduatedStudents: Int)
}
