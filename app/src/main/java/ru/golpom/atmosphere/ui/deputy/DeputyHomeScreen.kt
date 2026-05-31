/**
 * Экран завуча: бизнес-dashboard с переосмысленной инфографикой (scorecard, bullet, heatmap, brief).
 * UI-слой (Compose); §6.1–6.2 ТЗ.
 */
package ru.golpom.atmosphere.ui.deputy

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import ru.golpom.atmosphere.data.export.deputy.DeputyAnalyticsReportExporter
import ru.golpom.atmosphere.ui.export.rememberDeputyExportHandoff
import ru.golpom.atmosphere.ui.importing.ImportFeedbackEffect
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.AtmosphereDataLoadingBar
import ru.golpom.atmosphere.ui.theme.AtmosphereInitialLoading
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.atmosphereFormatSigned
import ru.golpom.atmosphere.ui.theme.atmosphereHeatmapColor
import ru.golpom.atmosphere.ui.theme.atmosphereHeatmapTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeputyHomeScreen(
    viewModel: DeputyHomeViewModel,
    onNavigateToHome: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenClassDetail: (String) -> Unit = {},
    onOpenStudentProfile: (String) -> Unit = {},
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val previous by viewModel.previousStats.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val periodConfig by viewModel.periodConfig.collectAsStateWithLifecycle()
    val report by viewModel.report.collectAsStateWithLifecycle()
    val navigateTo by viewModel.navigateTo.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedStudentId by viewModel.selectedStudentId.collectAsStateWithLifecycle()
    val studentAnalytics by viewModel.studentAnalytics.collectAsStateWithLifecycle()
    val importBatches by viewModel.importBatches.collectAsStateWithLifecycle()
    val localDataEnabled by viewModel.localDataEnabled.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val importStatusMessage by viewModel.importStatusMessage.collectAsStateWithLifecycle()
    var showDateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val exportHandoff = rememberDeputyExportHandoff { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val periodLabel = remember(periodConfig) { DeputyPeriodRange.label(periodConfig) }

    val dashboard = remember(stats, previous) {
        DeputyDashboardSnapshot(
            stats = stats,
            previous = previous,
        )
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> viewModel.importFromUris(uris) }

    val scrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

    LaunchedEffect(navigateTo) {
        navigateTo?.let {
            onNavigateToHome(it)
            viewModel.onNavigated()
        }
    }
    ImportFeedbackEffect(
        message = report,
        title = "Отчёты загружены",
        onConsumed = { viewModel.consumeReport() },
        onShortMessage = { snackbarHostState.showSnackbar(it) },
    )

    Scaffold(
        modifier = modifier,
        containerColor = AtmosphereBrand.Mist,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {},
                actions = {
                    val iconMod = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.25f))
                    IconButton(onClick = { viewModel.toggleRole() }, modifier = iconMod) {
                        Icon(Icons.Default.Person, contentDescription = "Сменить роль", tint = TextPrimary)
                    }
                    IconButton(
                        onClick = {
                            exportHandoff(DeputyAnalyticsReportExporter.exportSchool(dashboard, periodLabel))
                        },
                        modifier = iconMod,
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Экспорт школы", tint = TextPrimary)
                    }
                    IconButton(
                        onClick = {
                            importLauncher.launch(
                                arrayOf(
                                    "application/vnd.atmosphere.report",
                                    "application/octet-stream",
                                    "application/zip",
                                    "text/csv",
                                    "*/*",
                                ),
                            )
                        },
                        modifier = iconMod,
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = "Загрузить отчёт учителя", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                AtmosphereInitialLoading()
            }
            return@Scaffold
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
            DeputyDashboardHeader(
                topPadding = padding.calculateTopPadding(),
                topEndInset = 132.dp,
                periodConfig = periodConfig,
                onPeriodSelect = { type ->
                    if (type == PeriodType.CUSTOM) showDateDialog = true else viewModel.setPeriodType(type)
                },
            )

            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
            DeputyImportBatchesPanel(
                localDataEnabled = localDataEnabled,
                onLocalDataToggle = viewModel::setLocalDataEnabled,
                batches = importBatches,
                onToggleEnabled = viewModel::setImportBatchEnabled,
                onDelete = viewModel::deleteImportBatch,
            )

            ExecutiveSummaryPanel(summary = dashboard.summary)

            if (dashboard.trend.points.isNotEmpty()) {
                DeputyReportSection(title = "Динамика периода") {
                    TrendAnalyticsPanel(dashboard.trend)
                }
            }

            if (dashboard.classRanking.isNotEmpty()) {
                DeputyReportSection(title = "Рейтинг классов") {
                    ClassBulletRankingTable(
                        rows = dashboard.classRanking,
                        onClassClick = onOpenClassDetail,
                    )
                }
            }

            if (dashboard.parallels.size >= 2) {
                DeputyReportSection(title = "Сравнение параллелей") {
                    ParallelComparisonPanel(dashboard.parallels)
                }
            }

            if (dashboard.heatmap.classIds.isNotEmpty()) {
                DeputyReportSection(title = "По классам и дням") {
                    BusinessHeatmapMatrix(dashboard.heatmap)
                }
            }

            if (dashboard.weekdayPattern.isNotEmpty()) {
                DeputyReportSection(title = "Ритм недели") {
                    WeekdayDonutPanel(dashboard.weekdayPattern)
                }
            }

            if (dashboard.praiseList.isNotEmpty() || dashboard.watchList.isNotEmpty()) {
                DeputyReportSection(title = "Ученики") {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (dashboard.praiseList.isNotEmpty()) {
                            StudentRankingTable(
                                title = "Кого поощрить",
                                rows = dashboard.praiseList,
                                onStudentClick = onOpenStudentProfile,
                            )
                        }
                        if (dashboard.watchList.isNotEmpty()) {
                            StudentRankingTable(
                                title = "Кому уделить внимание",
                                rows = dashboard.watchList,
                                onStudentClick = onOpenStudentProfile,
                            )
                        }
                    }
                }
            }

            if (dashboard.brief.isNotEmpty()) {
                DeputyReportSection(title = "Тезисы для педсовета") {
                    ExecutiveBriefPanel(dashboard.brief)
                }
            }

            DeputyReportSection(title = "Поиск ученика") {
                StudentSearchBlock(
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    selectedStudentId = selectedStudentId,
                    analytics = studentAnalytics,
                    periodType = periodConfig.type,
                    onQueryChange = viewModel::searchStudents,
                    onSelectStudent = viewModel::selectStudent,
                    onClear = viewModel::clearSelectedStudent,
                    onPeriodSelect = viewModel::setPeriodType,
                    onCustomPeriodClick = { showDateDialog = true },
                    onExport = { analytics ->
                        exportHandoff(DeputyAnalyticsReportExporter.exportStudent(analytics))
                    },
                )
            }

            Spacer(Modifier.height(32.dp))
            }
            }

            AtmosphereDataLoadingBar(
                visible = importing,
                message = importStatusMessage.ifBlank { "Загружаем отчёты учителей…" },
                containerColor = AtmosphereBrand.Mist.copy(alpha = 0.88f),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (showDateDialog) {
        DeputyDateRangeDialog(
            onConfirm = { from, to ->
                viewModel.setCustomRange(from, to)
                showDateDialog = false
            },
            onDismiss = { showDateDialog = false },
        )
    }
}

