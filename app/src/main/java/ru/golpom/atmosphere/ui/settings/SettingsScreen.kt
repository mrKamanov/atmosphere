/**
 * Настройки: роль, импорт, экспорт, локальные данные.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.launch
import ru.golpom.atmosphere.ui.export.ExportPeriodDialog
import ru.golpom.atmosphere.ui.export.ExportSecurityDefaults
import ru.golpom.atmosphere.ui.export.rememberTeacherExportHandoff
import ru.golpom.atmosphere.ui.lesson.subjectDisplayName
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.golpom.atmosphere.ui.legal.openPrivacyPolicy
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.SurfaceBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.NavigationBarScrollSpacer
import ru.golpom.atmosphere.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = hiltViewModel<SettingsViewModel>()
    var userName by remember { mutableStateOf("") }
    var teacherLastName by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        userName = viewModel.loadUserName()
        teacherLastName = viewModel.loadTeacherLastName()
    }
    val focusManager = LocalFocusManager.current
    val meetingReminders by viewModel.meetingRemindersEnabled.collectAsStateWithLifecycle()
    val lessonReminders by viewModel.lessonRemindersEnabled.collectAsStateWithLifecycle()
    val systemNotifications by viewModel.systemNotificationsEnabled.collectAsStateWithLifecycle()
    val clearResult by viewModel.clearResult.collectAsStateWithLifecycle()
    val teacherSubjects by viewModel.teacherSubjects.collectAsStateWithLifecycle()

    var showClearScoresDialog by remember { mutableStateOf(false) }
    var showClearArchiveDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingExportTarget by remember { mutableStateOf<SettingsExportTarget?>(null) }
    var selectedSubjectKey by remember { mutableStateOf<String?>(null) }
    var subjectMenuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val showHandoff = rememberTeacherExportHandoff { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(teacherSubjects) {
        if (selectedSubjectKey == null && teacherSubjects.isNotEmpty()) {
            selectedSubjectKey = teacherSubjects.first()
        }
    }

    LaunchedEffect(clearResult) {
        clearResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissClearResult()
        }
    }

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted: Boolean ->
        if (granted) {
            viewModel.setSystemNotifications(true)
        } else {
            viewModel.setSystemNotifications(false)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfaceBg,
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SectionHeader("Профиль")

            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Ваше имя") },
                        placeholder = { Text("Напр., Сергей Дмитриевич") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (!it.isFocused) viewModel.setUserName(userName) },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                        ),
                    )
                    OutlinedTextField(
                        value = teacherLastName,
                        onValueChange = { teacherLastName = it },
                        label = { Text("Ваша фамилия в отчёте") },
                        placeholder = { Text("Напр., Иванова") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (!it.isFocused) viewModel.setTeacherLastName(teacherLastName) },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.setTeacherLastName(teacherLastName)
                                focusManager.clearFocus()
                            },
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                        ),
                    )
                    Text(
                        "Завуч увидит эту фамилию внутри отчёта. В имени файла фамилия не указывается.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                }
            }

            SectionHeader("Уведомления")

            SettingsCard {
                SettingsToggle(
                    icon = Icons.Default.CalendarMonth,
                    title = "Напоминания о собраниях",
                    subtitle = "За день и за 3 часа до собрания",
                    checked = meetingReminders,
                    onCheckedChange = { viewModel.setMeetingReminders(it) },
                )
                Spacer(Modifier.height(12.dp))
                SettingsDivider()
                Spacer(Modifier.height(12.dp))
                SettingsToggle(
                    icon = Icons.Default.School,
                    title = "Напоминания об уроках",
                    subtitle = "После завершения урока без отметок",
                    checked = lessonReminders,
                    onCheckedChange = { viewModel.setLessonReminders(it) },
                )
                Spacer(Modifier.height(12.dp))
                SettingsDivider()
                Spacer(Modifier.height(12.dp))
                SettingsToggle(
                    icon = Icons.Default.Notifications,
                    title = "Системные уведомления",
                    subtitle = "Показывать, даже когда приложение закрыто",
                    checked = systemNotifications,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                viewModel.setSystemNotifications(true)
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            viewModel.setSystemNotifications(enabled)
                        }
                    },
                )
            }

            SectionHeader("Отправить отчёт завучу")

            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Сформируйте файл с отметками за нужный период. После создания его можно передать завучу " +
                            "кнопкой «Отправить» или сохранить на телефон. Открывается файл только в «Атмосфера» " +
                            "на устройстве завуча.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp,
                    )
                    ExportActionButton(
                        text = "Все мои классы",
                        description = "Отметки по классам из вашего расписания",
                        onClick = {
                            pendingExportTarget = SettingsExportTarget.AllClasses
                            showExportDialog = true
                        },
                    )
                    ExportActionButton(
                        text = "Все мои отметки",
                        description = "Все отметки на этом телефоне",
                        onClick = {
                            pendingExportTarget = SettingsExportTarget.AllData
                            showExportDialog = true
                        },
                    )
                    if (teacherSubjects.isNotEmpty()) {
                        Box {
                            OutlinedTextField(
                                value = selectedSubjectKey?.let { subjectDisplayName(it) } ?: "Выберите предмет",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Предмет") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = Color(0xFFE5E7EB),
                                ),
                            )
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .clickable { subjectMenuExpanded = true },
                            )
                            DropdownMenu(
                                expanded = subjectMenuExpanded,
                                onDismissRequest = { subjectMenuExpanded = false },
                            ) {
                                teacherSubjects.forEach { key ->
                                    DropdownMenuItem(
                                        text = { Text(subjectDisplayName(key)) },
                                        onClick = {
                                            selectedSubjectKey = key
                                            subjectMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        ExportActionButton(
                            text = "Один предмет",
                            description = selectedSubjectKey?.let { subjectDisplayName(it) } ?: "",
                            onClick = {
                                val key = selectedSubjectKey ?: return@ExportActionButton
                                pendingExportTarget = SettingsExportTarget.Subject(key)
                                showExportDialog = true
                            },
                        )
                    } else {
                        Text(
                            "Чтобы отправить отчёт по одному предмету, сначала добавьте уроки в расписание.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                        )
                    }
                }
            }

            SectionHeader("Данные")

            SettingsCard {
                Column {
                    DataActionRow(
                        title = "Очистить все отметки",
                        description = "Удалить баллы всех учеников по всем предметам. Классы, ученики и расписание сохранятся.",
                        actionLabel = "Очистить",
                        onClick = { showClearScoresDialog = true },
                    )
                    SettingsDivider()
                    DataActionRow(
                        title = "Очистить архив",
                        description = "Удалить архивных учеников и старые уведомления.",
                        actionLabel = "Очистить",
                        onClick = { showClearArchiveDialog = true },
                    )
                    SettingsDivider()
                    DataActionRow(
                        title = "Удалить все данные",
                        description = "Полностью сбросить приложение. Действие необратимо.",
                        actionLabel = "Удалить",
                        onClick = { showClearAllDialog = true },
                        isDestructive = true,
                    )
                }
            }

            Text(
                text = "Политика конфиденциальности",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { context.openPrivacyPolicy() }
                    .padding(vertical = 12.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2563EB),
                textAlign = TextAlign.Center,
            )
            NavigationBarScrollSpacer()
        }
    }

    if (showClearScoresDialog) {
        ConfirmDialog(
            title = "Очистить все отметки?",
            message = "Будут удалены баллы всех учеников по всем предметам. " +
                "Ученики, классы и расписание останутся нетронутыми.",
            confirmText = "Очистить",
            onConfirm = {
                viewModel.clearAllScores()
                showClearScoresDialog = false
            },
            onDismiss = { showClearScoresDialog = false },
        )
    }

    if (showClearArchiveDialog) {
        ConfirmDialog(
            title = "Очистить архив?",
            message = "Будут удалены архивные ученики и отклонённые уведомления. " +
                "Активные ученики, отметки, классы и расписание останутся.",
            confirmText = "Очистить",
            onConfirm = {
                viewModel.clearArchive()
                showClearArchiveDialog = false
            },
            onDismiss = { showClearArchiveDialog = false },
        )
    }

    if (showClearAllDialog) {
        ConfirmDialog(
            title = "Удалить все данные?",
            message = "Будут удалены абсолютно все данные: классы, ученики, отметки, " +
                "расписание, собрания, уведомления. Это действие необратимо.",
            confirmText = "Удалить всё",
            onConfirm = {
                viewModel.clearAllData()
                showClearAllDialog = false
            },
            onDismiss = { showClearAllDialog = false },
            isDestructive = true,
        )
    }

    if (showExportDialog) {
        ExportPeriodDialog(
            title = when (val target = pendingExportTarget) {
                SettingsExportTarget.AllClasses -> "Отчёт: все мои классы"
                SettingsExportTarget.AllData -> "Отчёт: все мои отметки"
                is SettingsExportTarget.Subject -> "Отчёт: ${subjectDisplayName(target.subjectKey)}"
                null -> "Создание отчёта"
            },
            securityDefaults = ExportSecurityDefaults(),
            onConfirm = { period, options ->
                showExportDialog = false
                val target = pendingExportTarget
                pendingExportTarget = null
                viewModel.rememberExportSecurityChoices(options)
                scope.launch {
                    val payload = when (target) {
                        SettingsExportTarget.AllClasses -> viewModel.exportAllClasses(period, options)
                        SettingsExportTarget.AllData -> viewModel.exportAllData(period, options)
                        is SettingsExportTarget.Subject -> viewModel.exportSubject(target.subjectKey, period, options)
                        null -> return@launch
                    }
                    showHandoff(payload)
                }
            },
            onDismiss = {
                showExportDialog = false
                pendingExportTarget = null
            },
        )
    }

}

private sealed interface SettingsExportTarget {
    data object AllClasses : SettingsExportTarget
    data object AllData : SettingsExportTarget
    data class Subject(val subjectKey: String) : SettingsExportTarget
}

@Composable
private fun ExportActionButton(
    text: String,
    description: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2563EB).copy(alpha = 0.12f),
            contentColor = Color(0xFF2563EB),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
                if (description.isNotBlank()) {
                    Text(
                        description,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Color(0xFF2563EB).copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary.copy(alpha = 0.7f))
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.padding(end = 16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Spacer(Modifier.padding(start = 8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB)),
        )
    }
}

@Composable
private fun SettingsDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color = Color(0xFFE5E7EB),
    )
}

@Composable
private fun DataActionRow(
    title: String,
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    val actionColor = if (isDestructive) AtmosphereBrand.Negative else AtmosphereBrand.InkSoft
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                lineHeight = 20.sp,
            )
            Text(
                description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp,
            )
        }
        Text(
            actionLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = actionColor,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(message, fontSize = 14.sp, color = TextSecondary) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDestructive) Color(0xFFDC2626) else Color(0xFF2563EB),
                ),
            ) {
                Text(confirmText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
}
