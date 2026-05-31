/**
 * Список родительских собраний.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.meetings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import ru.golpom.atmosphere.data.local.entity.MeetingEntity
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.ChipBg
import ru.golpom.atmosphere.ui.theme.LessonGreen
import ru.golpom.atmosphere.ui.theme.SurfaceBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

private val WeekendColor = TextSecondary.copy(alpha = 0.45f)
private val MeetingBadge = Color(0xFF059669)
private val TodayBorder = Color(0xFF059669)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsScreen(
    viewModel: MeetingsViewModel,
    onBack: () -> Unit,
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val currentYearMonth by viewModel.currentYearMonth.collectAsStateWithLifecycle()
    val meetingsForDay by viewModel.meetingsForSelectedDay.collectAsStateWithLifecycle()
    val meetingsByDayCount by viewModel.meetingsByDayCount.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }
    var editMeeting by remember { mutableStateOf<MeetingEntity?>(null) }
    var selectedMeeting by remember { mutableStateOf<MeetingEntity?>(null) }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeMessage()
        }
    }

    selectedMeeting?.let { m ->
        MeetingDetailDialog(
            meeting = m,
            onDismiss = { selectedMeeting = null },
            onEdit = { selectedMeeting = null; editMeeting = m },
            onDelete = { selectedMeeting = null; viewModel.deleteMeeting(m.id) },
        )
    }

    if (showDialog || editMeeting != null) {
        MeetingDialog(
            initial = editMeeting,
            date = if (editMeeting != null)
                LocalDate.ofInstant(java.time.Instant.ofEpochMilli(editMeeting!!.dateTimeMillis), ZoneId.systemDefault())
            else selectedDate,
            classIds = classes.map { it.classId },
            onDismiss = { showDialog = false; editMeeting = null },
            onSave = { classId, date, time, topic, notes ->
                val m = editMeeting
                if (m != null) {
                    viewModel.updateMeeting(m.id, classId, date, time, topic, notes)
                } else {
                    viewModel.addMeeting(classId, date, time, topic, notes)
                }
                showDialog = false; editMeeting = null
            },
        )
    }

    Scaffold(
        containerColor = SurfaceBg,
        topBar = {
            TopAppBar(
                title = { Text("Родительские собрания", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = LessonGreen,
                contentColor = CardBg,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить собрание")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    ) {
                        Column {
                            CalendarHeader(
                                yearMonth = currentYearMonth,
                                onPrevious = { viewModel.previousMonth() },
                                onNext = { viewModel.nextMonth() },
                            )
                            HorizontalDivider(color = ChipBg, thickness = 1.dp)
                            CalendarGrid(
                                yearMonth = currentYearMonth,
                                selectedDate = selectedDate,
                                meetingCounts = meetingsByDayCount,
                                onDayClick = { viewModel.selectDate(it) },
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("ru"))),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        val cnt = meetingsByDayCount[selectedDate.dayOfMonth] ?: 0
                        if (cnt > 0) {
                            Text(
                                "$cnt собрани${if (cnt % 10 == 1 && cnt % 100 != 11) "е" else "й"}",
                                fontSize = 13.sp,
                                color = MeetingBadge,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (meetingsForDay.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(40.dp), tint = ChipBg)
                                Spacer(Modifier.height(10.dp))
                                Text("Нет собраний", fontSize = 15.sp, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(meetingsForDay, key = { it.id }) { meeting ->
                        MeetingCard(
                            meeting = meeting,
                            onClick = { selectedMeeting = meeting },
                            onEdit = { editMeeting = meeting },
                            onDelete = { viewModel.deleteMeeting(meeting.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(yearMonth: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    val monthName = yearMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru")))
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = TextSecondary)
        }
        Text(
            monthName.replaceFirstChar { it.uppercase() },
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    meetingCounts: Map<Int, Int>,
    onDayClick: (LocalDate) -> Unit,
) {
    val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val firstDayOfMonth = yearMonth.atDay(1).dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val today = LocalDate.now()

    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            dayLabels.forEachIndexed { idx, label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (idx >= 5) WeekendColor else TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        val cells = mutableListOf<CalendarCell>()
        repeat(firstDayOfMonth) { cells.add(CalendarCell.Empty) }
        for (day in 1..daysInMonth) cells.add(CalendarCell.Day(day))

        val rawRows = cells.chunked(7)
        val rows = if (rawRows.last().size < 7) {
            val last = rawRows.last()
            rawRows.dropLast(1) + listOf(last + List(7 - last.size) { CalendarCell.Empty })
        } else rawRows

        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { colIdx, cell ->
                    Box(
                        Modifier.weight(1f).padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (cell) {
                            is CalendarCell.Empty -> {}
                            is CalendarCell.Day -> {
                                val date = yearMonth.atDay(cell.day)
                                val isSelected = date == selectedDate
                                val isToday = date == today
                                val count = meetingCounts[cell.day] ?: 0
                                val isWeekend = colIdx >= 5

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .then(
                                            if (isSelected) Modifier.background(LessonGreen)
                                            else if (isToday) Modifier.border(1.5.dp, TodayBorder, RoundedCornerShape(10.dp))
                                            else Modifier
                                        )
                                        .clickable { onDayClick(date) }
                                        .padding(vertical = 5.dp),
                                ) {
                                    Text(
                                        cell.day.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> CardBg
                                            isWeekend -> WeekendColor
                                            else -> TextPrimary
                                        },
                                    )
                                    if (count > 0) {
                                        Spacer(Modifier.height(3.dp))
                                        if (count == 1) {
                                            Box(
                                                Modifier.size(5.dp).clip(RoundedCornerShape(2.5.dp)).background(
                                                    if (isSelected) CardBg.copy(alpha = 0.8f) else MeetingBadge,
                                                ),
                                            )
                                        } else {
                                            Text(
                                                count.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) CardBg.copy(alpha = 0.8f) else MeetingBadge,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class CalendarCell {
    data object Empty : CalendarCell()
    data class Day(val day: Int) : CalendarCell()
}

@Composable
private fun MeetingCard(meeting: MeetingEntity, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val time = remember(meeting.dateTimeMillis) {
        val zdt = java.time.Instant.ofEpochMilli(meeting.dateTimeMillis).atZone(ZoneId.systemDefault())
        zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LessonGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(time, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LessonGreen)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(meeting.topic, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary.copy(alpha = 0.5f))
                    Spacer(Modifier.width(4.dp))
                    Text(meeting.classId, fontSize = 13.sp, color = TextSecondary)
                }
            }
            if (meeting.notes.isNotBlank()) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(LessonGreen.copy(alpha = 0.5f)),
                )
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(18.dp), tint = TextSecondary.copy(alpha = 0.4f))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(18.dp), tint = TextSecondary.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun MeetingDetailDialog(meeting: MeetingEntity, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val zdt = remember(meeting.dateTimeMillis) {
        java.time.Instant.ofEpochMilli(meeting.dateTimeMillis).atZone(ZoneId.systemDefault())
    }
    val dayNames = listOf("понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье")
    val monthNames = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onEdit(); onDismiss() }) {
                    Text("Редактировать")
                }
                TextButton(onClick = { onDelete(); onDismiss() }) {
                    Text("Удалить", color = Color(0xFFDC2626))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
        title = {
            Text(meeting.topic, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${zdt.dayOfMonth} ${monthNames[zdt.monthValue - 1]} ${zdt.year}, ${dayNames[zdt.dayOfWeek.value - 1]}",
                        fontSize = 14.sp,
                        color = TextSecondary,
                    )
                }
                Text("${zdt.hour.toString().padStart(2, '0')}:${zdt.minute.toString().padStart(2, '0')} · ${meeting.classId}", fontSize = 14.sp, color = TextSecondary)
                if (meeting.notes.isNotBlank()) {
                    HorizontalDivider(color = ChipBg)
                    Text("Заметки", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Text(meeting.notes, fontSize = 14.sp, color = TextPrimary)
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeetingDialog(
    initial: MeetingEntity?,
    date: LocalDate,
    classIds: List<String>,
    onDismiss: () -> Unit,
    onSave: (classId: String, date: LocalDate, time: LocalTime, topic: String, notes: String) -> Unit,
) {
    var topic by remember { mutableStateOf(initial?.topic ?: "Родительское собрание") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var selectedClass by remember { mutableStateOf(initial?.classId ?: classIds.firstOrNull() ?: "") }
    var expanded by remember { mutableStateOf(false) }
    val initialTime = if (initial != null)
        LocalTime.ofInstant(java.time.Instant.ofEpochMilli(initial.dateTimeMillis), ZoneId.systemDefault())
    else LocalTime.of(18, 0)
    var hourText by remember { mutableStateOf(initialTime.hour.toString().padStart(2, '0')) }
    var minuteText by remember { mutableStateOf(initialTime.minute.toString().padStart(2, '0')) }
    val valid = topic.isNotBlank() && selectedClass.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) "Редактировать собрание" else "Добавить собрание", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    date.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("ru"))),
                    fontSize = 14.sp,
                    color = LessonGreen,
                    fontWeight = FontWeight.Medium,
                )

                TextField(
                    value = topic,
                    onValueChange = { topic = it },
                    singleLine = true,
                    label = { Text("Тема") },
                    placeholder = { Text("Родительское собрание") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ChipBg,
                        unfocusedContainerColor = ChipBg,
                        cursorColor = TextPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )

                if (classIds.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        TextField(
                            value = selectedClass,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Класс") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = ChipBg,
                                unfocusedContainerColor = ChipBg,
                                cursorColor = TextPrimary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            classIds.forEach { id ->
                                DropdownMenuItem(
                                    text = { Text(id) },
                                    onClick = { selectedClass = id; expanded = false },
                                )
                            }
                        }
                    }
                } else {
                    Text("Создайте класс на главном экране", fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.6f))
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = hourText,
                        onValueChange = { hourText = it.take(2).filter { c -> c.isDigit() } },
                        singleLine = true,
                        label = { Text("Час") },
                        modifier = Modifier.width(72.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ChipBg,
                            unfocusedContainerColor = ChipBg,
                            cursorColor = TextPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Text(":", fontSize = 18.sp, color = TextSecondary)
                    TextField(
                        value = minuteText,
                        onValueChange = { minuteText = it.take(2).filter { c -> c.isDigit() } },
                        singleLine = true,
                        label = { Text("Мин") },
                        modifier = Modifier.width(72.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ChipBg,
                            unfocusedContainerColor = ChipBg,
                            cursorColor = TextPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                TextField(
                    value = notes,
                    onValueChange = { if (it.length <= 500) notes = it },
                    singleLine = false,
                    maxLines = 3,
                    label = { Text("Заметки (необязательно)") },
                    supportingText = { Text("${notes.length}/500", fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.6f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ChipBg,
                        unfocusedContainerColor = ChipBg,
                        cursorColor = TextPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val h = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 18
                    val m = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    onSave(selectedClass, date, LocalTime.of(h, m), topic, notes)
                },
                enabled = valid,
            ) { Text(if (initial != null) "Сохранить" else "Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
