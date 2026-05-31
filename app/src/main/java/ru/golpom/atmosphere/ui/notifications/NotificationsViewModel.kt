/**
 * Состояние уведомлений и быстрых отметок.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.golpom.atmosphere.data.local.entity.NotificationEntity
import ru.golpom.atmosphere.data.notifications.SystemNotificationHelper
import ru.golpom.atmosphere.data.preferences.UserPreferencesRepository
import ru.golpom.atmosphere.data.repository.CatalogRepository

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val systemNotificationHelper: SystemNotificationHelper,
) : ViewModel() {

    val notifications: StateFlow<List<NotificationEntity>> =
        catalogRepository.observeNotifications()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> =
        catalogRepository.observeUnreadNotificationCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _showGradePrompt = MutableStateFlow<GradePromptState?>(null)
    val showGradePrompt: StateFlow<GradePromptState?> = _showGradePrompt.asStateFlow()

    private val _notificationCreated = MutableStateFlow(false)
    val notificationCreated: StateFlow<Boolean> = _notificationCreated.asStateFlow()

    init {
        viewModelScope.launch {
            catalogRepository.cleanOldNotifications()
            autoCheckMeetingReminders()
            autoCheckLessonCompletions()
        }
    }

    fun dismiss(id: Long) {
        viewModelScope.launch { catalogRepository.dismissNotification(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { catalogRepository.deleteNotification(id) }
    }

    fun markRead(id: Long) {
        viewModelScope.launch { catalogRepository.markNotificationRead(id) }
    }

    fun checkNow() {
        viewModelScope.launch {
            autoCheckMeetingReminders()
            autoCheckLessonCompletions()
        }
    }

    fun dismissGradePrompt() {
        _showGradePrompt.value = null
    }

    fun applyBulkGrade(studentIds: List<String>, classId: String, subjectKey: String, behaviorType: String, scoreImpact: Int) {
        viewModelScope.launch {
            for (sid in studentIds) {
                catalogRepository.logBehavior(sid, classId, subjectKey, behaviorType, scoreImpact)
            }
            _showGradePrompt.value = null
        }
    }

    fun showGradePromptForLesson(classId: String, subjectKey: String) {
        viewModelScope.launch {
            val students = catalogRepository.observeTeacherActiveByClass(classId).first()
            val rows = catalogRepository.getScoresByClassAndSubject(classId, subjectKey)
            if (students.isNotEmpty() && rows.all { it.totalScore == 0 }) {
                _showGradePrompt.value = GradePromptState(
                    classId = classId,
                    subjectKey = subjectKey,
                    studentIds = students.map { it.studentId },
                )
            }
        }
    }

    private suspend fun autoCheckMeetingReminders() {
        val enabled = userPreferencesRepository.meetingRemindersEnabled.first()
        if (!enabled) return

        val meetings = catalogRepository.observeMeetings().first()
        val now = System.currentTimeMillis()
        val existing = catalogRepository.observeNotifications().first()

        for (m in meetings) {
            val dayBefore = m.dateTimeMillis - 24L * 60 * 60 * 1000
            val threeHoursBefore = m.dateTimeMillis - 3L * 60 * 60 * 1000
            val meetingRef = "meeting_${m.id}"

            if (now in dayBefore..m.dateTimeMillis) {
                val key = "reminder_1d_$meetingRef"
                if (existing.none { it.relatedId == key }) {
                    createNotification(
                        NotificationEntity(
                            type = "meeting_reminder",
                            title = "Родительское собрание завтра",
                            body = "Завтра собрание для ${m.classId}. Тема: «${m.topic}».",
                            timestampMillis = now,
                            relatedId = key,
                        ),
                    )
                }
            }

            if (now in threeHoursBefore..m.dateTimeMillis) {
                val key = "reminder_3h_$meetingRef"
                if (existing.none { it.relatedId == key }) {
                    val userName = userPreferencesRepository.userName.first()
                    val greeting = if (userName.isNotBlank()) "$userName, напоминаем" else "Напоминаем"
                    createNotification(
                        NotificationEntity(
                            type = "meeting_reminder",
                            title = "Собрание через 3 часа",
                            body = "$greeting: сегодня собрание для ${m.classId} в ${formatTime(m.dateTimeMillis)}.",
                            timestampMillis = now,
                            relatedId = key,
                        ),
                    )
                }
            }
        }
    }

    private suspend fun autoCheckLessonCompletions() {
        val enabled = userPreferencesRepository.lessonRemindersEnabled.first()
        if (!enabled) return

        val now = ZonedDateTime.now()
        val currentDay = (now.dayOfWeek.value + 6) % 7 + 1
        val currentMinutes = now.toLocalTime().toSecondOfDay() / 60

        val allSchedule = catalogRepository.observeAllSchedule().first()
        val recentlyEnded = allSchedule.filter { entry ->
            entry.dayOfWeek == currentDay &&
                entry.endTimeMinutes <= currentMinutes &&
                entry.endTimeMinutes > currentMinutes - 120
        }

        val existing = catalogRepository.observeNotifications().first()

        for (entry in recentlyEnded) {
            val logs = catalogRepository.getScoresByClassAndSubject(entry.classId, entry.subjectKey)
            val hasGrades = logs.any { it.totalScore != 0 }
            if (!hasGrades) {
                val key = "lesson_nogrades_${entry.id}"
                if (existing.none { it.relatedId == key }) {
                    val userName = userPreferencesRepository.userName.first()
                    val greeting = if (userName.isNotBlank()) "$userName, " else ""
                    createNotification(
                        NotificationEntity(
                            type = "lesson_grade_prompt",
                            title = "Урок без отметок",
                            body = "${greeting}урок «${entry.subjectKey}» (${entry.classId}) завершён. Поставьте баллы ученикам.",
                            timestampMillis = now.toInstant().toEpochMilli(),
                            relatedId = key,
                            metadataJson = """{"classId":"${entry.classId}","subjectKey":"${entry.subjectKey}"}""",
                        ),
                    )
                }
            }
        }
    }

    private suspend fun createNotification(entity: NotificationEntity) {
        if (isInQuietHours()) return
        catalogRepository.insertNotification(entity)
        if (userPreferencesRepository.systemNotificationsEnabled.first()) {
            systemNotificationHelper.postNotification(entity.title, entity.body)
        }
    }

    private fun isInQuietHours(): Boolean {
        val now = ZonedDateTime.now()
        val hour = now.hour
        return when {
            now.dayOfWeek == DayOfWeek.SUNDAY && hour < 10 -> true
            hour >= 21 -> true
            hour < 8 -> true
            else -> false
        }
    }

    companion object {
        private fun formatTime(millis: Long): String {
            val zdt = java.time.Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
            return zdt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        }
    }
}

data class GradePromptState(
    val classId: String,
    val subjectKey: String,
    val studentIds: List<String>,
)
