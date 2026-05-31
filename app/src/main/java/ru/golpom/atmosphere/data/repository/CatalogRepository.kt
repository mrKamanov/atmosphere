/**
 * Единая точка чтения/записи каталога (классы, урок, логи) для UI.
 * Data-слой; границы суток для урока — локальная календарная дата устройства.
 */
package ru.golpom.atmosphere.data.repository

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.Year
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.golpom.atmosphere.data.config.ClassConfigFileDto
import ru.golpom.atmosphere.data.config.ClassConfigJsonCodec
import ru.golpom.atmosphere.data.config.ClassConfigStudentDto
import ru.golpom.atmosphere.data.csv.BehaviorLogImportRow
import ru.golpom.atmosphere.data.csv.BehaviorLogImportStats
import ru.golpom.atmosphere.data.csv.ParsedStudentRow
import ru.golpom.atmosphere.data.csv.StudentImportSummary
import ru.golpom.atmosphere.domain.student.StudentIdentity
import ru.golpom.atmosphere.data.local.dao.BehaviorLogDao
import ru.golpom.atmosphere.data.local.dao.ClassDao
import ru.golpom.atmosphere.data.local.dao.ImportBatchDao
import ru.golpom.atmosphere.data.local.dao.MeetingDao
import ru.golpom.atmosphere.data.local.dao.NotificationDao
import ru.golpom.atmosphere.data.local.dao.ScheduleEntryDao
import ru.golpom.atmosphere.data.local.dao.StudentDao
import ru.golpom.atmosphere.data.local.entity.BehaviorLogEntity
import ru.golpom.atmosphere.data.local.entity.toBehaviorLogEntity
import ru.golpom.atmosphere.data.local.entity.ClassEntity
import ru.golpom.atmosphere.data.local.entity.ImportBatchEntity
import ru.golpom.atmosphere.data.local.entity.MeetingEntity
import ru.golpom.atmosphere.data.preferences.UserPreferencesRepository
import ru.golpom.atmosphere.data.local.entity.ScheduleEntryEntity
import ru.golpom.atmosphere.data.local.entity.NotificationEntity
import ru.golpom.atmosphere.data.local.entity.StudentEntity
import ru.golpom.atmosphere.data.local.model.BehaviorTypeCount
import ru.golpom.atmosphere.data.local.model.DailyScore
import ru.golpom.atmosphere.data.local.model.LessonStudentRow
import ru.golpom.atmosphere.data.local.model.StudentSubjectBreakdown
import ru.golpom.atmosphere.data.local.model.StudentSubjectScore
import ru.golpom.atmosphere.data.local.model.StudentTotalsInRange
import java.nio.charset.StandardCharsets

