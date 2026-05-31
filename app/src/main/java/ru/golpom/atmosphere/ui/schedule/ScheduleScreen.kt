/**
 * Недельное расписание учителя.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.schedule

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.golpom.atmosphere.data.local.entity.ScheduleEntryEntity
import ru.golpom.atmosphere.ui.importing.ImportFeedbackEffect
import ru.golpom.atmosphere.ui.theme.AtmosphereDataLoadingBar
import ru.golpom.atmosphere.domain.student.StudentIdentity
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.ChipBg
import ru.golpom.atmosphere.ui.theme.LessonGreen
import ru.golpom.atmosphere.ui.theme.SurfaceBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

private val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
private val TimeRed = Color(0xFFDC2626)

private fun minutesToTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}

private fun parseTime(s: String): Int? {
    val parts = s.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

private fun normalizeSubject(input: String): String {
    var t = input.trim()
    if (t.endsWith("внеурочная деятельность", ignoreCase = true)) {
        t = t.substring(0, t.length - "внеурочная деятельность".length).trim()
    }
    if (t.isEmpty()) return t
    return t.substring(0, 1).uppercase() + t.substring(1).lowercase()
}

private fun hasConflict(
    start: Int, end: Int, day: Int,
    entries: List<ScheduleEntryEntity>,
    excludeId: Long? = null,
): Boolean = entries.any { it.dayOfWeek == day && it.id != excludeId && start < it.endTimeMinutes && end > it.startTimeMinutes }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onBack: () -> Unit,
    onOpenLesson: (classId: String, subjectKey: String) -> Unit,
) {
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val importResult by viewModel.importResult.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val importStatusMessage by viewModel.importStatusMessage.collectAsStateWithLifecycle()
    val dayEntries = allEntries.filter { it.dayOfWeek == selectedDay }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<ScheduleEntryEntity?>(null) }

    val templatePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importTemplate(it) }
    }
    val mySchoolPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importMySchool(it) }
    }
    val templateSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        uri?.let { uri ->
            viewModel.generateTemplate { bytes ->
                if (bytes != null) {
                    scope.launch {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { os -> os.write(bytes) }
                            snackbarHostState.showSnackbar("Шаблон сохранён")
                        } catch (t: Throwable) {
                            snackbarHostState.showSnackbar("Ошибка: ${t.message}")
                        }
                    }
                }
            }
        }
    }

    ImportFeedbackEffect(
        message = importResult,
        onConsumed = { viewModel.consumeImportResult() },
        onShortMessage = { snackbarHostState.showSnackbar(it) },
    )

    if (showDialog) {
        AddScheduleDialog(
            selectedDay = selectedDay,
            classIds = classes.map { it.classId },
            existingEntries = allEntries,
            initialEntry = null,
            onDismiss = { showDialog = false; editEntry = null },
            onSave = { subject, classId, startMin, endMin ->
                viewModel.addEntry(startMin, endMin, subject, classId)
                showDialog = false
            },
        )
    }
    editEntry?.let { entry ->
        AddScheduleDialog(
            selectedDay = selectedDay,
            classIds = classes.map { it.classId },
            existingEntries = allEntries,
            initialEntry = entry,
            onDismiss = { editEntry = null },
            onSave = { subject, classId, startMin, endMin ->
                viewModel.updateEntry(entry.copy(subjectKey = subject, classId = classId, startTimeMinutes = startMin, endTimeMinutes = endMin))
                editEntry = null
            },
        )
    }

    Scaffold(
        containerColor = SurfaceBg,
        topBar = {
            TopAppBar(
                title = { Text("Расписание", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Ещё", tint = TextPrimary)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Скачать шаблон") },
                                onClick = { showMenu = false; templateSaver.launch("Расписание_шаблон.xlsx") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Загрузить из шаблона") },
                                onClick = { showMenu = false; templatePicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) },
                                leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Загрузить из «Моя школа»") },
                                onClick = { showMenu = false; mySchoolPicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) },
                                leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Очистить расписание") },
                                onClick = { showMenu = false; viewModel.clearAll() },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            )
                        }
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
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Row(Modifier.fillMaxWidth().pointerInput(selectedDay) {
                var dragTotal = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragTotal += dragAmount
                        if (dragTotal > 120f && selectedDay > 1) {
                            dragTotal = 0f
                            viewModel.selectDay(selectedDay - 1)
                        } else if (dragTotal < -120f && selectedDay < 6) {
                            dragTotal = 0f
                            viewModel.selectDay(selectedDay + 1)
                        }
                    },
                )
            }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                dayNames.forEachIndexed { index, name ->
                    val day = index + 1
                    val isSelected = day == selectedDay
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) LessonGreen else ChipBg)
                            .clickable { viewModel.selectDay(day) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            name,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) CardBg else TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (dayEntries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(48.dp), tint = ChipBg)
                        Spacer(Modifier.height(12.dp))
                        Text("Нет уроков", fontSize = 16.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text("Нажмите + чтобы добавить", fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.6f))
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    items(dayEntries, key = { it.id }) { entry ->
                        ScheduleSlotCard(
                            entry = entry,
                            onClick = { onOpenLesson(entry.classId, entry.subjectKey) },
                            onEdit = { editEntry = entry },
                            onDelete = { viewModel.deleteEntry(entry.id) },
                        )
                    }
                }
            }
        }

            AtmosphereDataLoadingBar(
                visible = importing,
                message = importStatusMessage.ifBlank { "Загружаем данные…" },
                containerColor = SurfaceBg.copy(alpha = 0.88f),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val durationOptions = listOf(30, 35, 40, 45)

@Composable
private fun AddScheduleDialog(
    selectedDay: Int,
    classIds: List<String>,
    existingEntries: List<ScheduleEntryEntity>,
    initialEntry: ScheduleEntryEntity?,
    onDismiss: () -> Unit,
    onSave: (subject: String, classId: String, startMin: Int, endMin: Int) -> Unit,
) {
    val isEdit = initialEntry != null
    val defaultStart = remember(existingEntries, selectedDay, initialEntry) {
        if (initialEntry != null) minutesToTime(initialEntry.startTimeMinutes)
        else {
            val last = existingEntries
                .filter { it.dayOfWeek == selectedDay }
                .maxByOrNull { it.endTimeMinutes }
            if (last != null) minutesToTime(last.endTimeMinutes + 10) else "09:00"
        }
    }
    val defaultEnd = remember(initialEntry) { if (initialEntry != null) minutesToTime(initialEntry.endTimeMinutes) else "" }
    var startText by remember { mutableStateOf(defaultStart) }
    var endText by remember { mutableStateOf(defaultEnd) }
    var subjectText by remember { mutableStateOf(initialEntry?.subjectKey ?: "") }
    var classText by remember { mutableStateOf(initialEntry?.classId ?: "") }
    var useCustomClass by remember { mutableStateOf(initialEntry?.classId != null && initialEntry.classId !in classIds) }
    var selectedDuration by remember { mutableStateOf<Int?>(null) }

    val startMin = parseTime(startText)
    val endMin = parseTime(endText)
    val normalizedSubject = normalizeSubject(subjectText)
    val normalizedClass = StudentIdentity.normalizeClassId(classText)
    val timeConflict = startMin != null && endMin != null && startMin < endMin &&
        hasConflict(startMin, endMin, selectedDay, existingEntries, initialEntry?.id)
    val classValid = if (isEdit) normalizedClass.isNotBlank() || initialEntry?.classId.isNullOrBlank() else normalizedClass.isNotBlank()
    val valid = normalizedSubject.isNotBlank() && classValid &&
        startMin != null && endMin != null && startMin < endMin && !timeConflict

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Редактировать урок" else "Добавить урок", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("День: ${dayNames.getOrNull(selectedDay - 1) ?: ""}", fontSize = 14.sp, color = TextSecondary)

                Text("Предмет", fontSize = 12.sp, color = TextSecondary)
                TextField(
                    value = subjectText,
                    onValueChange = { subjectText = it },
                    singleLine = true,
                    placeholder = { Text("География") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ChipBg,
                        unfocusedContainerColor = ChipBg,
                        cursorColor = TextPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
                if (subjectText.isNotBlank()) {
                    Text("Будет: $normalizedSubject", fontSize = 13.sp, color = LessonGreen)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Начало", fontSize = 12.sp, color = TextSecondary)
                        TextField(
                            value = startText,
                            onValueChange = { startText = it; selectedDuration = null },
                            singleLine = true,
                            placeholder = { Text("09:00") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = ChipBg,
                                unfocusedContainerColor = if (timeConflict) TimeRed.copy(alpha = 0.1f) else ChipBg,
                                cursorColor = TextPrimary,
                                focusedTextColor = if (timeConflict) TimeRed else TextPrimary,
                                unfocusedTextColor = if (timeConflict) TimeRed else TextPrimary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Конец", fontSize = 12.sp, color = TextSecondary)
                        TextField(
                            value = endText,
                            onValueChange = { endText = it; selectedDuration = null },
                            singleLine = true,
                            placeholder = { Text("09:45") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = ChipBg,
                                unfocusedContainerColor = if (timeConflict) TimeRed.copy(alpha = 0.1f) else ChipBg,
                                cursorColor = TextPrimary,
                                focusedTextColor = if (timeConflict) TimeRed else TextPrimary,
                                unfocusedTextColor = if (timeConflict) TimeRed else TextPrimary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                }

                Text("Длительность", fontSize = 12.sp, color = TextSecondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    durationOptions.forEach { dur ->
                        val isSelected = selectedDuration == dur
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) LessonGreen else ChipBg)
                                .clickable {
                                    selectedDuration = dur
                                    startMin?.let { s ->
                                        endText = minutesToTime(s + dur)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$dur", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                color = if (isSelected) CardBg else TextPrimary)
                        }
                    }
                }

                if (timeConflict) {
                    Text("Время пересекается с другим уроком", fontSize = 12.sp, color = TimeRed)
                }

                Text("Класс", fontSize = 12.sp, color = TextSecondary)
                if (classIds.isEmpty()) {
                    Text("Создайте класс на главном экране", fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.6f))
                } else {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        classIds.forEach { id ->
                            val isSelected = !useCustomClass && classText == id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) LessonGreen else ChipBg)
                                    .clickable { classText = id; useCustomClass = false }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(id, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                    color = if (isSelected) CardBg else TextPrimary)
                            }
                        }
                    }
                }
                TextField(
                    value = classText,
                    onValueChange = { classText = it; useCustomClass = true },
                    singleLine = true,
                    placeholder = { Text("5-И") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ChipBg,
                        unfocusedContainerColor = ChipBg,
                        cursorColor = TextPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
                if (classText.isNotBlank()) {
                    Text("Будет: $normalizedClass", fontSize = 13.sp, color = LessonGreen)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(normalizedSubject, normalizedClass, startMin!!, endMin!!)
            }, enabled = valid) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun ScheduleSlotCard(entry: ScheduleEntryEntity, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(minutesToTime(entry.startTimeMinutes), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(minutesToTime(entry.endTimeMinutes), fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.subjectKey, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("Класс ${entry.classId}", fontSize = 13.sp, color = TextSecondary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(20.dp), tint = TextSecondary.copy(alpha = 0.5f))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(20.dp), tint = TextSecondary.copy(alpha = 0.5f))
            }
        }
    }
}
