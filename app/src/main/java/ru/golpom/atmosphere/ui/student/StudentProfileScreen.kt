/**
 * Профиль ученика: история отметок.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import ru.golpom.atmosphere.data.local.entity.BehaviorLogEntity
import ru.golpom.atmosphere.domain.behaviorTypeLabelRu
import ru.golpom.atmosphere.ui.export.ExportPeriodDialog
import ru.golpom.atmosphere.ui.export.rememberTeacherExportHandoff
import ru.golpom.atmosphere.ui.lesson.subjectDisplayName
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.ChipBg
import ru.golpom.atmosphere.ui.theme.SurfaceBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())

private val GreenPositive = Color(0xFF16A34A)
private val RedNegative = Color(0xFFDC2626)
private val OrangeWarning = Color(0xFFEA580C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    viewModel: StudentProfileViewModel,
    onBack: () -> Unit,
) {
    val student by viewModel.student.collectAsStateWithLifecycle()
    val logs by viewModel.behaviorLogs.collectAsStateWithLifecycle()
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

    val subjectScores = remember(logs) {
        logs.groupBy { it.subjectKey }.map { (subj, entries) ->
            subj to entries.sumOf { it.scoreImpact }
        }.sortedByDescending { it.second }
    }

    val plusCount = remember(logs) { logs.count { it.scoreImpact > 0 } }
    val minusCount = remember(logs) { logs.count { it.scoreImpact < 0 } }
    val topViolations = remember(logs) {
        logs.filter { it.scoreImpact < 0 }
            .groupingBy { it.behaviorType }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        student?.let { "${it.lastName} ${it.firstName}" } ?: "Ученик",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Выгрузить", tint = TextSecondary)
                    }
                    IconButton(onClick = { viewModel.clearAllLogs() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить всё", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Default.Add, contentDescription = null, Modifier.size(22.dp), tint = CardBg) },
                        iconBg = GreenPositive,
                        label = "Хорошо",
                        value = "$plusCount",
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Default.Close, contentDescription = null, Modifier.size(22.dp), tint = CardBg) },
                        iconBg = RedNegative,
                        label = "Нарушения",
                        value = "$minusCount",
                    )
                }
            }

            if (topViolations.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Частые нарушения", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            topViolations.forEach { (type, count) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(8.dp).clip(CircleShape).background(OrangeWarning),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(behaviorTypeLabelRu(type), fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                                    Text("$count", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OrangeWarning)
                                }
                            }
                        }
                    }
                }
            }

            if (subjectScores.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("По предметам", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Spacer(Modifier.height(8.dp))
                            val maxAbs = maxOf(subjectScores.maxOfOrNull { abs(it.second) } ?: 1, 1)
                            subjectScores.forEach { (subj, score) ->
                                val ratio = abs(score).toFloat() / maxAbs
                                val isPos = score >= 0
                                val barColor = if (isPos) Color(0xFF059669).copy(alpha = 0.2f) else Color(0xFFDC2626).copy(alpha = 0.2f)
                                val fillColor = if (isPos) Color(0xFF059669) else Color(0xFFDC2626)
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(subjectDisplayName(subj), fontSize = 13.sp, color = TextPrimary, modifier = Modifier.width(110.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.width(8.dp))
                                    Box(Modifier.weight(1f).height(18.dp)) {
                                        Box(Modifier.fillMaxWidth(ratio.coerceIn(0.02f, 1f)).height(18.dp).clip(RoundedCornerShape(6.dp)).background(barColor))
                                        Box(Modifier.fillMaxWidth(ratio.coerceIn(0.02f, 1f)).height(18.dp).clip(RoundedCornerShape(6.dp)).background(fillColor.copy(alpha = 0.6f)))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(formatSigned(score), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fillColor, modifier = Modifier.width(36.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Лента событий", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }

            items(logs, key = { it.id }) { entry ->
                LogEntryCard(entry = entry, onDelete = { viewModel.deleteLog(entry.id) })
            }
        }
    }

    if (showExportDialog) {
        ExportPeriodDialog(
            title = "Выгрузка ученика",
            securityDefaults = exportSecurityDefaults,
            onConfirm = { period, options ->
                showExportDialog = false
                viewModel.rememberExportSecurityChoices(options)
                scope.launch {
                    showHandoff(viewModel.exportStudent(period, options))
                }
            },
            onDismiss = { showExportDialog = false },
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    iconBg: Color,
    label: String,
    value: String,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(label, fontSize = 13.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun LogEntryCard(entry: BehaviorLogEntity, onDelete: () -> Unit) {
    val whenText = remember(entry.timestamp) {
        timeFormatter.format(Instant.ofEpochMilli(entry.timestamp))
    }
    val isPositive = entry.scoreImpact > 0
    val accentColor = if (isPositive) GreenPositive else RedNegative
    val sign = if (isPositive) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(start = 0.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(accentColor),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(whenText, fontSize = 12.sp, color = TextSecondary)
                Text(
                    "${subjectDisplayName(entry.subjectKey)} · ${behaviorTypeLabelRu(entry.behaviorType)}",
                    fontSize = 14.sp,
                    color = TextPrimary,
                )
                Text(
                    "Влияние: $sign${entry.scoreImpact}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentColor,
                )
                if (entry.comment.isNotBlank()) {
                    Text(entry.comment, fontSize = 13.sp, color = TextSecondary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(20.dp), tint = TextSecondary.copy(alpha = 0.6f))
            }
        }
    }
}

private fun formatSigned(v: Int): String = if (v >= 0) "+$v" else "$v"