@Singleton
class CatalogRepository @Inject constructor(
    private val classDao: ClassDao,
    private val studentDao: StudentDao,
    private val behaviorLogDao: BehaviorLogDao,
    private val importBatchDao: ImportBatchDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val scheduleEntryDao: ScheduleEntryDao,
    private val meetingDao: MeetingDao,
    private val notificationDao: NotificationDao,
) {
    fun observeClasses(): Flow<List<ClassEntity>> = classDao.observeAll()

    fun observeTeacherClasses(): Flow<List<ClassEntity>> = classDao.observeForTeacher()

    fun observeActiveByClass(classId: String): Flow<List<StudentEntity>> =
        studentDao.observeActiveByClass(classId)

    fun observeTeacherActiveByClass(classId: String): Flow<List<StudentEntity>> =
        studentDao.observeTeacherActiveByClass(classId)

    fun observeAllStudents(): Flow<List<StudentEntity>> =
        studentDao.observeAllActive()

    fun observeTeacherStudents(): Flow<List<StudentEntity>> =
        studentDao.observeTeacherAllActive()

    /** Все «свои» ученики (активные + архив), без созданных при импорте завуча. */
    fun observeTeacherStudentsAll(): Flow<List<StudentEntity>> =
        studentDao.observeTeacherAll()

    fun observeStudentCount(): kotlinx.coroutines.flow.Flow<Long> = studentDao.observeCount()

    fun observeTeacherStudentCount(): kotlinx.coroutines.flow.Flow<Long> =
        studentDao.observeTeacherCount()

    fun observeStudent(studentId: String): Flow<StudentEntity?> =
        studentDao.observeStudent(studentId)

    fun observeBehaviorLogsForStudent(studentId: String): Flow<List<BehaviorLogEntity>> =
        behaviorLogDao.observeTeacherLogsForStudent(studentId)

    fun observeDeputyBehaviorLogsForStudent(studentId: String): Flow<List<BehaviorLogEntity>> =
        behaviorLogDao.observeDeputyLogsForStudent(studentId)
            .map { logs -> logs.map { it.toBehaviorLogEntity() } }

    fun observeLessonRows(
        classId: String,
        subjectKey: String,
    ): Flow<List<LessonStudentRow>> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = start + Duration.ofDays(1).toMillis()
        return behaviorLogDao.observeTeacherLessonRows(classId, subjectKey, start, end)
    }

    suspend fun getBehaviorLogsForClassAndSubject(
        classId: String,
        subjectKey: String,
    ): List<BehaviorLogEntity> =
        behaviorLogDao.listTeacherByClassAndSubject(classId, subjectKey)

    suspend fun logBehavior(
        studentId: String,
        classId: String,
        subjectKey: String,
        behaviorType: String,
        scoreImpact: Int,
        comment: String = "",
    ) {
        behaviorLogDao.insert(
            BehaviorLogEntity(
                timestamp = System.currentTimeMillis(),
                studentId = studentId,
                classId = classId,
                subjectKey = subjectKey,
                behaviorType = behaviorType,
                scoreImpact = scoreImpact,
                comment = comment,
            ),
        )
    }

    /**
     * Вставка строк из CSV: дубликаты по составному уникальному индексу игнорируются (Room IGNORE).
     * Если в строке есть ФИО — ученик ищется/создаётся по классу + имени (слияние с разных устройств).
     */
    suspend fun importBehaviorLogs(
        rows: List<BehaviorLogImportRow>,
        importBatchId: String? = null,
    ): BehaviorLogImportStats {
        var inserted = 0
        var skippedUnknownStudent = 0
        var skippedDuplicate = 0
        val newStudentIds = mutableSetOf<String>()
        for (row in rows) {
            val e = row.log
            val resolved = resolveStudentIdForImport(
                classId = e.classId,
                sourceStudentId = e.studentId,
                firstName = row.firstName,
                lastName = row.lastName,
                importBatchId = importBatchId,
            )
            val localStudentId = resolved?.studentId
            if (resolved?.created == true && localStudentId != null) {
                newStudentIds.add(localStudentId)
            }
            if (localStudentId == null) {
                skippedUnknownStudent++
                continue
            }
            val tagged = e.copy(studentId = localStudentId, importBatchId = importBatchId)
            val rowId = behaviorLogDao.insert(tagged)
            if (rowId == -1L) {
                skippedDuplicate++
            } else {
                inserted++
            }
        }
        return BehaviorLogImportStats(
            inserted = inserted,
            skippedUnknownStudent = skippedUnknownStudent,
            skippedDuplicate = skippedDuplicate,
            studentsCreated = newStudentIds.size,
        )
    }

    /**
     * Сопоставление ученика при импорте: сначала класс + ФИО, иначе старый student_id из файла.
     */
    data class ResolvedStudent(val studentId: String, val created: Boolean)

    suspend fun resolveStudentIdForImport(
        classId: String,
        sourceStudentId: String,
        firstName: String?,
        lastName: String?,
        importBatchId: String? = null,
    ): ResolvedStudent? {
        if (StudentIdentity.namesUsable(firstName, lastName)) {
            return findOrCreateStudentByIdentity(
                classId,
                firstName!!,
                lastName!!,
                createdByImportBatchId = importBatchId,
            )
        }
        return sourceStudentId.takeIf { studentDao.exists(it) }?.let { ResolvedStudent(it, created = false) }
    }

    suspend fun findOrCreateStudentByIdentity(
        classId: String,
        firstName: String,
        lastName: String,
        createdByImportBatchId: String? = null,
    ): ResolvedStudent {
        val normalizedClass = StudentIdentity.normalizeClassId(classId)
        val fn = StudentIdentity.normalizeName(firstName)
        val ln = StudentIdentity.normalizeName(lastName)
        classDao.insert(ClassEntity(normalizedClass))
        studentDao.findByClassAndName(normalizedClass, fn, ln)?.let {
            return ResolvedStudent(it.studentId, created = false)
        }
        val canonicalId = StudentIdentity.canonicalStudentId(normalizedClass, fn, ln)
        if (studentDao.exists(canonicalId)) {
            return ResolvedStudent(canonicalId, created = false)
        }
        val rowId = studentDao.insertIgnore(
            StudentEntity(
                studentId = canonicalId,
                firstName = fn,
                lastName = ln,
                classId = normalizedClass,
                status = "ACTIVE",
                createdByImportBatchId = createdByImportBatchId,
            ),
        )
        return ResolvedStudent(canonicalId, created = rowId != -1L)
    }

    suspend fun buildBehaviorLogExportRows(logs: List<BehaviorLogEntity>): List<BehaviorLogImportRow> {
        if (logs.isEmpty()) return emptyList()
        val ids = logs.map { it.studentId }.distinct()
        val students = studentDao.listByIds(ids).associateBy { it.studentId }
        return logs.map { log ->
            val student = students[log.studentId]
            BehaviorLogImportRow(
                log = log,
                firstName = student?.firstName,
                lastName = student?.lastName,
            )
        }
    }

    fun observeImportBatches(): Flow<List<ImportBatchEntity>> = importBatchDao.observeAll()

    suspend fun upsertImportBatch(entity: ImportBatchEntity) {
        importBatchDao.upsert(entity)
    }

    suspend fun setImportBatchEnabled(batchId: String, enabled: Boolean) {
        importBatchDao.setEnabled(batchId, enabled)
    }

    suspend fun deleteImportBatch(batchId: String) {
        behaviorLogDao.deleteByImportBatchId(batchId)
        studentDao.deleteCreatedByImportBatch(batchId)
        importBatchDao.deleteById(batchId)
    }

    /**
     * Слияние учеников: только новые `student_id` (IGNORE); класс создаётся при необходимости.
     * Для строк без ID — ключ `Год-класс-порядковыйНомер` в пределах импорта и текущей БД.
     */
    suspend fun mergeStudentsFromParsedRows(
        rows: List<ParsedStudentRow>,
        parseWarningsCount: Int = 0,
    ): StudentImportSummary {
        if (rows.isEmpty()) {
            return StudentImportSummary(0, 0, parseWarningsCount)
        }
        val seqByClass = mutableMapOf<String, Int>()
        var inserted = 0
        var skippedExisting = 0
        for (row in rows) {
            classDao.insert(ClassEntity(row.classId))
            val id = row.studentId?.takeIf { it.isNotBlank() }
                ?: nextGeneratedStudentId(seqByClass, row.classId)
            val status = if (row.status.uppercase() == "ARCHIVED") "ARCHIVED" else "ACTIVE"
            val entity = StudentEntity(
                studentId = id,
                firstName = row.firstName,
                lastName = row.lastName,
                classId = row.classId,
                status = status,
            )
            val rowId = studentDao.insertIgnore(entity)
            if (rowId == -1L) {
                skippedExisting++
            } else {
                inserted++
            }
        }
        return StudentImportSummary(
            inserted = inserted,
            skippedExisting = skippedExisting,
            parseWarnings = parseWarningsCount,
        )
    }

    suspend fun mergeStudentsFromClassConfigJson(text: String): StudentImportSummary {
        val dto = ClassConfigJsonCodec.decode(text)
        val rows = dto.students.map { s ->
            ParsedStudentRow(
                studentId = s.student_id,
                firstName = s.first_name,
                lastName = s.last_name,
                classId = s.class_id,
                status = s.status,
            )
        }
        return mergeStudentsFromParsedRows(rows, 0)
    }

    suspend fun buildClassConfigExport(fileName: String): Pair<String, ByteArray> {
        val list = studentDao.listAll()
        val dto = ClassConfigFileDto(
            students = list.map { s ->
                ClassConfigStudentDto(
                    student_id = s.studentId,
                    first_name = s.firstName,
                    last_name = s.lastName,
                    class_id = s.classId,
                    status = s.status,
                )
            },
        )
        val json = ClassConfigJsonCodec.encode(dto)
        return fileName to json.toByteArray(StandardCharsets.UTF_8)
    }

    suspend fun deleteBehaviorLog(logId: Long) {
        behaviorLogDao.deleteById(logId)
    }

    suspend fun clearBehaviorLogsForStudent(studentId: String) {
        behaviorLogDao.deleteByStudentId(studentId)
    }

    fun observeTotalScore(): kotlinx.coroutines.flow.Flow<Int> = behaviorLogDao.observeTotalScore()

    fun observeTeacherTotalScore(): kotlinx.coroutines.flow.Flow<Int> =
        behaviorLogDao.observeTeacherTotalScore()

    fun observeScheduleByDay(dayOfWeek: Int): Flow<List<ScheduleEntryEntity>> =
        scheduleEntryDao.observeByDay(dayOfWeek)

    fun observeAllSchedule(): Flow<List<ScheduleEntryEntity>> =
        scheduleEntryDao.observeAll()

    suspend fun saveScheduleEntry(entry: ScheduleEntryEntity) {
        scheduleEntryDao.insert(entry)
    }

    suspend fun updateScheduleEntry(entry: ScheduleEntryEntity) {
        scheduleEntryDao.update(entry)
    }

    suspend fun deleteScheduleEntry(id: Long) {
        scheduleEntryDao.deleteById(id)
    }

    suspend fun replaceAllSchedule(entries: List<ScheduleEntryEntity>) {
        scheduleEntryDao.deleteAll()
        for (e in entries) {
            scheduleEntryDao.insert(e)
        }
    }

    suspend fun ensureClassesExist(classIds: Set<String>) {
        for (id in classIds) {
            val normalized = StudentIdentity.normalizeClassId(id)
            if (normalized.isNotBlank()) classDao.insert(ClassEntity(classId = normalized))
        }
    }

    suspend fun createClass(classId: String) {
        val normalized = StudentIdentity.normalizeClassId(classId)
        if (normalized.isNotBlank()) classDao.insert(ClassEntity(classId = normalized))
    }

    suspend fun deleteClass(classId: String) {
        classDao.deleteByClassId(classId)
    }

    suspend fun addStudent(studentId: String, firstName: String, lastName: String, classId: String) {
        val fn = StudentIdentity.normalizeName(firstName)
        val ln = StudentIdentity.normalizeName(lastName)
        val cls = StudentIdentity.normalizeClassId(classId)
        if (fn.isBlank() || ln.isBlank() || cls.isBlank()) return
        studentDao.insert(
            StudentEntity(
                studentId = studentId,
                firstName = fn,
                lastName = ln,
                classId = cls,
                status = "ACTIVE",
            ),
        )
    }

    suspend fun moveStudent(studentId: String, newClassId: String) {
        studentDao.moveStudent(studentId, newClassId)
    }

    suspend fun archiveStudent(studentId: String) {
        studentDao.archiveStudent(studentId)
    }

    suspend fun hardDeleteStudent(studentId: String) {
        studentDao.hardDelete(studentId)
    }

    suspend fun restoreStudent(studentId: String) {
        studentDao.restoreStudent(studentId)
    }

    fun observeArchivedStudents(): Flow<List<StudentEntity>> = studentDao.observeArchived()

    fun observeTeacherArchivedStudents(): Flow<List<StudentEntity>> =
        studentDao.observeTeacherArchived()

    fun observeAllStudentsFull(): Flow<List<StudentEntity>> = studentDao.observeAll()

    suspend fun importStudents(entries: List<StudentEntity>, skipDuplicates: Boolean = true): Int {
        val existing = studentDao.listAll()
        var count = 0
        for (e in entries) {
            val dup = existing.any { ex ->
                ex.classId == e.classId &&
                    ex.lastName.equals(e.lastName, ignoreCase = true) &&
                    ex.firstName.equals(e.firstName, ignoreCase = true) &&
                    ex.status == "ACTIVE"
            }
            if (dup && skipDuplicates) continue
            classDao.insert(ClassEntity(e.classId))
            studentDao.insert(e)
            count++
        }
        return count
    }

    fun observeSubjectsByClass(classId: String): Flow<List<String>> =
        scheduleEntryDao.observeSubjectsByClass(classId)

    suspend fun listTeacherClassIds(): List<String> = scheduleEntryDao.listDistinctClassIds()

    suspend fun listTeacherSubjects(): List<String> = scheduleEntryDao.listDistinctSubjects()

    suspend fun getLogsForExport(
        scope: ru.golpom.atmosphere.domain.export.TeacherExportScope,
        fromMillis: Long,
        toMillis: Long,
    ): List<BehaviorLogEntity> = when (scope) {
        is ru.golpom.atmosphere.domain.export.TeacherExportScope.Student ->
            behaviorLogDao.exportStudentLogs(scope.studentId, fromMillis, toMillis)
        is ru.golpom.atmosphere.domain.export.TeacherExportScope.Class ->
            behaviorLogDao.exportClassLogs(scope.classId, fromMillis, toMillis)
        ru.golpom.atmosphere.domain.export.TeacherExportScope.AllMyClasses -> {
            val classIds = scheduleEntryDao.listDistinctClassIds()
            if (classIds.isEmpty()) emptyList()
            else behaviorLogDao.exportClassesLogs(classIds, fromMillis, toMillis)
        }
        is ru.golpom.atmosphere.domain.export.TeacherExportScope.Subject -> {
            val classIds = scheduleEntryDao.listDistinctClassIds()
            if (classIds.isEmpty()) emptyList()
            else behaviorLogDao.exportSubjectLogs(scope.subjectKey, classIds, fromMillis, toMillis)
        }
        ru.golpom.atmosphere.domain.export.TeacherExportScope.AllData ->
            behaviorLogDao.exportAllLogs(fromMillis, toMillis)
    }

    suspend fun getScoresByClassAndSubject(classId: String, subjectKey: String): List<StudentSubjectScore> =
        behaviorLogDao.getScoresByClassAndSubject(classId, subjectKey)

    suspend fun getTotalScoresForClass(classId: String): List<StudentSubjectScore> =
        behaviorLogDao.getTotalScoresForClass(classId)

    fun observeMeetings(): Flow<List<MeetingEntity>> = meetingDao.observeAll()

    fun observeNextMeeting(fromMillis: Long): Flow<MeetingEntity?> = meetingDao.observeNext(fromMillis)

    suspend fun addMeeting(entity: MeetingEntity): Long = meetingDao.insert(entity)

    suspend fun updateMeeting(id: Long, classId: String, dateTimeMillis: Long, topic: String, notes: String) {
        meetingDao.update(id, classId, dateTimeMillis, topic, notes)
    }

    suspend fun deleteMeeting(id: Long) {
        meetingDao.deleteById(id)
    }

    fun observeNotifications(): Flow<List<NotificationEntity>> = notificationDao.observeActive()

    fun observeUnreadNotificationCount(): Flow<Int> = notificationDao.observeUnreadCount()

    suspend fun insertNotification(entity: NotificationEntity): Long =
        notificationDao.insert(entity)

    suspend fun dismissNotification(id: Long) = notificationDao.dismiss(id)

    suspend fun markNotificationRead(id: Long) = notificationDao.markRead(id)

    suspend fun deleteNotification(id: Long) = notificationDao.delete(id)

    suspend fun cleanOldNotifications() {
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val threeDaysAgo = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000
        notificationDao.cleanDismissedOlderThan(weekAgo)
        notificationDao.cleanActiveOlderThan(threeDaysAgo)
    }

    suspend fun clearAllScores() {
        behaviorLogDao.deleteAll()
    }

    suspend fun clearArchive() {
        notificationDao.deleteAllDismissed()
        studentDao.deleteArchived()
    }

    suspend fun clearAllData() {
        notificationDao.deleteAll()
        meetingDao.deleteAll()
        scheduleEntryDao.deleteAll()
        behaviorLogDao.deleteAll()
        importBatchDao.deleteAll()
        studentDao.deleteAll()
        classDao.deleteAll()
    }

    suspend fun getDeputyStats(fromMillis: Long, toMillis: Long): ru.golpom.atmosphere.ui.deputy.DeputyStats {
        val includeLocal = includeLocalDeputyData()
        val classScores = behaviorLogDao.classScoresInRange(fromMillis, toMillis, includeLocal)
        val allStudents = studentDao.observeAllActive().first()
        val dailyTotals = behaviorLogDao.dailyTotalsInRange(fromMillis, toMillis, includeLocal)
        val dailyCounts = behaviorLogDao.dailyCountsInRange(fromMillis, toMillis, includeLocal)
        return ru.golpom.atmosphere.ui.deputy.DeputyStats(
            totalScore = behaviorLogDao.totalScoreInRange(fromMillis, toMillis, includeLocal),
            totalPositive = behaviorLogDao.totalPositiveInRange(fromMillis, toMillis, includeLocal),
            totalNegative = behaviorLogDao.totalNegativeInRange(fromMillis, toMillis, includeLocal),
            activeStudentCount = behaviorLogDao.activeStudentCountInRange(fromMillis, toMillis, includeLocal),
            classCount = classDao.observeAll().first().size,
            studentCount = studentDao.observeCount().first(),
            classScores = classScores,
            dailyScores = behaviorLogDao.dailyScoresInRange(fromMillis, toMillis, includeLocal),
            topPositiveStudents = buildStudentScores(
                students = allStudents,
                scores = behaviorLogDao.studentScoresInRange(fromMillis, toMillis, includeLocal),
                take = 10,
            ),
            topNegativeStudents = buildStudentScores(
                students = allStudents,
                scores = behaviorLogDao.studentNegativeScoresInRange(fromMillis, toMillis, includeLocal),
                take = 10,
            ),
            heatmapData = behaviorLogDao.heatmapByClassAndDay(fromMillis, toMillis, includeLocal),
            parallelScores = computeParallelScores(classScores, allStudents),
            fatigueData = computeFatigue(dailyTotals, dailyCounts),
        )
    }

    private suspend fun includeLocalDeputyData(): Boolean =
        userPreferencesRepository.deputyLocalDataEnabled.first()

    private fun computeFatigue(
        dailyTotals: List<ru.golpom.atmosphere.data.local.model.DailyScore>,
        dailyCounts: List<ru.golpom.atmosphere.data.local.model.DailyScore>,
    ): List<ru.golpom.atmosphere.data.local.model.DayOfWeekScore> {
        val countMap = dailyCounts.associate { it.day to it.score }
        val buckets = dailyTotals.mapNotNull { dt ->
            val count = countMap[dt.day] ?: return@mapNotNull null
            val dow = LocalDate.ofEpochDay(dt.day).dayOfWeek.value // 1=Mon, 7=Sun
            Triple(dow, dt.score, count)
        }
        return buckets.groupBy { it.first }.map { (dow, items) ->
            ru.golpom.atmosphere.data.local.model.DayOfWeekScore(
                dayOfWeek = dow,
                totalScore = items.sumOf { it.second },
                entryCount = items.sumOf { it.third },
            )
        }.sortedBy { it.dayOfWeek }
    }

    private fun computeParallelScores(
        classScores: List<ru.golpom.atmosphere.data.local.model.ClassScore>,
        students: List<ru.golpom.atmosphere.data.local.entity.StudentEntity>,
    ): List<ru.golpom.atmosphere.ui.deputy.ParallelScore> {
        val studentCountByClass = students.groupingBy { it.classId }.eachCount()

        fun extractParallel(classId: String): String =
            classId.takeWhile { it.isDigit() }.ifEmpty { classId }

        return classScores.groupBy { extractParallel(it.classId) }.map { (parallel, scores) ->
            val classIds = scores.map { it.classId }
            ru.golpom.atmosphere.ui.deputy.ParallelScore(
                parallel = parallel,
                totalScore = scores.sumOf { it.score },
                classCount = classIds.size,
                studentCount = classIds.sumOf { studentCountByClass[it] ?: 0 },
            )
        }.sortedByDescending { it.totalScore }
    }
    suspend fun getClassDetailStats(classId: String, fromMillis: Long, toMillis: Long): ru.golpom.atmosphere.ui.deputy.ClassDetailData {
        val includeLocal = includeLocalDeputyData()
        return ru.golpom.atmosphere.ui.deputy.ClassDetailData(
            totalScore = behaviorLogDao.classTotalScoreInRange(classId, fromMillis, toMillis, includeLocal),
            totalPositive = behaviorLogDao.classTotalPositiveInRange(classId, fromMillis, toMillis, includeLocal),
            totalNegative = behaviorLogDao.classTotalNegativeInRange(classId, fromMillis, toMillis, includeLocal),
            dailyScores = behaviorLogDao.classDailyScoresInRange(classId, fromMillis, toMillis, includeLocal),
            positiveStudents = buildStudentScores(
                students = studentDao.observeAllActive().first().filter { it.classId == classId },
                scores = behaviorLogDao.classStudentScoresInRange(classId, fromMillis, toMillis, includeLocal),
                take = 50,
            ),
            negativeStudents = buildStudentScores(
                students = studentDao.observeAllActive().first().filter { it.classId == classId },
                scores = behaviorLogDao.classStudentNegativeScoresInRange(classId, fromMillis, toMillis, includeLocal),
                take = 50,
            ),
        )
    }

    private fun buildStudentScores(
        students: List<ru.golpom.atmosphere.data.local.entity.StudentEntity>,
        scores: List<ru.golpom.atmosphere.data.local.model.StudentSubjectScore>,
        take: Int,
    ): List<ru.golpom.atmosphere.ui.deputy.StudentWithScore> {
        val studentMap = students.associateBy { it.studentId }
        return scores
            .filter { it.studentId in studentMap }
            .map { s ->
                val stu = studentMap.getValue(s.studentId)
                ru.golpom.atmosphere.ui.deputy.StudentWithScore(
                    studentId = s.studentId,
                    firstName = stu.firstName,
                    lastName = stu.lastName,
                    classId = stu.classId,
                    score = s.totalScore,
                )
            }
            .take(take)
    }

    suspend fun getStudentSubjectHeatmap(classId: String, fromMillis: Long, toMillis: Long): List<ru.golpom.atmosphere.data.local.model.StudentSubjectCell> {
        val includeLocal = includeLocalDeputyData()
        return behaviorLogDao.studentSubjectScoresInRange(classId, fromMillis, toMillis, includeLocal)
    }

    suspend fun getStudentDailyScores(studentId: String, fromMillis: Long, toMillis: Long): List<DailyScore> =
        getStudentDailyScores(studentId, fromMillis, toMillis, forDeputyDetail = true)

    suspend fun getStudentDailyScores(
        studentId: String,
        fromMillis: Long,
        toMillis: Long,
        forDeputyDetail: Boolean,
    ): List<DailyScore> {
        val includeLocal = if (forDeputyDetail) true else false
        return behaviorLogDao.studentDailyScoresInRange(studentId, fromMillis, toMillis, includeLocal)
    }

    suspend fun getStudentById(studentId: String): StudentEntity? =
        studentDao.observeStudent(studentId).first()

    suspend fun getStudentTotalsInRange(
        studentId: String,
        fromMillis: Long,
        toMillis: Long,
    ): StudentTotalsInRange =
        getStudentTotalsInRange(studentId, fromMillis, toMillis, forDeputyDetail = true)

    suspend fun getStudentTotalsInRange(
        studentId: String,
        fromMillis: Long,
        toMillis: Long,
        forDeputyDetail: Boolean,
    ): StudentTotalsInRange {
        val includeLocal = if (forDeputyDetail) true else false
        return behaviorLogDao.studentTotalsInRange(studentId, fromMillis, toMillis, includeLocal)
            ?: StudentTotalsInRange(0, 0, 0, 0)
    }

    suspend fun getStudentSubjectBreakdown(
        studentId: String,
        fromMillis: Long,
        toMillis: Long,
    ): List<StudentSubjectBreakdown> {
        val includeLocal = includeLocalDeputyData()
        return behaviorLogDao.studentSubjectBreakdownInRange(studentId, fromMillis, toMillis, includeLocal)
    }

    suspend fun getStudentViolationTypes(
        studentId: String,
        fromMillis: Long,
        toMillis: Long,
    ): List<BehaviorTypeCount> {
        val includeLocal = includeLocalDeputyData()
        return behaviorLogDao.studentViolationTypesInRange(studentId, fromMillis, toMillis, includeLocal)
    }

    suspend fun getStudentMeritTypes(
        studentId: String,
        fromMillis: Long,
        toMillis: Long,
    ): List<BehaviorTypeCount> {
        val includeLocal = includeLocalDeputyData()
        return behaviorLogDao.studentMeritTypesInRange(studentId, fromMillis, toMillis, includeLocal)
    }

    suspend fun getStudentLogsInRange(
        studentId: String,
        fromMillis: Long,
        toMillis: Long,
    ): List<BehaviorLogEntity> =
        getStudentLogsInRange(studentId, fromMillis, toMillis, forDeputyDetail = true)

    suspend fun getStudentLogsInRange(
        studentId: String,
        fromMillis: Long,
        toMillis: Long,
        forDeputyDetail: Boolean,
    ): List<BehaviorLogEntity> {
        // Карточка ученика у завуча: все видимые отметки (импорт + свои), независимо от тумблера «Мои уроки».
        val includeLocal = if (forDeputyDetail) true else false
        return behaviorLogDao.studentLogsInRangeVisible(studentId, fromMillis, toMillis, includeLocal)
            .map { it.toBehaviorLogEntity() }
    }

    suspend fun getClassLogsInRange(
        classId: String,
        fromMillis: Long,
        toMillis: Long,
    ): List<BehaviorLogEntity> {
        val includeLocal = includeLocalDeputyData()
        return behaviorLogDao.classLogsInRangeVisible(classId, fromMillis, toMillis, includeLocal)
            .map { it.toBehaviorLogEntity() }
    }

    suspend fun getActiveStudentsInClass(classId: String): List<StudentEntity> =
        studentDao.observeAllActive().first().filter { it.classId == classId }

    suspend fun searchStudents(query: String): List<StudentEntity> =
        if (query.isBlank()) emptyList() else studentDao.searchActiveStudents(query)

    private suspend fun nextGeneratedStudentId(
        seqByClass: MutableMap<String, Int>,
        classId: String,
    ): String {
        val prev = seqByClass[classId] ?: studentDao.countByClass(classId).toInt()
        val next = prev + 1
        seqByClass[classId] = next
        return "${Year.now().value}-$classId-$next"
    }
}
