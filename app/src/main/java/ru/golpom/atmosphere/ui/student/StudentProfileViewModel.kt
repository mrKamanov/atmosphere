/**
 * Профиль ученика: карточка из каталога и лента событий поведения (все предметы).
 * UI-слой (ViewModel); аргумент навигации — `studentId`.
 */
package ru.golpom.atmosphere.ui.student

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import ru.golpom.atmosphere.domain.AppRole
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StudentProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val teacherBehaviorExporter: TeacherBehaviorExporter,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val studentId: String =
        URLDecoder.decode(
            checkNotNull(savedStateHandle.get<String>("studentId")),
            StandardCharsets.UTF_8.name(),
        )

    val student = catalogRepository.observeStudent(studentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val behaviorLogs = userPreferencesRepository.appRole
        .flatMapLatest { role ->
            when (role) {
                AppRole.DEPUTY -> catalogRepository.observeDeputyBehaviorLogsForStudent(studentId)
                else -> catalogRepository.observeBehaviorLogsForStudent(studentId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteLog(logId: Long) {
        viewModelScope.launch {
            catalogRepository.deleteBehaviorLog(logId)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            catalogRepository.clearBehaviorLogsForStudent(studentId)
        }
    }

    suspend fun exportSecurityDefaults(): ExportSecurityDefaults = ExportSecurityDefaults(
        neutralFileName = userPreferencesRepository.exportNeutralFileName.first(),
    )

    fun rememberExportSecurityChoices(options: TeacherExportOptions) {
        viewModelScope.launch {
            userPreferencesRepository.setExportNeutralFileName(options.neutralFileName)
        }
    }

    suspend fun exportStudent(period: ExportPeriodSelection, options: TeacherExportOptions): ExportPayload =
        withContext(Dispatchers.IO) {
            teacherBehaviorExporter.export(
                TeacherExportRequest(
                    scope = TeacherExportScope.Student(studentId),
                    period = period,
                    options = options,
                ),
            )
        }
}
