/**
 * Состояние экрана собраний.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.meetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.golpom.atmosphere.data.local.entity.MeetingEntity
import ru.golpom.atmosphere.data.repository.CatalogRepository

@HiltViewModel
class MeetingsViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val allMeetings = catalogRepository.observeMeetings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val classes = catalogRepository.observeTeacherClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentYearMonth = MutableStateFlow(YearMonth.now())
    val currentYearMonth: StateFlow<YearMonth> = _currentYearMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val meetingsForSelectedDay = combine(allMeetings, _selectedDate) { meetings, date ->
        val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        meetings.filter { it.dateTimeMillis in dayStart until dayEnd }.sortedBy { it.dateTimeMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val meetingsByDayCount = combine(allMeetings, _currentYearMonth) { meetings, ym ->
        val monthStart = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val monthEnd = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        meetings.filter { it.dateTimeMillis in monthStart until monthEnd }
            .groupBy { LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it.dateTimeMillis), ZoneId.systemDefault()).dayOfMonth }
            .mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun previousMonth() {
        _currentYearMonth.value = _currentYearMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentYearMonth.value = _currentYearMonth.value.plusMonths(1)
    }

    fun addMeeting(classId: String, date: LocalDate, time: LocalTime, topic: String, notes: String) {
        val dateTimeMillis = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        viewModelScope.launch {
            catalogRepository.addMeeting(
                MeetingEntity(
                    classId = classId,
                    dateTimeMillis = dateTimeMillis,
                    topic = topic.trim(),
                    notes = notes.trim(),
                ),
            )
            _userMessage.value = "Собрание добавлено"
        }
    }

    fun updateMeeting(id: Long, classId: String, date: LocalDate, time: LocalTime, topic: String, notes: String) {
        val dateTimeMillis = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        viewModelScope.launch {
            catalogRepository.updateMeeting(id, classId, dateTimeMillis, topic.trim(), notes.trim())
            _userMessage.value = "Собрание обновлено"
        }
    }

    fun deleteMeeting(id: Long) {
        viewModelScope.launch {
            catalogRepository.deleteMeeting(id)
            _userMessage.value = "Собрание удалено"
        }
    }

    fun consumeMessage() {
        _userMessage.value = null
    }
}
