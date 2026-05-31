/**
 * Состояние dashboard завуча и загрузка аналитики.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.deputy

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.golpom.atmosphere.data.csv.BehaviorLogImportStats
import ru.golpom.atmosphere.data.export.DeputyReportImporter
import ru.golpom.atmosphere.data.local.entity.ImportBatchEntity
import ru.golpom.atmosphere.data.preferences.UserPreferencesRepository
import ru.golpom.atmosphere.data.repository.CatalogRepository
import ru.golpom.atmosphere.domain.AppRole
import ru.golpom.atmosphere.ui.navigation.NavDestinations

@HiltViewModel
class DeputyHomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalogRepository: CatalogRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val deputyReportImporter: DeputyReportImporter,
) : ViewModel() {

    val importBatches = catalogRepository.observeImportBatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val localDataEnabled = userPreferencesRepository.deputyLocalDataEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _report = MutableStateFlow<String?>(null)
    val report: StateFlow<String?> = _report.asStateFlow()

    private val _navigateTo = MutableStateFlow<String?>(null)
    val navigateTo: StateFlow<String?> = _navigateTo.asStateFlow()

    private val _periodConfig = MutableStateFlow(PeriodConfig())
    val periodConfig: StateFlow<PeriodConfig> = _periodConfig.asStateFlow()

    private val _stats = MutableStateFlow(DeputyStats())
    val stats: StateFlow<DeputyStats> = _stats.asStateFlow()

    private val _previousStats = MutableStateFlow<DeputyStats?>(null)
    val previousStats: StateFlow<DeputyStats?> = _previousStats.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _importStatusMessage = MutableStateFlow("")
    val importStatusMessage: StateFlow<String> = _importStatusMessage.asStateFlow()

    private var hasLoadedDashboard = false

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ru.golpom.atmosphere.data.local.entity.StudentEntity>>(emptyList())
    val searchResults: StateFlow<List<ru.golpom.atmosphere.data.local.entity.StudentEntity>> = _searchResults.asStateFlow()

    private val _selectedStudentId = MutableStateFlow<String?>(null)
    val selectedStudentId: StateFlow<String?> = _selectedStudentId.asStateFlow()

    private val _studentAnalytics = MutableStateFlow<StudentAnalytics?>(null)
    val studentAnalytics: StateFlow<StudentAnalytics?> = _studentAnalytics.asStateFlow()

    init {
        refresh()
    }

    fun setPeriodType(type: PeriodType) {
        _periodConfig.value = _periodConfig.value.copy(type = type, fromDate = null, toDate = null)
        refresh()
        reloadSelectedStudent()
    }

    fun setCustomRange(fromMillis: Long, toMillis: Long) {
        _periodConfig.value = _periodConfig.value.copy(type = PeriodType.CUSTOM, fromDate = fromMillis, toDate = toMillis)
        refresh()
        reloadSelectedStudent()
    }

    fun refresh() {
        viewModelScope.launch {
            val showBlockingLoader = !hasLoadedDashboard
            if (showBlockingLoader) {
                _loading.value = true
            }
            try {
                loadDashboardStats()
            } finally {
                hasLoadedDashboard = true
                _loading.value = false
            }
        }
    }

    private suspend fun loadDashboardStats() {
        val cfg = _periodConfig.value
        val (from, to) = DeputyPeriodRange.calc(cfg)
        _stats.value = catalogRepository.getDeputyStats(from, to)
        val duration = to - from
        if (duration > 0) {
            val prevTo = from - 1
            val prevFrom = prevTo - duration
            _previousStats.value = catalogRepository.getDeputyStats(prevFrom, prevTo)
        } else {
            _previousStats.value = null
        }
        reloadSelectedStudent()
    }

    fun searchStudents(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            _searchResults.value = catalogRepository.searchStudents(query)
        }
    }

    fun selectStudent(studentId: String) {
        _selectedStudentId.value = studentId
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        reloadSelectedStudent()
    }

    fun clearSelectedStudent() {
        _selectedStudentId.value = null
        _studentAnalytics.value = null
    }

    fun reloadSelectedStudent() {
        val studentId = _selectedStudentId.value ?: return
        viewModelScope.launch {
            val cfg = _periodConfig.value
            val (from, to) = DeputyPeriodRange.calc(cfg)
            val student = catalogRepository.getStudentById(studentId) ?: run {
                _studentAnalytics.value = null
                return@launch
            }
            val totals = catalogRepository.getStudentTotalsInRange(studentId, from, to)
            val daily = catalogRepository.getStudentDailyScores(studentId, from, to)
            val logs = catalogRepository.getStudentLogsInRange(studentId, from, to)
            val praiseCount = logs.count { it.scoreImpact > 0 }
            val violationCount = logs.count { it.scoreImpact < 0 }
            val insight = StudentInsightBuilder.build(logs, daily)
            _studentAnalytics.value = StudentAnalytics(
                student = student,
                periodLabel = DeputyPeriodRange.label(cfg),
                totalScore = totals.totalScore,
                eventCount = totals.eventCount,
                praiseCount = praiseCount,
                violationCount = violationCount,
                dailyScores = daily,
                insight = insight,
            )
        }
    }

    fun toggleRole() {
        viewModelScope.launch {
            val current = userPreferencesRepository.appRole.first()
            val next = when (current) {
                AppRole.DEPUTY -> AppRole.TEACHER
                AppRole.TEACHER -> AppRole.DEPUTY
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

    fun onNavigated() { _navigateTo.value = null }

    fun consumeReport() { _report.value = null }

    fun importFromUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _importing.value = true
            val total = uris.size
            val lines = try {
                buildString {
                    uris.forEachIndexed { index, uri ->
                        val fileName = resolveDisplayName(uri, index)
                        setImportProgress(index + 1, total, fileName, "Читаем файл…")
                        val bytes = readUriBytes(uri)
                        if (bytes == null) {
                            appendLine("$fileName: не удалось открыть файл.")
                            return@forEachIndexed
                        }
                        setImportProgress(index + 1, total, fileName, "Разбираем отметки…")
                        val result = deputyReportImporter.importFile(fileName, bytes)
                        if (result.errorMessage != null) {
                            appendLine("$fileName: ${result.errorMessage}")
                        } else {
                            val stats = result.stats!!
                            appendLine(formatImportSuccess(result.label, stats, result.parseWarnings))
                        }
                    }
                }.trim()
            } finally {
                setImportProgress(total, total, step = "Обновляем аналитику…")
                try {
                    loadDashboardStats()
                } finally {
                    _importing.value = false
                    _importStatusMessage.value = ""
                }
            }
            _report.value = lines.ifEmpty { "Файл не выбран или в нём нет данных." }
        }
    }

    private fun setImportProgress(current: Int, total: Int, fileName: String = "", step: String) {
        _importStatusMessage.value = when {
            fileName.isNotBlank() && total > 1 ->
                "$step\nФайл $current из $total · $fileName"
            fileName.isNotBlank() ->
                "$step\n$fileName"
            total > 1 ->
                "$step\n$current из $total"
            else ->
                step
        }
    }

    fun setImportBatchEnabled(batchId: String, enabled: Boolean) {
        viewModelScope.launch {
            catalogRepository.setImportBatchEnabled(batchId, enabled)
            refresh()
        }
    }

    fun setLocalDataEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDeputyLocalDataEnabled(enabled)
            refresh()
        }
    }

    fun deleteImportBatch(batchId: String) {
        viewModelScope.launch {
            catalogRepository.deleteImportBatch(batchId)
            refresh()
        }
    }

    private fun resolveDisplayName(uri: Uri, index: Int): String {
        val fromProvider = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
        return fromProvider?.takeIf { it.isNotBlank() } ?: "Файл ${index + 1}"
    }

    private suspend fun readUriBytes(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                if (bytes.size > MAX_IMPORT_BYTES) return@use null
                bytes
            }
        }.getOrNull()
    }

    companion object {
        private const val MAX_IMPORT_BYTES = 4 * 1024 * 1024

        private fun formatImportSuccess(
            teacherLabel: String,
            stats: BehaviorLogImportStats,
            parseWarnings: Int,
        ): String = buildString {
            append("$teacherLabel: добавлено ${stats.inserted} отметок")
            if (stats.studentsCreated > 0) append(", новых учеников ${stats.studentsCreated}")
            if (stats.skippedUnknownStudent > 0) {
                append(", не найдено учеников — ${stats.skippedUnknownStudent}")
            }
            if (stats.skippedDuplicate > 0) append(", повторов пропущено: ${stats.skippedDuplicate}")
            if (parseWarnings > 0) append(", строк с замечаниями: $parseWarnings")
            append('.')
        }
    }
}
