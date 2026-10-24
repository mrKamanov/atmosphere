/**
 * Состояние списка классов.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.golpom.atmosphere.data.repository.CatalogRepository
import ru.golpom.atmosphere.data.repository.SchoolYearRepository
import ru.golpom.atmosphere.domain.schoolyear.GraduateAction
import ru.golpom.atmosphere.domain.schoolyear.SchoolYearRollover
import ru.golpom.atmosphere.domain.schoolyear.SchoolYearRolloverPlan
import ru.golpom.atmosphere.domain.student.StudentIdentity

/** Состояние диалога «Новый учебный год». null — диалог закрыт. */
data class SchoolYearDialogState(
    val plan: SchoolYearRolloverPlan,
    val graduateAction: GraduateAction = GraduateAction.ARCHIVE,
    val shiftSchedule: Boolean = true,
    /** Дата прошлого перевода, если он был меньше полугода назад: предупреждаем о повторе. */
    val recentRolloverDate: LocalDate? = null,
    val applying: Boolean = false,
)

@HiltViewModel
class ClassesViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val schoolYearRepository: SchoolYearRepository,
) : ViewModel() {

    val classes = catalogRepository.observeTeacherClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Подсказка над списком классов: с июня по сентябрь, если в этом сезоне
     * классы ещё не переводили и есть что переводить.
     */
    val showSchoolYearHint: StateFlow<Boolean> = combine(
        classes,
        schoolYearRepository.lastRolloverMillis,
    ) { list, last ->
        val today = LocalDate.now()
        val inSeason = today.monthValue in 6..9
        val notDoneThisSeason = last == null || daysSince(last) > RECENT_ROLLOVER_DAYS
        inSeason && notDoneThisSeason && list.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _schoolYearDialog = MutableStateFlow<SchoolYearDialogState?>(null)
    val schoolYearDialog: StateFlow<SchoolYearDialogState?> = _schoolYearDialog.asStateFlow()

    fun createClass(raw: String) {
        val id = StudentIdentity.normalizeClassId(raw)
        if (id.isBlank()) {
            _userMessage.value = "Введите код класса"
            return
        }
        viewModelScope.launch {
            catalogRepository.createClass(id)
            _userMessage.value = "Класс $id добавлен"
        }
    }

    private val _pendingClassDeletion = MutableStateFlow<CatalogRepository.ClassDeletionImpact?>(null)

    /** Класс, который учитель собирается удалить, и что пропадёт вместе с ним. */
    val pendingClassDeletion: StateFlow<CatalogRepository.ClassDeletionImpact?> =
        _pendingClassDeletion.asStateFlow()

    fun requestDeleteClass(classId: String) {
        viewModelScope.launch {
            _pendingClassDeletion.value = catalogRepository.getClassDeletionImpact(classId)
        }
    }

    fun dismissDeleteClass() {
        _pendingClassDeletion.value = null
    }

    fun confirmDeleteClass() {
        val impact = _pendingClassDeletion.value ?: return
        _pendingClassDeletion.value = null
        viewModelScope.launch {
            catalogRepository.deleteClass(impact.classId)
            _userMessage.value = "Класс ${impact.classId} удалён"
        }
    }

    fun consumeMessage() {
        _userMessage.value = null
    }

    // region Новый учебный год

    fun openSchoolYearDialog() {
        viewModelScope.launch {
            val plan = schoolYearRepository.buildPlan(SchoolYearRollover.DEFAULT_GRADUATION_GRADE)
            val last = schoolYearRepository.lastRolloverMillis.first()
            val recent = last
                ?.takeIf { daysSince(it) <= RECENT_ROLLOVER_DAYS }
                ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            _schoolYearDialog.value = SchoolYearDialogState(plan = plan, recentRolloverDate = recent)
        }
    }

    fun setGraduationGrade(grade: Int) {
        val current = _schoolYearDialog.value ?: return
        if (current.plan.graduationGrade == grade) return
        viewModelScope.launch {
            val plan = schoolYearRepository.buildPlan(grade)
            _schoolYearDialog.value = _schoolYearDialog.value?.copy(plan = plan)
        }
    }

    fun setGraduateAction(action: GraduateAction) {
        _schoolYearDialog.value = _schoolYearDialog.value?.copy(graduateAction = action)
    }

    fun setShiftSchedule(enabled: Boolean) {
        _schoolYearDialog.value = _schoolYearDialog.value?.copy(shiftSchedule = enabled)
    }

    fun dismissSchoolYearDialog() {
        if (_schoolYearDialog.value?.applying == true) return
        _schoolYearDialog.value = null
    }

    fun confirmSchoolYearRollover() {
        val state = _schoolYearDialog.value ?: return
        if (state.applying || state.plan.isEmpty) return
        _schoolYearDialog.value = state.copy(applying = true)
        viewModelScope.launch {
            val message = try {
                val result = schoolYearRepository.apply(
                    plan = state.plan,
                    graduateAction = state.graduateAction,
                    shiftSchedule = state.shiftSchedule,
                )
                buildString {
                    append("Готово: переведено учеников — ${result.promotedStudents}")
                    if (result.graduatedStudents > 0) {
                        val verb = when (state.graduateAction) {
                            GraduateAction.ARCHIVE -> "в архиве"
                            GraduateAction.DELETE -> "удалено"
                        }
                        append(", выпускников $verb — ${result.graduatedStudents}")
                    }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Exception) {
                "Не удалось перевести классы: ${t.message ?: t::class.java.simpleName}"
            }
            _schoolYearDialog.value = null
            _userMessage.value = message
        }
    }

    // endregion

    private fun daysSince(millis: Long): Long =
        ChronoUnit.DAYS.between(Instant.ofEpochMilli(millis), Instant.now())

    private companion object {
        /** Меньше ~полугода с прошлого перевода — считаем, что этот год уже переведён. */
        const val RECENT_ROLLOVER_DAYS = 180L
    }
}
