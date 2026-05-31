/**
 * Состояние домашнего экрана учителя.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.teacher

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.golpom.atmosphere.data.csv.StudentListCsvParseOutcome
import ru.golpom.atmosphere.data.csv.StudentListCsvParser
import ru.golpom.atmosphere.data.local.entity.ScheduleEntryEntity
import ru.golpom.atmosphere.data.preferences.UserPreferencesRepository
import ru.golpom.atmosphere.data.repository.CatalogRepository
import ru.golpom.atmosphere.domain.AppRole
import ru.golpom.atmosphere.ui.navigation.NavDestinations
import java.nio.charset.StandardCharsets

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalogRepository: CatalogRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val classes = catalogRepository.observeTeacherClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val teacherLastName = userPreferencesRepository.teacherLastName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Учитель")

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val studentCount = catalogRepository.observeTeacherStudentCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val totalScore = catalogRepository.observeTeacherTotalScore()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val allMeetings = catalogRepository.observeMeetings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val upcomingMeetingCount = allMeetings
        .map { list -> list.count { it.dateTimeMillis > System.currentTimeMillis() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val nextMeeting = catalogRepository.observeNextMeeting(System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _navigateTo = MutableStateFlow<String?>(null)
    val navigateTo: StateFlow<String?> = _navigateTo.asStateFlow()

    private val allSchedule = catalogRepository.observeAllSchedule()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val nextLesson = allSchedule
        .map { entries -> findNextLesson(entries) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun consumeUserMessage() {
        _userMessage.value = null
    }

    fun saveTeacherLastName(raw: String) {
        viewModelScope.launch {
            userPreferencesRepository.setTeacherLastName(raw.trim())
        }
    }

    fun importStudentsFromUri(uri: Uri) {
        viewModelScope.launch {
            val text = readUriText(uri) ?: run {
                _userMessage.value = "Не удалось прочитать файл со списком учеников."
                return@launch
            }
            when (val outcome = StudentListCsvParser.parse(text)) {
                is StudentListCsvParseOutcome.Failure ->
                    _userMessage.value = outcome.message

                is StudentListCsvParseOutcome.Success -> {
                    val summary = catalogRepository.mergeStudentsFromParsedRows(
                        outcome.rows,
                        outcome.warnings.size,
                    )
                    _userMessage.value = buildString {
                        append("Добавлено учеников: ${summary.inserted}.")
                        if (summary.skippedExisting > 0) {
                            append(" Уже были в списке: ${summary.skippedExisting}.")
                        }
                        if (summary.parseWarnings > 0) {
                            append(" Строк с замечаниями: ${summary.parseWarnings}.")
                        }
                    }
                }
            }
        }
    }

    fun importClassConfigFromUri(uri: Uri) {
        viewModelScope.launch {
            val text = readUriText(uri) ?: run {
                _userMessage.value = "Не удалось прочитать файл настроек класса."
                return@launch
            }
            runCatching {
                val summary = catalogRepository.mergeStudentsFromClassConfigJson(text)
                _userMessage.value = buildString {
                    append("В список класса добавлено: ${summary.inserted}.")
                    if (summary.skippedExisting > 0) {
                        append(" Уже были: ${summary.skippedExisting}.")
                    }
                }
            }.onFailure { e ->
                _userMessage.value = "Ошибка при чтении файла: ${e.message ?: "неизвестная ошибка"}"
            }
        }
    }

    fun toggleRole() {
        viewModelScope.launch {
            val current = userPreferencesRepository.appRole.first()
            val next = when (current) {
                AppRole.TEACHER -> AppRole.DEPUTY
                AppRole.DEPUTY -> AppRole.TEACHER
                AppRole.NOT_SET -> AppRole.TEACHER
            }
            userPreferencesRepository.setRole(next)
            _navigateTo.value = when (next) {
                AppRole.TEACHER -> NavDestinations.TEACHER_HOME
                AppRole.DEPUTY -> NavDestinations.DEPUTY_HOME
                AppRole.NOT_SET -> NavDestinations.TEACHER_HOME
            }
        }
    }

    fun onNavigated() {
        _navigateTo.value = null
    }

    suspend fun prepareClassConfigExport(): Pair<String, ByteArray> {
        val name = "Class_Config_${LocalDate.now()}.json"
        return catalogRepository.buildClassConfigExport(name)
    }

    private suspend fun readUriText(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                if (bytes.size > MAX_BYTES) return@use null
                String(bytes, StandardCharsets.UTF_8)
            }
        }.getOrNull()
    }

    private fun findNextLesson(entries: List<ScheduleEntryEntity>): ScheduleEntryEntity? {
        if (entries.isEmpty()) return null
        val now = java.time.ZonedDateTime.now()
        val currentMinutes = now.toLocalTime().toSecondOfDay() / 60
        val todayIso = now.dayOfWeek.value // 1=Пн .. 7=Вс; в расписании только 1..6

        if (todayIso != 7) {
            entries
                .filter { it.dayOfWeek == todayIso && it.startTimeMinutes > currentMinutes }
                .minByOrNull { it.startTimeMinutes }
                ?.let { return it }
        }

        for (daysAhead in 1..7) {
            val futureDay = now.plusDays(daysAhead.toLong()).dayOfWeek.value
            if (futureDay == 7) continue
            entries
                .filter { it.dayOfWeek == futureDay }
                .minByOrNull { it.startTimeMinutes }
                ?.let { return it }
        }

        return entries.minWithOrNull(compareBy({ it.dayOfWeek }, { it.startTimeMinutes }))
    }

    companion object {
        private const val MAX_BYTES = 4 * 1024 * 1024
    }
}
