/**
 * Диалог «Новый учебный год»: предпросмотр перевода классов и выбор, что делать с выпускниками.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.classes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import ru.golpom.atmosphere.domain.studentsRu
import ru.golpom.atmosphere.domain.schoolyear.GraduateAction
import ru.golpom.atmosphere.domain.schoolyear.SchoolYearRollover
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.ChipBg
import ru.golpom.atmosphere.ui.theme.LessonGreen
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun SchoolYearRolloverDialog(
    state: SchoolYearDialogState,
    onGraduationGradeChange: (Int) -> Unit,
    onGraduateActionChange: (GraduateAction) -> Unit,
    onShiftScheduleChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val plan = state.plan

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Upgrade, contentDescription = null, tint = LessonGreen) },
        title = { Text("Новый учебный год", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.recentRolloverDate?.let { date ->
                    WarningBox(
                        "Классы уже переводили ${date.format(dateFormat)}. " +
                            "Повторный перевод поднимет их ещё на год.",
                    )
                }

                Text(
                    "Каждый класс станет на год старше, буква сохранится: 5-А → 6-А. " +
                        "Отметки прошлых лет остаются за прежним классом — отчёты за прошлый год не изменятся.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                )

                SectionTitle("Последний класс в школе")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SchoolYearRollover.GRADUATION_GRADE_OPTIONS.forEach { grade ->
                        FilterChip(
                            selected = plan.graduationGrade == grade,
                            onClick = { onGraduationGradeChange(grade) },
                            label = { Text("$grade-й") },
                            enabled = !state.applying,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AtmosphereBrand.PositiveSoft,
                                selectedLabelColor = LessonGreen,
                            ),
                        )
                    }
                }

                if (plan.isEmpty) {
                    Text(
                        "Нет классов с учениками, которые можно перевести.",
                        fontSize = 14.sp,
                        color = TextPrimary,
                    )
                }

                if (plan.promotions.isNotEmpty()) {
                    SectionTitle("Перевод · ${studentsLabel(plan.promotedStudents)}")
                    plan.promotions.forEach { p ->
                        ClassRow(
                            from = p.fromClassId,
                            to = p.toClassId,
                            note = if (p.studentCount > 0) studentsLabel(p.studentCount) else "только расписание",
                        )
                    }
                }

                if (plan.graduations.isNotEmpty()) {
                    SectionTitle("Выпуск · ${studentsLabel(plan.graduatingStudents)}")
                    plan.graduations.forEach { g ->
                        ClassRow(
                            from = g.classId,
                            to = null,
                            note = if (g.studentCount > 0) studentsLabel(g.studentCount) else "только расписание",
                        )
                    }
                    if (plan.graduatingStudents > 0) {
                        Column {
                            ChoiceRow(
                                selected = state.graduateAction == GraduateAction.ARCHIVE,
                                enabled = !state.applying,
                                title = "В архив",
                                subtitle = "Скроются из списков, история отметок сохранится",
                                onClick = { onGraduateActionChange(GraduateAction.ARCHIVE) },
                            )
                            ChoiceRow(
                                selected = state.graduateAction == GraduateAction.DELETE,
                                enabled = !state.applying,
                                title = "Удалить",
                                subtitle = "Вместе с историей отметок, без возможности вернуть",
                                onClick = { onGraduateActionChange(GraduateAction.DELETE) },
                            )
                        }
                    }
                }

                if (plan.unrecognized.isNotEmpty()) {
                    Text(
                        "Не удалось определить номер класса, останутся как есть: " +
                            plan.unrecognized.joinToString(", "),
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                }

                if (!plan.isEmpty) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !state.applying) { onShiftScheduleChange(!state.shiftSchedule) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = state.shiftSchedule,
                            onCheckedChange = onShiftScheduleChange,
                            enabled = !state.applying,
                            colors = CheckboxDefaults.colors(checkedColor = LessonGreen),
                        )
                        Column {
                            Text("Перевести классы и в расписании", fontSize = 14.sp, color = TextPrimary)
                            Text(
                                "Уроки выпускных классов удалятся",
                                fontSize = 12.sp,
                                color = TextSecondary,
                            )
                        }
                    }

                    Text(
                        "После перевода добавьте новых учеников и уберите тех, кто ушёл из школы.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                    Text(
                        "Завуч объединяет учеников по классу и ФИО. Чтобы в его сводке ученик " +
                            "не раздвоился, коллегам тоже нужно перевести свои классы до первых отметок нового года.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                }
            }
        },
        confirmButton = {
            if (state.applying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(2.dp),
                    strokeWidth = 2.dp,
                    color = LessonGreen,
                )
            } else {
                val deleting = state.graduateAction == GraduateAction.DELETE && plan.graduatingStudents > 0
                TextButton(onClick = onConfirm, enabled = !plan.isEmpty) {
                    Text(
                        if (deleting) "Перевести и удалить выпускников" else "Перевести",
                        color = if (deleting) AtmosphereBrand.NegativeDeep else LessonGreen,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.applying) { Text("Отмена") }
        },
    )
}

/** Подсказка над списком классов летом, пока год ещё не переведён. */
@Composable
fun SchoolYearHintCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AtmosphereBrand.PositiveSoft),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Upgrade, contentDescription = null, tint = LessonGreen, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Новый учебный год", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    "Переведите классы на год вперёд, выпускников — в архив",
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LessonGreen)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
}

@Composable
private fun ClassRow(from: String, to: String?, note: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChipBg, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(from, fontWeight = FontWeight.Medium, color = TextPrimary)
        Spacer(Modifier.width(8.dp))
        if (to != null) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "в",
                modifier = Modifier.size(16.dp),
                tint = TextSecondary,
            )
            Spacer(Modifier.width(8.dp))
            Text(to, fontWeight = FontWeight.Medium, color = LessonGreen)
        } else {
            Text("выпуск", color = TextSecondary)
        }
        Spacer(Modifier.weight(1f))
        Text(note, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun ChoiceRow(
    selected: Boolean,
    enabled: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(selectedColor = LessonGreen),
        )
        Column {
            Text(title, fontSize = 14.sp, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun WarningBox(text: String) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .background(AtmosphereBrand.NegativeSoft, RoundedCornerShape(10.dp))
            .padding(12.dp),
        fontSize = 13.sp,
        color = AtmosphereBrand.NegativeDeep,
    )
}

private fun studentsLabel(count: Int): String = studentsRu(count)
