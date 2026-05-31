/**
 * Домашний экран учителя: баннер, расписание дня, быстрые действия.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.golpom.atmosphere.data.local.entity.ClassEntity
import ru.golpom.atmosphere.data.local.entity.MeetingEntity
import ru.golpom.atmosphere.data.local.entity.ScheduleEntryEntity
import ru.golpom.atmosphere.ui.notifications.GradePromptDialog
import ru.golpom.atmosphere.ui.notifications.NotificationListSheet
import ru.golpom.atmosphere.ui.notifications.NotificationsViewModel
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.ChipBg
import ru.golpom.atmosphere.ui.theme.LessonGreen
import ru.golpom.atmosphere.ui.theme.NavigationBarScrollSpacer
import ru.golpom.atmosphere.ui.theme.PrimaryBlue
import ru.golpom.atmosphere.ui.theme.SurfaceBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherHomeScreen(
    viewModel: TeacherHomeViewModel,
    onOpenLesson: (classId: String, subjectKey: String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHome: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenClasses: () -> Unit,
    onOpenClassDetail: (classId: String) -> Unit,
    onOpenStudents: () -> Unit,
    onOpenMeetings: () -> Unit,
) {
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val studentCount by viewModel.studentCount.collectAsStateWithLifecycle()
    val navigateTo by viewModel.navigateTo.collectAsStateWithLifecycle()
    val nextLesson by viewModel.nextLesson.collectAsStateWithLifecycle()
    val upcomingMeetingCount by viewModel.upcomingMeetingCount.collectAsStateWithLifecycle()
    val nextMeeting by viewModel.nextMeeting.collectAsStateWithLifecycle()
    val totalScore by viewModel.totalScore.collectAsStateWithLifecycle()
    val notificationsViewModel = hiltViewModel<NotificationsViewModel>()
    val notifications by notificationsViewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by notificationsViewModel.unreadCount.collectAsStateWithLifecycle()
    val gradePromptState by notificationsViewModel.showGradePrompt.collectAsStateWithLifecycle()
    val showNotifications = remember { mutableStateOf(false) }
    LaunchedEffect(navigateTo) {
        navigateTo?.let { route ->
            onNavigateToHome(route)
            viewModel.onNavigated()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = SurfaceBg,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {},
                actions = {
                    val iconMod = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.25f))
                    IconButton(onClick = { viewModel.toggleRole() }, modifier = iconMod) {
                        Icon(Icons.Default.Person, tint = TextPrimary, contentDescription = "Сменить роль")
                    }
                    IconButton(onClick = { showNotifications.value = true }, modifier = iconMod) {
                        Box {
                            Icon(Icons.Default.Notifications, tint = TextPrimary, contentDescription = "Уведомления")
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF5350)),
                                )
                            }
                        }
                    }
                    IconButton(onClick = onOpenSettings, modifier = iconMod) {
                        Icon(Icons.Default.Settings, tint = TextPrimary, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            ) {
                AtmosphereBanner(
                    totalScore = totalScore,
                    modifier = Modifier.fillMaxSize(),
                    bgColor = SurfaceBg,
                )

                Column(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = padding.calculateTopPadding() + 100.dp),
                ) {
                    Text("Атмосфера", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ChipBg)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text("Режим учителя", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    }
                }
            }

            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ScheduleCard(onClick = onOpenSchedule, onOpenLesson = onOpenLesson, nextLesson = nextLesson)
                ClassesCard(classes = classes, onOpenClassDetail = onOpenClassDetail, onOpenClasses = onOpenClasses)
                StudentsCard(studentCount = studentCount, onOpenStudents = onOpenStudents)
                MeetingsCard(
                    onClick = onOpenMeetings,
                    upcomingCount = upcomingMeetingCount,
                    nextMeeting = nextMeeting,
                )
            }
            NavigationBarScrollSpacer()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showNotifications.value) {
        ModalBottomSheet(
            onDismissRequest = { showNotifications.value = false },
            sheetState = sheetState,
            containerColor = CardBg,
        ) {
            NotificationListSheet(
                notifications = notifications,
                onDismiss = { notificationsViewModel.dismiss(it) },
                onDelete = { notificationsViewModel.delete(it) },
                onMarkRead = { notificationsViewModel.markRead(it) },
                onClose = { showNotifications.value = false },
            )
        }
    }

    gradePromptState?.let { state ->
        GradePromptDialog(
            state = state,
            onApply = { behaviorType, scoreImpact ->
                notificationsViewModel.applyBulkGrade(
                    studentIds = state.studentIds,
                    classId = state.classId,
                    subjectKey = state.subjectKey,
                    behaviorType = behaviorType,
                    scoreImpact = scoreImpact,
                )
            },
            onDismiss = { notificationsViewModel.dismissGradePrompt() },
        )
    }
}

@Composable
private fun BaseCard(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Box(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun CardIcon(icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(ChipBg),
        contentAlignment = Alignment.Center,
    ) { icon() }
}

@Composable
private fun CardTitleRow(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CardIcon { icon() }
        Spacer(Modifier.width(16.dp))
        Box(Modifier.weight(1f)) { title() }
        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = TextSecondary.copy(alpha = 0.4f))
    }
}



@Composable
private fun ScheduleCard(onClick: () -> Unit, onOpenLesson: (classId: String, subjectKey: String) -> Unit, nextLesson: ScheduleEntryEntity?) {
    BaseCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { nextLesson?.let { onOpenLesson(it.classId, it.subjectKey) } },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LessonGreen, contentColor = CardBg),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            ) {
                Text("Урок", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(20.dp), tint = TextSecondary)
                Spacer(Modifier.width(8.dp))
                Text(nextLessonText(nextLesson), fontSize = 14.sp, color = TextSecondary)
            }
        }
    }
}

private fun nextLessonText(lesson: ScheduleEntryEntity?): String {
    if (lesson == null) return "Нет ближайших уроков"
    val now = java.time.ZonedDateTime.now()
    val currentDay = (now.dayOfWeek.value + 6) % 7 + 1
    val prefix = if (lesson.dayOfWeek == currentDay) "сегодня" else dayName(lesson.dayOfWeek)
    val h = lesson.startTimeMinutes / 60
    val m = lesson.startTimeMinutes % 60
    val time = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    return "$prefix, $time, ${lesson.subjectKey}, ${lesson.classId}"
}

private fun dayName(day: Int): String = when (day) {
    1 -> "Пн"; 2 -> "Вт"; 3 -> "Ср"; 4 -> "Чт"; 5 -> "Пт"; 6 -> "Сб"; else -> ""
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassesCard(
    classes: List<ClassEntity>,
    onOpenClassDetail: (classId: String) -> Unit,
    onOpenClasses: () -> Unit,
) {
    BaseCard(onClick = onOpenClasses) {
        Column {
            CardTitleRow(
                icon = { Icon(Icons.Default.Class, contentDescription = null, Modifier.size(24.dp), tint = TextSecondary) },
                title = { Text("Мои классы", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            )
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                classes.forEach { cls ->
                    SuggestionChip(
                        onClick = { onOpenClassDetail(cls.classId) },
                        label = { Text(cls.classId, fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(14.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = ChipBg, labelColor = TextPrimary),
                        border = null,
                    )
                }
                SuggestionChip(
                    onClick = onOpenClasses,
                    label = { Text("+", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(14.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = ChipBg, labelColor = TextSecondary),
                    border = null,
                )
            }
        }
    }
}

@Composable
private fun StudentsCard(studentCount: Long, onOpenStudents: () -> Unit) {
    BaseCard(onClick = onOpenStudents) {
        Column {
            CardTitleRow(
                icon = { Icon(Icons.Default.Groups, contentDescription = null, Modifier.size(24.dp), tint = TextSecondary) },
                title = { Text("Мои ученики", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val label = when {
                    studentCount % 100 in 11L..14L -> "$studentCount учеников"
                    studentCount % 10 == 1L -> "$studentCount ученик"
                    studentCount % 10 in 2L..4L -> "$studentCount ученика"
                    else -> "$studentCount учеников"
                }
                Text(label, fontSize = 14.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenStudents) {
                    Icon(Icons.Default.Search, contentDescription = "Поиск", modifier = Modifier.size(20.dp), tint = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun MeetingsCard(onClick: () -> Unit, upcomingCount: Int, nextMeeting: MeetingEntity?) {
    BaseCard(onClick = onClick) {
        Column {
            CardTitleRow(
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, Modifier.size(24.dp), tint = TextSecondary) },
                title = { Text("Родительские собрания", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            )
            Spacer(Modifier.height(12.dp))
            val label = when {
                upcomingCount % 100 in 11..14 -> "$upcomingCount предстоящих"
                upcomingCount % 10 == 1 -> "$upcomingCount предстоящее"
                upcomingCount % 10 in 2..4 -> "$upcomingCount предстоящих"
                else -> "$upcomingCount предстоящих"
            }
            Text(label, fontSize = 14.sp, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val nextText = if (nextMeeting != null) {
                    val zdt = java.time.Instant.ofEpochMilli(nextMeeting.dateTimeMillis).atZone(java.time.ZoneId.systemDefault())
                    val dayNames = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
                    val dow = dayNames[zdt.dayOfWeek.value - 1]
                    "${nextMeeting.classId}, ${zdt.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))}, $dow, ${zdt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}"
                } else "Нет ближайших"
                Text(nextText, fontSize = 14.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                IconButton(onClick = onClick) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Календарь", modifier = Modifier.size(20.dp), tint = TextSecondary)
                }
            }
        }
    }
}
