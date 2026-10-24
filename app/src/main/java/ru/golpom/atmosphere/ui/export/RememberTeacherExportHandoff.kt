/**
 * Compose-хук: лаунчеры экспорта учителя.
 * UI-слой.
 */
package ru.golpom.atmosphere.ui.export

import android.content.Context
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
import ru.golpom.atmosphere.data.export.TeacherReportFile
import ru.golpom.atmosphere.ui.transfer.TransferSendDialog

/**
 * После формирования [ExportPayload] — диалог «Поделиться» / «Сохранить» (SAF).
 */
@Composable
fun rememberTeacherExportHandoff(
    onSnack: (String) -> Unit = {},
): (ExportPayload) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var handoffPayload by remember { mutableStateOf<ExportPayload?>(null) }
    var lanPayload by remember { mutableStateOf<ExportPayload?>(null) }
    var pendingSave by remember { mutableStateOf<ExportPayload?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(TeacherReportFile.MIME_TYPE),
    ) { uri ->
        val payload = pendingSave
        pendingSave = null
        if (uri == null || payload == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = runCatching {
                writeExportPayload(context, payload, uri)
                if (payload.recordCount == 0) {
                    "Файл сохранён. За выбранный период отметок нет."
                } else {
                    "Сохранено: ${payload.recordCount} отметок."
                }
            }.getOrElse { "Не удалось сохранить файл. Попробуйте ещё раз." }
            onSnack(message)
        }
    }

    handoffPayload?.let { payload ->
        val sendLan: (() -> Unit)? = if (payload.appSealed) {
            {
                lanPayload = payload
                handoffPayload = null
            }
        } else {
            null
        }
        ExportHandoffDialog(
            payload = payload,
            onShare = {
                ExportShareHelper.share(context, payload)
                handoffPayload = null
            },
            onSave = {
                pendingSave = payload
                handoffPayload = null
                saveLauncher.launch(payload.fileName)
            },
            onDismiss = { handoffPayload = null },
            onSendLan = sendLan,
        )
    }

    lanPayload?.let { payload ->
        TransferSendDialog(
            payload = payload,
            onSnack = onSnack,
            onDismiss = { lanPayload = null },
        )
    }

    return { payload -> handoffPayload = payload }
}
