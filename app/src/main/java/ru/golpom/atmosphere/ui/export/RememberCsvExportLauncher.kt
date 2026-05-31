/**
 * SAF CreateDocument + запись CSV на диск.
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

@Composable
fun rememberCsvExportLauncher(
    onResult: (String?) -> Unit = {},
): (ExportPayload) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<ExportPayload?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        val payload = pending
        pending = null
        if (uri == null || payload == null) {
            onResult(null)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val message = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.utf8Bytes)
                }
                if (payload.recordCount == 0) {
                    "Файл сохранён. За выбранный период записей нет."
                } else {
                    "Выгружено ${payload.recordCount} записей (${payload.periodLabel})."
                }
            }.getOrElse { "Не удалось сохранить файл." }
            onResult(message)
        }
    }

    return { payload ->
        pending = payload
        launcher.launch(payload.fileName)
    }
}

suspend fun writeExportPayload(context: Context, payload: ExportPayload, uri: android.net.Uri) {
    context.contentResolver.openOutputStream(uri)?.use { out ->
        out.write(payload.utf8Bytes)
    }
}
