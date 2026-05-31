/**
 * Compose-хук: передача HTML-отчёта завуча (поделиться / сохранить HTML / PDF).
 * UI-слой.
 */
package ru.golpom.atmosphere.ui.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import ru.golpom.atmosphere.data.export.deputy.DeputyAnalyticsHtml
import ru.golpom.atmosphere.data.export.deputy.HtmlReportPdfExporter

@Composable
fun rememberDeputyExportHandoff(
    onSnack: (String) -> Unit = {},
): (ExportPayload) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var handoffPayload by remember { mutableStateOf<ExportPayload?>(null) }
    var pendingHtmlSave by remember { mutableStateOf<ExportPayload?>(null) }
    var pendingPdfSave by remember { mutableStateOf<ExportPayload?>(null) }

    val saveHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(DeputyAnalyticsHtml.MIME_TYPE),
    ) { uri ->
        val payload = pendingHtmlSave
        pendingHtmlSave = null
        if (uri == null || payload == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = runCatching {
                writeExportPayload(context, payload, uri)
                "HTML сохранён · ${payload.periodLabel}."
            }.getOrElse { "Не удалось сохранить HTML." }
            onSnack(message)
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(HtmlReportPdfExporter.MIME_TYPE),
    ) { uri ->
        val payload = pendingPdfSave
        pendingPdfSave = null
        if (uri == null || payload == null) return@rememberLauncherForActivityResult
        scope.launch {
            onSnack("Формируем PDF…")
            val message = runCatching {
                val html = (payload.pdfUtf8Bytes ?: payload.utf8Bytes).decodeToString()
                val pdf = HtmlReportPdfExporter.render(context, html)
                context.contentResolver.openOutputStream(uri)?.use { it.write(pdf) }
                    ?: error("Нет доступа к файлу")
                "PDF сохранён · ${payload.periodLabel}."
            }.getOrElse {
                it.message?.takeIf { msg -> msg.isNotBlank() } ?: "Не удалось сохранить PDF."
            }
            onSnack(message)
        }
    }

    handoffPayload?.let { payload ->
        ExportHandoffDialog(
            payload = payload,
            recordsLine = if (payload.recordCount == 0) {
                "За выбранный период отметок нет — файл всё равно можно отправить."
            } else {
                "В отчёте ${payload.recordCount} отметок · ${payload.periodLabel}."
            },
            hintText = "«HTML» или «PDF» — сохранить на устройстве.\n" +
                "«Отправить» — передать HTML-файл с этого телефона.",
            onShare = {
                ExportShareHelper.shareDeputy(context, payload)
                handoffPayload = null
            },
            onSave = {
                pendingHtmlSave = payload
                handoffPayload = null
                saveHtmlLauncher.launch(payload.fileName)
            },
            onSavePdf = {
                pendingPdfSave = payload
                handoffPayload = null
                savePdfLauncher.launch(HtmlReportPdfExporter.pdfFileName(payload.fileName))
            },
            savePdfLabel = "PDF",
            onDismiss = { handoffPayload = null },
        )
    }

    return { payload -> handoffPayload = payload }
}