@Composable
fun DeputyDateRangeDialog(
    onConfirm: (Long, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = java.time.LocalDate.now()
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    var currentMonth by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var startDate by remember { mutableStateOf<java.time.LocalDate?>(today.minusDays(7)) }
    var endDate by remember { mutableStateOf<java.time.LocalDate?>(today) }
    val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val months = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AtmosphereBrand.Paper),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Период отчёта", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Text("${months[currentMonth.monthValue - 1]} ${currentMonth.year}", fontWeight = FontWeight.Medium)
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    dayLabels.forEach { Text(it, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
                }
                val firstDay = currentMonth.withDayOfMonth(1)
                val startDow = firstDay.dayOfWeek.value % 7
                val daysInMonth = currentMonth.lengthOfMonth()
                val rows = (startDow + daysInMonth + 6) / 7
                Column {
                    (0 until rows).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            (0 until 7).forEach { col ->
                                val dayNum = row * 7 + col - startDow + 1
                                if (dayNum in 1..daysInMonth) {
                                    val date = currentMonth.withDayOfMonth(dayNum)
                                    val isStart = date == startDate
                                    val isEnd = date == endDate
                                    val inRange = startDate != null && endDate != null &&
                                        date > minOf(startDate!!, endDate!!) && date < maxOf(startDate!!, endDate!!)
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isStart || isEnd -> AtmosphereBrand.SkyMid
                                                    inRange -> AtmosphereBrand.SkyMid.copy(alpha = 0.12f)
                                                    else -> Color.Transparent
                                                },
                                            )
                                            .clickable {
                                                if (startDate == null || endDate != null) {
                                                    startDate = date; endDate = null
                                                } else {
                                                    endDate = if (date.isBefore(startDate)) {
                                                        val t = startDate; startDate = date; t
                                                    } else date
                                                }
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "$dayNum",
                                            color = if (isStart || isEnd) Color.White else AtmosphereBrand.Ink,
                                            fontWeight = if (isStart || isEnd) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("с ${startDate?.let { dateFormat.format(it) } ?: "—"}", modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val s = startDate ?: return@TextButton
                        val e = endDate ?: return@TextButton
                        if (!s.isAfter(e)) {
                            val zone = java.time.ZoneId.systemDefault()
                            onConfirm(
                                s.atStartOfDay(zone).toInstant().toEpochMilli(),
                                e.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli(),
                            )
                        }
                    }) { Text("Применить") }
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                }
            }
        }
    }
}

