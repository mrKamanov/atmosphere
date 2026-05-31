/**
 * Карточка класса учителя: ученики и сводка.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.classes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.golpom.atmosphere.data.local.entity.StudentEntity
import ru.golpom.atmosphere.ui.export.ExportPeriodDialog
import ru.golpom.atmosphere.ui.export.rememberTeacherExportHandoff
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.fabListBottomPadding
import ru.golpom.atmosphere.ui.theme.ChipBg
import ru.golpom.atmosphere.ui.theme.LessonGreen
import ru.golpom.atmosphere.ui.theme.SurfaceBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

private val ScoreGreen = Color(0xFF059669)
private val ScoreRed = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classId: String,
    viewModel: ClassDetailViewModel,
    onBack: () -> Unit,
    onOpenStudentProfile: (studentId: String) -> Unit,
) {
    LaunchedEffect(classId) {
        viewModel.setClassId(classId)
        viewModel.selectSubject(null)
    }
    val students by viewModel.students.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val selectedSubject by viewModel.selectedSubject.collectAsStateWithLifecycle()
    val scores by viewModel.scores.collectAsStateWithLifecycle()
    var showExportDialog by remember { mutableStateOf(false) }
    var exportSecurityDefaults by remember { mutableStateOf(ru.golpom.atmosphere.ui.export.ExportSecurityDefaults()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showHandoff = rememberTeacherExportHandoff { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    LaunchedEffect(showExportDialog) {
        if (showExportDialog) exportSecurityDefaults = viewModel.exportSecurityDefaults()
    }

    Scaffold(
        containerColor = SurfaceBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Класс $classId", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Выгрузить класс", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SubjectTab(
                    label = "Общая",
                    selected = selectedSubject == null,
                    onClick = { viewModel.selectSubject(null) },
                )
                subjects.forEach { subj ->
                    SubjectTab(
                        label = subj,
                        selected = selectedSubject == subj,
                        onClick = { viewModel.selectSubject(subj) },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = fabListBottomPadding(),
            ) {
                items(students, key = { it.studentId }) { student ->
                    StudentScoreCard(
                        student = student,
                        score = scores[student.studentId] ?: 0,
                        onClick = { onOpenStudentProfile(student.studentId) },
                    )
                }
            }
        }
    }

    if (showExportDialog) {
        ExportPeriodDialog(
            title = "Выгрузка класса $classId",
            securityDefaults = exportSecurityDefaults,
            onConfirm = { period, options ->
                showExportDialog = false
                viewModel.rememberExportSecurityChoices(options)
                scope.launch {
                    showHandoff(viewModel.exportClass(period, options))
                }
            },
            onDismiss = { showExportDialog = false },
        )
    }
}

@Composable
private fun SubjectTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) LessonGreen else ChipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) CardBg else TextSecondary,
        )
    }
}

@Composable
private fun StudentScoreCard(student: StudentEntity, score: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(24.dp), tint = TextSecondary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${student.lastName} ${student.firstName}", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (score >= 0) ScoreGreen.copy(alpha = 0.15f) else ScoreRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (score > 0) "+$score" else "$score",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (score >= 0) ScoreGreen else ScoreRed,
                )
            }
        }
    }
}
