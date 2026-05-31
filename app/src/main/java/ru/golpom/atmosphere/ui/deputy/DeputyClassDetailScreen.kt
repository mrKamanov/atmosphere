/**
 * Аналитика класса для завуча: инсайты, недельный график, календарь, матрица ученик × предмет.
 */
package ru.golpom.atmosphere.ui.deputy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.golpom.atmosphere.data.export.deputy.DeputyAnalyticsReportExporter
import ru.golpom.atmosphere.data.local.model.StudentSubjectCell
import ru.golpom.atmosphere.ui.export.rememberDeputyExportHandoff
import ru.golpom.atmosphere.ui.lesson.subjectDisplayName
import ru.golpom.atmosphere.ui.theme.listBottomPadding
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeputyClassDetailScreen(
    classId: String,
    viewModel: DeputyClassDetailViewModel,
    onBack: () -> Unit,
    onOpenStudentProfile: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showDateDialog by remember { mutableStateOf(false) }
    val exportHandoff = rememberDeputyExportHandoff()

    LaunchedEffect(classId) { viewModel.loadClass(classId) }

    if (showDateDialog) {
        DeputyDateRangeDialog(
            onConfirm = { from, to ->
                viewModel.setCustomRange(from, to)
                showDateDialog = false
            },
            onDismiss = { showDateDialog = false },
        )
    }

    Scaffold(
        containerColor = AtmosphereBrand.Mist,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.classId, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Ink)
                        Text("Анализ класса", fontSize = 12.sp, color = AtmosphereBrand.InkMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = AtmosphereBrand.TealAccent)
                    }
                },
                actions = {
                    val analytics = state.analytics
                    if (analytics != null && !state.loading) {
                        IconButton(
                            onClick = {
                                val labels = state.availableSubjects.associateWith { subjectDisplayName(it) }
                                exportHandoff(
                                    DeputyAnalyticsReportExporter.exportClass(
                                        analytics,
                                        state.heatmapData,
                                        labels,
                                    ),
                                )
                            },
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Экспорт класса", tint = AtmosphereBrand.TealAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AtmosphereBrand.Mist),
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AtmosphereBrand.SkyMid)
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AtmosphereBrand.Paper)
                    .padding(4.dp),
            ) {
                ClassTabChip("Анализ", tab == 0, { tab = 0 }, Modifier.weight(1f))
                ClassTabChip("Таблица", tab == 1, { tab = 1 }, Modifier.weight(1f))
            }
            Spacer(Modifier.padding(6.dp))
            when (tab) {
                0 -> {
                    val analytics = state.analytics
                    if (analytics == null) {
                        Text("Нет данных", color = AtmosphereBrand.InkMuted, modifier = Modifier.padding(24.dp))
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = listBottomPadding(24.dp),
                        ) {
                            item {
                                InsightAnalyticsPanel(
                                    title = "Класс ${analytics.classId}",
                                    subtitle = "${analytics.activeStudentCount} учеников",
                                    totalScore = analytics.totalScore,
                                    periodLabel = analytics.periodLabel,
                                    periodType = state.periodConfig.type,
                                    eventCount = analytics.eventCount,
                                    praiseCount = analytics.praiseCount,
                                    violationCount = analytics.violationCount,
                                    insight = analytics.insight,
                                    dailyScores = analytics.dailyScores,
                                    talkSectionTitle = "О чём говорить на классном часе",
                                    strengthsSectionTitle = "Где коллектив силён",
                                    onPeriodSelect = viewModel::setPeriodType,
                                    onCustomPeriodClick = { showDateDialog = true },
                                    footer = {
                                        ClassStudentsFooter(
                                            praise = analytics.praiseStudents,
                                            watch = analytics.watchStudents,
                                            onStudentClick = onOpenStudentProfile,
                                        )
                                    },
                                )
                            }
                            item { Spacer(Modifier.padding(8.dp)) }
                        }
                    }
                }
                1 -> ClassHeatmapContent(state.heatmapData, state.availableSubjects)
            }
        }
    }
}

@Composable
private fun ClassStudentsFooter(
    praise: List<StudentRankingRow>,
    watch: List<StudentRankingRow>,
    onStudentClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (praise.isNotEmpty()) {
            Text("Кого поощрить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Positive)
            praise.take(5).forEach { row ->
                ClassStudentInsightRow(row, onStudentClick)
            }
        }
        if (watch.isNotEmpty()) {
            Text("Кому уделить внимание", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AtmosphereBrand.Negative)
            watch.take(5).forEach { row ->
                ClassStudentInsightRow(row, onStudentClick)
            }
        }
    }
}

@Composable
private fun ClassStudentInsightRow(row: StudentRankingRow, onClick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick(row.studentId) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.name, fontSize = 13.sp, color = AtmosphereBrand.Ink)
        Text(
            ru.golpom.atmosphere.ui.theme.atmosphereFormatSigned(row.score),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = ru.golpom.atmosphere.ui.theme.atmosphereScoreColor(row.score),
        )
    }
}

@Composable
private fun ClassTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) AtmosphereBrand.SkyMid else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else AtmosphereBrand.InkSoft,
        )
    }
}

@Composable
private fun ClassHeatmapContent(data: List<StudentSubjectCell>, subjects: List<String>) {
    if (data.isEmpty() || subjects.isEmpty()) {
        Text("Нет данных", color = AtmosphereBrand.InkMuted, modifier = Modifier.padding(24.dp))
        return
    }
    val students = data.map { it.studentId to "${it.lastName} ${it.firstName}" }.distinct().sortedBy { it.second }
    LazyColumn(contentPadding = listBottomPadding(24.dp)) {
        item {
            DeputyReportSection(title = "Ученики и предметы") {
                DeputyMatrixHeatmap(
                    rowLabels = students,
                    columnLabels = subjects.map { subjectDisplayName(it) },
                    cellScore = { rowKey, colLabel ->
                        val key = subjects.find { subjectDisplayName(it) == colLabel } ?: colLabel
                        data.find { it.studentId == rowKey && it.subjectKey == key }?.score ?: 0
                    },
                )
            }
        }
    }
}
