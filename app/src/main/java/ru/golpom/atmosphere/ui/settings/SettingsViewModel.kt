/**
 * Состояние экрана настроек.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.golpom.atmosphere.data.export.TeacherBehaviorExporter
import ru.golpom.atmosphere.data.preferences.UserPreferencesRepository
import ru.golpom.atmosphere.data.repository.CatalogRepository
import ru.golpom.atmosphere.domain.export.ExportPeriodSelection
import ru.golpom.atmosphere.domain.export.TeacherExportOptions
import ru.golpom.atmosphere.domain.export.TeacherExportRequest
import ru.golpom.atmosphere.domain.export.TeacherExportScope
import ru.golpom.atmosphere.ui.export.ExportPayload
import ru.golpom.atmosphere.ui.export.ExportSecurityDefaults

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val catalogRepository: CatalogRepository,
    private val teacherBehaviorExporter: TeacherBehaviorExporter,
) : ViewModel() {

    private val _clearResult = MutableStateFlow<String?>(null)
    val clearResult: kotlinx.coroutines.flow.StateFlow<String?> = _clearResult

    private val _teacherSubjects = MutableStateFlow<List<String>>(emptyList())
    val teacherSubjects: kotlinx.coroutines.flow.StateFlow<List<String>> = _teacherSubjects

    init {
        refreshTeacherSubjects()
    }

    val userName = preferencesRepository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val teacherLastName = preferencesRepository.teacherLastName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Учитель")

    val meetingRemindersEnabled = preferencesRepository.meetingRemindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val lessonRemindersEnabled = preferencesRepository.lessonRemindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val systemNotificationsEnabled = preferencesRepository.systemNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val exportNeutralFileName = preferencesRepository.exportNeutralFileName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    suspend fun loadUserName(): String = preferencesRepository.userName.first()

    suspend fun loadTeacherLastName(): String = preferencesRepository.teacherLastName.first()

    suspend fun exportSecurityDefaults(): ExportSecurityDefaults = ExportSecurityDefaults(
        neutralFileName = preferencesRepository.exportNeutralFileName.first(),
    )

    fun setTeacherLastName(value: String) {
        viewModelScope.launch { preferencesRepository.setTeacherLastName(value) }
    }

    fun refreshTeacherSubjects() {
        viewModelScope.launch {
            _teacherSubjects.value = catalogRepository.listTeacherSubjects()
        }
    }

    suspend fun exportAllClasses(period: ExportPeriodSelection, options: TeacherExportOptions): ExportPayload =
        export(TeacherExportScope.AllMyClasses, period, options)

    suspend fun exportAllData(period: ExportPeriodSelection, options: TeacherExportOptions): ExportPayload =
        export(TeacherExportScope.AllData, period, options)

    suspend fun exportSubject(
        subjectKey: String,
        period: ExportPeriodSelection,
        options: TeacherExportOptions,
    ): ExportPayload = export(TeacherExportScope.Subject(subjectKey), period, options)

    private suspend fun export(
        scope: TeacherExportScope,
        period: ExportPeriodSelection,
        options: TeacherExportOptions,
    ): ExportPayload = withContext(Dispatchers.IO) {
        teacherBehaviorExporter.export(TeacherExportRequest(scope, period, options))
    }

    fun setExportNeutralFileName(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setExportNeutralFileName(enabled) }
    }

    fun rememberExportSecurityChoices(options: TeacherExportOptions) {
        setExportNeutralFileName(options.neutralFileName)
    }

    fun setUserName(name: String) {
        viewModelScope.launch { preferencesRepository.setUserName(name) }
    }

    fun setMeetingReminders(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setMeetingRemindersEnabled(enabled) }
    }

    fun setLessonReminders(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setLessonRemindersEnabled(enabled) }
    }

    fun setSystemNotifications(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSystemNotificationsEnabled(enabled) }
    }

    fun clearAllScores() {
        viewModelScope.launch {
            catalogRepository.clearAllScores()
            _clearResult.value = "Все отметки удалены"
        }
    }

    fun clearArchive() {
        viewModelScope.launch {
            catalogRepository.clearArchive()
            _clearResult.value = "Архив очищен"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            catalogRepository.clearAllData()
            _clearResult.value = "Классы, ученики и отметки удалены"
        }
    }

    fun dismissClearResult() {
        _clearResult.value = null
    }
}
