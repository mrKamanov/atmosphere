/**
 * Список учеников учителя.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.students

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.golpom.atmosphere.data.local.entity.StudentEntity
import ru.golpom.atmosphere.domain.student.StudentIdentity
import ru.golpom.atmosphere.ui.importing.ImportFeedbackEffect
import ru.golpom.atmosphere.ui.theme.AtmosphereDataLoadingBar
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.ChipBg
import ru.golpom.atmosphere.ui.theme.LessonGreen
import ru.golpom.atmosphere.ui.theme.SurfaceBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
    viewModel: StudentsViewModel,
    onBack: () -> Unit,
    onOpenStudentProfile: (studentId: String) -> Unit,
) {
    val allStudents by viewModel.students.collectAsStateWithLifecycle()
    val currentFilter by viewModel.filter.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val importStatusMessage by viewModel.importStatusMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val students = remember(allStudents, searchQuery) {
        if (searchQuery.isBlank()) allStudents
        else allStudents.filter { s ->
            "${s.lastName} ${s.firstName} ${s.classId}".contains(searchQuery, ignoreCase = true)
        }
    }

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
        message = userMessage,
        onConsumed = { viewModel.consumeMessage() },
        onShortMessage = { snackbarHostState.showSnackbar(it) },
    )

    if (showAddDialog) {
        AddStudentDialog(
            classIds = classes.map { it.classId },
            onDismiss = { showAddDialog = false },
            onConfirm = { firstName, lastName, classId ->
                viewModel.addStudent(firstName, lastName, classId)
                showAddDialog = false
            },
        )
    }

    Scaffold(
        containerColor = SurfaceBg,
        topBar = {
            TopAppBar(
                title = { Text("Мои ученики", fontWeight = FontWeight.SemiBold) },
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
                                onClick = { showMenu = false; templateSaver.launch("Ученики_шаблон.xlsx") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Загрузить из шаблона") },
                                onClick = { showMenu = false; templatePicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) },
                                leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Загрузить из Моя школа") },
                                onClick = { showMenu = false; mySchoolPicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) },
                                leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null) },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg),
            )
        },
        floatingActionButton = {
            if (currentFilter != StudentFilter.ARCHIVED) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = LessonGreen,
                    contentColor = CardBg,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить ученика")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize()) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                placeholder = { Text("Поиск по имени или классу") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg,
                    cursorColor = TextPrimary,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StudentFilter.entries.forEach { f ->
                    val isSelected = f == currentFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) LessonGreen else ChipBg)
                            .clickable { viewModel.setFilter(f) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            when (f) {
                                StudentFilter.ACTIVE -> "Активные"
                                StudentFilter.ARCHIVED -> "Архив"
                                StudentFilter.ALL -> "Все"
                            },
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) CardBg else TextSecondary,
                        )
                    }
                }
            }

            if (students.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(48.dp), tint = ChipBg)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            when (currentFilter) {
                                StudentFilter.ACTIVE -> "Нет активных учеников"
                                StudentFilter.ARCHIVED -> "Архив пуст"
                                StudentFilter.ALL -> "Нет учеников"
                            },
                            fontSize = 16.sp, color = TextSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (currentFilter == StudentFilter.ACTIVE) "Нажмите + чтобы добавить"
                            else "",
                            fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.6f),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    items(students, key = { it.studentId }) { student ->
                        StudentCard(
                            student = student,
                            isArchived = student.status == "ARCHIVED",
                            onClick = { onOpenStudentProfile(student.studentId) },
                            onMove = { newClass -> viewModel.moveStudent(student.studentId, newClass) },
                            onArchive = { viewModel.archiveStudent(student.studentId) },
                            onHardDelete = { viewModel.hardDeleteStudent(student.studentId) },
                            onRestore = { viewModel.restoreStudent(student.studentId) },
                            classIds = classes.map { it.classId },
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

@Composable
private fun StudentCard(
    student: StudentEntity,
    isArchived: Boolean,
    onClick: () -> Unit,
    onMove: (String) -> Unit,
    onArchive: () -> Unit,
    onHardDelete: () -> Unit,
    onRestore: () -> Unit,
    classIds: List<String>,
) {
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showMoveDialog) {
        MoveClassDialog(
            currentClassId = student.classId,
            classIds = classIds,
            onDismiss = { showMoveDialog = false },
            onConfirm = { newClass ->
                onMove(newClass)
                showMoveDialog = false
            },
        )
    }
    if (showDeleteDialog) {
        if (isArchived) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удалить навсегда?", fontWeight = FontWeight.SemiBold) },
                text = { Text("${student.lastName} ${student.firstName} и вся история отметок будут безвозвратно удалены.") },
                confirmButton = {
                    TextButton(onClick = { onHardDelete(); showDeleteDialog = false }) { Text("Удалить навсегда") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
                },
            )
        } else {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удалить ученика?", fontWeight = FontWeight.SemiBold) },
                text = { Text("${student.lastName} ${student.firstName}. История отметок сохранится.") },
                confirmButton = {
                    TextButton(onClick = { onArchive(); showDeleteDialog = false }) { Text("Архивировать") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
                        TextButton(onClick = { onHardDelete(); showDeleteDialog = false }) {
                            Text("Удалить навсегда", color = androidx.compose.ui.graphics.Color(0xFFDC2626))
                        }
                    }
                },
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isArchived) 2.dp else 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isArchived) Icons.Default.RestoreFromTrash else Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isArchived) TextSecondary.copy(alpha = 0.5f) else TextSecondary,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${student.lastName} ${student.firstName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isArchived) TextSecondary.copy(alpha = 0.7f) else TextPrimary,
                )
                Text(student.classId, fontSize = 13.sp, color = TextSecondary)
            }
            if (isArchived) {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Unarchive, contentDescription = "Восстановить", modifier = Modifier.size(20.dp), tint = LessonGreen)
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Удалить навсегда", modifier = Modifier.size(20.dp), tint = TextSecondary.copy(alpha = 0.5f))
                }
            } else {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(20.dp), tint = TextSecondary.copy(alpha = 0.5f))
                }
                TextButton(onClick = { showMoveDialog = true }) {
                    Text("Перевести", fontSize = 13.sp, color = LessonGreen)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStudentDialog(
    classIds: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (firstName: String, lastName: String, classId: String) -> Unit,
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(classIds.firstOrNull() ?: "") }
    var expanded by remember { mutableStateOf(false) }
    val normalizedLast = StudentIdentity.normalizeName(lastName)
    val normalizedFirst = StudentIdentity.normalizeName(firstName)
    val valid = normalizedFirst.isNotBlank() && normalizedLast.isNotBlank() && selectedClass.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить ученика", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    singleLine = true,
                    label = { Text("Фамилия") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ChipBg,
                        unfocusedContainerColor = ChipBg,
                        cursorColor = TextPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
                TextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    singleLine = true,
                    label = { Text("Имя") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ChipBg,
                        unfocusedContainerColor = ChipBg,
                        cursorColor = TextPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
                if (lastName.isNotBlank() || firstName.isNotBlank()) {
                    Text(
                        "Будет: $normalizedLast $normalizedFirst",
                        fontSize = 13.sp,
                        color = LessonGreen,
                    )
                }
                if (classIds.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        TextField(
                            value = selectedClass,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Класс") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(),
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
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(firstName, lastName, selectedClass) }, enabled = valid) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveClassDialog(
    currentClassId: String,
    classIds: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selectedClass by remember { mutableStateOf(currentClassId) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Перевести ученика", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text("Текущий класс: $currentClassId", fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    TextField(
                        value = selectedClass,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Новый класс") },
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
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedClass) }, enabled = selectedClass != currentClassId && selectedClass.isNotBlank()) {
                Text("Перевести")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
