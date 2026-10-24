/**
 * Экран урока: сетка учеников (pinch-zoom §5.1), шторка отметок, экспорт CSV, переход в профиль по долгому тапу.
 * UI-слой (Compose), сценарий §5.1 и §4 ТЗ.
 */
package ru.golpom.atmosphere.ui.lesson

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.golpom.atmosphere.data.local.model.LessonStudentRow
import ru.golpom.atmosphere.ui.theme.listBottomPadding
import ru.golpom.atmosphere.ui.theme.SurfaceBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    viewModel: LessonViewModel,
    onBack: () -> Unit,
    onOpenStudentProfile: (studentId: String) -> Unit = {},
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<LessonStudentRow?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingExport by remember { mutableStateOf<LessonExportPayload?>(null) }
    var gridScale by remember { mutableFloatStateOf(1f) }
    val marksSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val payload = pendingExport
        pendingExport = null
        if (uri == null || payload == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.utf8Bytes)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            subjectDisplayName(viewModel.subjectKey),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Text(
                            "Класс ${viewModel.classId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                pendingExport = viewModel.buildExportPayload()
                                exportLauncher.launch(pendingExport!!.fileName)
                            }
                        },
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Сохранить отметки урока", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        gridScale = (gridScale * zoom).coerceIn(0.5f, 3f)
                    }
                },
        ) {
            val loadedRows = rows
            if (loadedRows != null && loadedRows.isEmpty()) {
                EmptyLessonHint(classId = viewModel.classId)
                return@Box
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = gridScale
                        scaleY = gridScale
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    },
                contentPadding = listBottomPadding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(loadedRows.orEmpty(), key = { it.studentId }) { row ->
                    StudentLessonCard(
                        row = row,
                        onClick = { selected = row },
                        onLongClick = { onOpenStudentProfile(row.studentId) },
                    )
                }
            }
        }
    }

    selected?.let { row ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = marksSheetState,
        ) {
            LessonQuickActionsSheet(
                studentLabel = "${row.lastName} ${row.firstName}",
                onPick = { preset ->
                    viewModel.logBehavior(row.studentId, preset)
                    selected = null
                },
                onDismiss = { selected = null },
            )
        }
    }
}

/** Пустой урок: чаще всего класс в расписании записан не так, как класс учеников. */
@Composable
private fun EmptyLessonHint(classId: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "В классе $classId нет учеников",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            "Добавьте учеников в «Мои ученики» или проверьте, что класс в расписании " +
                "называется так же, как класс учеников (например, «9-Л», а не «9Л»).",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
