/**
 * Состояние экрана расписания.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.schedule

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.golpom.atmosphere.data.excel.MySchoolExcelParser
import ru.golpom.atmosphere.data.excel.ScheduleExcelParser
import ru.golpom.atmosphere.data.local.entity.ScheduleEntryEntity
import ru.golpom.atmosphere.data.repository.CatalogRepository

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    val allEntries = catalogRepository.observeAllSchedule()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val classes = catalogRepository.observeTeacherClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedDay = MutableStateFlow(1)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _importResult = MutableStateFlow<String?>(null)
    val importResult: StateFlow<String?> = _importResult.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _importStatusMessage = MutableStateFlow("")
    val importStatusMessage: StateFlow<String> = _importStatusMessage.asStateFlow()

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun addEntry(startMinutes: Int, endMinutes: Int, subjectKey: String, classId: String) {
        viewModelScope.launch {
            catalogRepository.saveScheduleEntry(
                ScheduleEntryEntity(
                    dayOfWeek = _selectedDay.value,
                    startTimeMinutes = startMinutes,
                    endTimeMinutes = endMinutes,
                    subjectKey = subjectKey,
                    classId = classId,
                ),
            )
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            catalogRepository.deleteScheduleEntry(id)
        }
    }

    fun updateEntry(entry: ScheduleEntryEntity) {
        viewModelScope.launch {
            catalogRepository.updateScheduleEntry(entry)
        }
    }

    fun importTemplate(uri: Uri) {
        viewModelScope.launch {
            _importing.value = true
            _importStatusMessage.value = "Загружаем расписание из файла…"
            val result = try {
                withContext(Dispatchers.IO) {
                val input = context.contentResolver.openInputStream(uri) ?: return@withContext "Ошибка открытия файла"
                val parsed = ScheduleExcelParser.parseTemplate(input)
                if (parsed.rows.isEmpty()) {
                    if (parsed.errors.isNotEmpty()) parsed.errors.joinToString("\n")
                    else "Файл не содержит уроков"
                } else {
                    val entities = ScheduleExcelParser.toEntities(parsed.rows)
                    catalogRepository.ensureClassesExist(entities.map { it.classId }.toSet())
                    catalogRepository.replaceAllSchedule(entities)
                    val errMsg = if (parsed.errors.isNotEmpty()) "\n\nЗамечания:\n${parsed.errors.joinToString("\n")}" else ""
                    "Загружено ${parsed.rows.size} уроков$errMsg"
                }
            }
            } finally {
                _importing.value = false
                _importStatusMessage.value = ""
            }
            _importResult.value = result
        }
    }

    fun importMySchool(uri: Uri) {
        viewModelScope.launch {
            _importing.value = true
            _importStatusMessage.value = "Загружаем расписание из «Моя школа»…"
            val result = try {
                withContext(Dispatchers.IO) {
                val input = context.contentResolver.openInputStream(uri) ?: return@withContext "Ошибка открытия файла"
                val parsed = MySchoolExcelParser.parse(input)
                if (parsed.rows.isEmpty()) {
                    if (parsed.errors.isNotEmpty()) parsed.errors.joinToString("\n")
                    else "Файл не содержит уроков"
                } else {
                    val entities = MySchoolExcelParser.toEntities(parsed.rows)
                    catalogRepository.ensureClassesExist(entities.map { it.classId }.toSet())
                    catalogRepository.replaceAllSchedule(entities)
                    val errMsg = if (parsed.errors.isNotEmpty()) "\n\nЗамечания:\n${parsed.errors.joinToString("\n")}" else ""
                    "Загружено ${parsed.rows.size} уроков$errMsg"
                }
            }
            } finally {
                _importing.value = false
                _importStatusMessage.value = ""
            }
            _importResult.value = result
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            catalogRepository.replaceAllSchedule(emptyList())
            _importResult.value = "Расписание очищено"
        }
    }

    fun generateTemplate(callback: (ByteArray?) -> Unit) {
        viewModelScope.launch {
            _importing.value = true
            _importStatusMessage.value = "Готовим шаблон расписания…"
            val bytes = try {
                withContext(Dispatchers.IO) {
                    val baos = ByteArrayOutputStream()
                    try {
                        ScheduleExcelParser.generateTemplate(baos)
                        baos.toByteArray()
                    } catch (t: Throwable) {
                        _importResult.value = "Ошибка создания шаблона: ${t.message}"
                        null
                    }
                }
            } finally {
                _importing.value = false
                _importStatusMessage.value = ""
            }
            callback(bytes)
        }
    }

    fun consumeImportResult() {
        _importResult.value = null
    }

    fun findCurrentOrNext(): ScheduleEntryEntity? {
        val now = java.util.Calendar.getInstance()
        val currentDay = ((now.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7) + 1
        val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        val entries = allEntries.value
        return entries
            .filter { it.dayOfWeek == currentDay && it.startTimeMinutes <= currentMinutes && it.endTimeMinutes > currentMinutes }
            .minByOrNull { it.startTimeMinutes }
            ?: entries
                .filter { it.dayOfWeek == currentDay && it.startTimeMinutes > currentMinutes }
                .minByOrNull { it.startTimeMinutes }
    }
}
