/**
 * Состояние экрана класса учителя.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.golpom.atmosphere.data.export.TeacherBehaviorExporter
import ru.golpom.atmosphere.data.preferences.UserPreferencesRepository
import ru.golpom.atmosphere.data.local.entity.StudentEntity
import ru.golpom.atmosphere.data.local.model.StudentSubjectScore
import ru.golpom.atmosphere.data.repository.CatalogRepository
import ru.golpom.atmosphere.domain.export.ExportPeriodSelection
import ru.golpom.atmosphere.domain.export.TeacherExportOptions
import ru.golpom.atmosphere.domain.export.TeacherExportRequest
import ru.golpom.atmosphere.domain.export.TeacherExportScope
import ru.golpom.atmosphere.ui.export.ExportPayload
import ru.golpom.atmosphere.ui.export.ExportSecurityDefaults

@HiltViewModel
class ClassDetailViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val teacherBehaviorExporter: TeacherBehaviorExporter,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _classId = MutableStateFlow("")
    val classId: StateFlow<String> = _classId.asStateFlow()

    val students = _classId
        .flatMapLatest { catalogRepository.observeTeacherActiveByClass(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subjects = _classId
        .flatMapLatest { catalogRepository.observeSubjectsByClass(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedSubject = MutableStateFlow<String?>(null)
    val selectedSubject: StateFlow<String?> = _selectedSubject.asStateFlow()

    private val _scores = MutableStateFlow<Map<String, Int>>(emptyMap())
    val scores: StateFlow<Map<String, Int>> = _scores.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun setClassId(id: String) {
        _classId.value = id
    }

    fun selectSubject(subject: String?) {
        _selectedSubject.value = subject
        loadScores()
    }

    private fun loadScores() {
        val cid = _classId.value
        if (cid.isBlank()) return
        viewModelScope.launch {
            _loading.value = true
            val result = if (_selectedSubject.value != null) {
                catalogRepository.getScoresByClassAndSubject(cid, _selectedSubject.value!!)
                    .associate { it.studentId to it.totalScore }
            } else {
                catalogRepository.getTotalScoresForClass(cid)
                    .associate { it.studentId to it.totalScore }
            }
            _scores.value = result
            _loading.value = false
        }
    }

    fun getScore(studentId: String): Int = _scores.value[studentId] ?: 0

    suspend fun exportSecurityDefaults(): ExportSecurityDefaults = ExportSecurityDefaults(
        neutralFileName = userPreferencesRepository.exportNeutralFileName.first(),
    )

    fun rememberExportSecurityChoices(options: TeacherExportOptions) {
        viewModelScope.launch {
            userPreferencesRepository.setExportNeutralFileName(options.neutralFileName)
        }
    }

    suspend fun exportClass(period: ExportPeriodSelection, options: TeacherExportOptions): ExportPayload =
        withContext(Dispatchers.IO) {
            teacherBehaviorExporter.export(
                TeacherExportRequest(
                    scope = TeacherExportScope.Class(_classId.value),
                    period = period,
                    options = options,
                ),
            )
        }
}
