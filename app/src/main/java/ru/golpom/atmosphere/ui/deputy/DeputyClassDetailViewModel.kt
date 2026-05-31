/**
 * Детализация класса для завуча.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.deputy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.golpom.atmosphere.data.local.model.StudentSubjectCell
import ru.golpom.atmosphere.data.repository.CatalogRepository

data class ClassDetailUiState(
    val classId: String = "",
    val analytics: ClassAnalytics? = null,
    val heatmapData: List<StudentSubjectCell> = emptyList(),
    val availableSubjects: List<String> = emptyList(),
    val periodConfig: PeriodConfig = PeriodConfig(),
    val loading: Boolean = true,
)

@HiltViewModel
class DeputyClassDetailViewModel @Inject constructor(
    private val repository: CatalogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ClassDetailUiState())
    val state: StateFlow<ClassDetailUiState> = _state.asStateFlow()

    private var loadedClassId: String? = null

    fun loadClass(classId: String) {
        if (loadedClassId == classId && _state.value.analytics != null) return
        loadedClassId = classId
        _state.value = _state.value.copy(classId = classId, loading = true)
        reload()
    }

    fun setPeriodType(type: PeriodType) {
        _state.value = _state.value.copy(
            periodConfig = _state.value.periodConfig.copy(type = type, fromDate = null, toDate = null),
        )
        reload()
    }

    fun setCustomRange(fromMillis: Long, toMillis: Long) {
        _state.value = _state.value.copy(
            periodConfig = _state.value.periodConfig.copy(
                type = PeriodType.CUSTOM,
                fromDate = fromMillis,
                toDate = toMillis,
            ),
        )
        reload()
    }

    private fun reload() {
        val classId = loadedClassId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val cfg = _state.value.periodConfig
            val (from, to) = DeputyPeriodRange.calc(cfg)
            val students = repository.getActiveStudentsInClass(classId)
            val totals = repository.getClassDetailStats(classId, from, to)
            val logs = repository.getClassLogsInRange(classId, from, to)
            val daily = totals.dailyScores
            val heatmap = repository.getStudentSubjectHeatmap(classId, from, to)
            val subjects = heatmap.map { it.subjectKey }.distinct().sorted()
            val insight = ClassInsightBuilder.build(logs, daily, students)
            val praiseCount = logs.count { it.scoreImpact > 0 }
            val violationCount = logs.count { it.scoreImpact < 0 }

            val praiseRows = totals.positiveStudents.mapIndexed { i, s ->
                val total = totals.positiveStudents.sumOf { abs(it.score) }.coerceAtLeast(1)
                StudentRankingRow(
                    s.studentId,
                    "${s.lastName} ${s.firstName}",
                    s.classId,
                    s.score,
                    abs(s.score) * 100 / total,
                    i + 1,
                )
            }
            val watchRows = totals.negativeStudents.mapIndexed { i, s ->
                val total = totals.negativeStudents.sumOf { abs(it.score) }.coerceAtLeast(1)
                StudentRankingRow(
                    s.studentId,
                    "${s.lastName} ${s.firstName}",
                    s.classId,
                    s.score,
                    abs(s.score) * 100 / total,
                    i + 1,
                )
            }

            _state.value = ClassDetailUiState(
                classId = classId,
                analytics = ClassAnalytics(
                    classId = classId,
                    periodLabel = DeputyPeriodRange.label(cfg),
                    totalScore = totals.totalScore,
                    eventCount = logs.size,
                    praiseCount = praiseCount,
                    violationCount = violationCount,
                    activeStudentCount = students.size,
                    dailyScores = daily,
                    insight = insight,
                    praiseStudents = praiseRows,
                    watchStudents = watchRows,
                ),
                heatmapData = heatmap,
                availableSubjects = subjects,
                periodConfig = cfg,
                loading = false,
            )
        }
    }
}
