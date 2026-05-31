/**
 * Room-база приложения и SQL-представления.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.golpom.atmosphere.data.local.dao.BehaviorLogDao
import ru.golpom.atmosphere.data.local.dao.ClassDao
import ru.golpom.atmosphere.data.local.dao.ImportBatchDao
import ru.golpom.atmosphere.data.local.dao.MeetingDao
import ru.golpom.atmosphere.data.local.dao.NotificationDao
import ru.golpom.atmosphere.data.local.dao.ScheduleEntryDao
import ru.golpom.atmosphere.data.local.dao.StudentDao
import ru.golpom.atmosphere.data.local.entity.BehaviorLogEntity
import ru.golpom.atmosphere.data.local.entity.BehaviorLogVisibleEntity
import ru.golpom.atmosphere.data.local.entity.ClassEntity
import ru.golpom.atmosphere.data.local.entity.ImportBatchEntity
import ru.golpom.atmosphere.data.local.entity.MeetingEntity
import ru.golpom.atmosphere.data.local.entity.NotificationEntity
import ru.golpom.atmosphere.data.local.entity.ScheduleEntryEntity
import ru.golpom.atmosphere.data.local.entity.StudentEntity

@Database(
    entities = [
        ClassEntity::class,
        StudentEntity::class,
        BehaviorLogEntity::class,
        ImportBatchEntity::class,
        ScheduleEntryEntity::class,
        MeetingEntity::class,
        NotificationEntity::class,
    ],
    version = 7,
    exportSchema = false,
    views = [BehaviorLogVisibleEntity::class],
)
abstract class AtmosphereDatabase : RoomDatabase() {
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun behaviorLogDao(): BehaviorLogDao
    abstract fun importBatchDao(): ImportBatchDao
    abstract fun scheduleEntryDao(): ScheduleEntryDao
    abstract fun meetingDao(): MeetingDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        val MIGRATION_3_4 = Migration(3, 4) { db ->
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    type TEXT NOT NULL,
                    title TEXT NOT NULL,
                    body TEXT NOT NULL,
                    timestampMillis INTEGER NOT NULL,
                    isRead INTEGER NOT NULL DEFAULT 0,
                    isDismissed INTEGER NOT NULL DEFAULT 0,
                    relatedId TEXT,
                    metadataJson TEXT
                )""",
            )
        }

        val MIGRATION_4_5 = Migration(4, 5) { db ->
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS import_batches (
                    batch_id TEXT NOT NULL PRIMARY KEY,
                    teacher_profile_id TEXT NOT NULL,
                    teacher_profile_short_id TEXT NOT NULL,
                    teacher_last_name TEXT NOT NULL,
                    teacher_display_name TEXT NOT NULL,
                    source_file_name TEXT NOT NULL,
                    imported_at_millis INTEGER NOT NULL,
                    period_label TEXT NOT NULL,
                    period_from_millis INTEGER,
                    period_to_millis INTEGER,
                    records_in_file INTEGER NOT NULL,
                    inserted_count INTEGER NOT NULL,
                    skipped_duplicate INTEGER NOT NULL,
                    skipped_unknown_student INTEGER NOT NULL,
                    enabled INTEGER NOT NULL DEFAULT 1
                )""",
            )
            db.execSQL("ALTER TABLE behavior_logs ADD COLUMN import_batch_id TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_behavior_logs_import_batch_id ON behavior_logs(import_batch_id)")
            BehaviorLogsVisibleViewSql.recreate(db)
        }

        /** Исправление VIEW на устройствах, где 4→5 уже создал VIEW без backticks / с SELECT *. */
        val MIGRATION_5_6 = Migration(5, 6) { db ->
            BehaviorLogsVisibleViewSql.recreate(db)
        }

        /** Ученики, созданные при импорте завуча, не показываются в режиме учителя. */
        val MIGRATION_6_7 = Migration(6, 7) { db ->
            db.execSQL("ALTER TABLE students ADD COLUMN created_by_import_batch_id TEXT")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_students_created_by_import_batch_id " +
                    "ON students(created_by_import_batch_id)",
            )
            db.execSQL(
                """
                UPDATE students SET created_by_import_batch_id = (
                    SELECT l.import_batch_id FROM behavior_logs l
                    WHERE l.student_id = students.student_id AND l.import_batch_id IS NOT NULL
                    LIMIT 1
                )
                WHERE created_by_import_batch_id IS NULL
                  AND NOT EXISTS (
                    SELECT 1 FROM behavior_logs l
                    WHERE l.student_id = students.student_id AND l.import_batch_id IS NULL
                  )
                  AND EXISTS (
                    SELECT 1 FROM behavior_logs l
                    WHERE l.student_id = students.student_id AND l.import_batch_id IS NOT NULL
                  )
                """.trimIndent(),
            )
        }
    }
}
