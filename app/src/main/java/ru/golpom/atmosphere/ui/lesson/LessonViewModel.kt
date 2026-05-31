/**
 * Состояние и действия экрана урока: сетка учеников, запись событий, подготовка CSV для экспорта.
 * UI-слой (ViewModel); аргументы навигации — `classId`, `subjectKey`.
 */
package ru.golpom.atmosphere.ui.lesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.golpom.atmosphere.data.csv.BehaviorLogCsvSerializer
import ru.golpom.atmosphere.data.csv.TeacherBehaviorExportFileName
import ru.golpom.atmosphere.data.preferences.UserPreferencesRepository
import ru.golpom.atmosphere.data.repository.CatalogRepository
import ru.golpom.atmosphere.domain.BehaviorPreset

@HiltViewModel
class LessonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val classId: String =
        URLDecoder.decode(
            checkNotNull(savedStateHandle.get<String>("classId")),
            StandardCharsets.UTF_8.name(),
        )
    val subjectKey: String = checkNotNull(savedStateHandle.get<String>("subjectKey"))

    val rows = catalogRepository.observeLessonRows(classId, subjectKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun logBehavior(studentId: String, preset: BehaviorPreset) {
        viewModelScope.launch {
            catalogRepository.logBehavior(
                studentId = studentId,
                classId = classId,
                subjectKey = subjectKey,
                behaviorType = preset.behaviorType,
                scoreImpact = preset.scoreImpact,
            )
        }
    }

    suspend fun buildExportPayload(): LessonExportPayload = withContext(Dispatchers.IO) {
        val logs = catalogRepository.getBehaviorLogsForClassAndSubject(classId, subjectKey)
        val teacherLastName = userPreferencesRepository.teacherLastName.first()
        val teacherProfileShortId = userPreferencesRepository.getOrCreateTeacherProfileShortId()
        val fileName = TeacherBehaviorExportFileName.build(
            classId = classId,
            subjectKey = subjectKey,
            teacherLastName = teacherLastName,
            teacherProfileShortId = teacherProfileShortId,
        )
        val exportRows = catalogRepository.buildBehaviorLogExportRows(logs)
        val csv = BehaviorLogCsvSerializer.serialize(exportRows)
        LessonExportPayload(
            fileName = fileName,
            utf8Bytes = csv.toByteArray(StandardCharsets.UTF_8),
        )
    }
}
