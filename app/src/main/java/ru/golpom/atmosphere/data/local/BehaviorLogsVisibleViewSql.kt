/**
 * SQL для представления видимых отметок поведения.
 * Data-слой (Room).
 */
package ru.golpom.atmosphere.data.local

/**
 * SQL VIEW должен побайтно совпадать с тем, что Room ожидает от [ru.golpom.atmosphere.data.local.entity.BehaviorLogVisibleEntity]
 * (включая обратные кавычки вокруг имени VIEW).
 */
internal object BehaviorLogsVisibleViewSql {
    const val CREATE =
        """CREATE VIEW `behavior_logs_visible` AS SELECT id, timestamp, student_id, class_id, subject_key, behavior_type, score_impact, comment, import_batch_id
        FROM behavior_logs
        WHERE import_batch_id IS NULL
           OR import_batch_id IN (SELECT batch_id FROM import_batches WHERE enabled = 1)"""

    fun recreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("DROP VIEW IF EXISTS behavior_logs_visible")
        db.execSQL(CREATE)
    }
}
