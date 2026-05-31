/**
 * Тексты и intent для шаринга экспорта.
 * UI-слой.
 */
package ru.golpom.atmosphere.ui.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import ru.golpom.atmosphere.data.export.TeacherReportFile
import ru.golpom.atmosphere.data.export.deputy.DeputyAnalyticsHtml

object ExportShareHelper {

    fun share(context: Context, payload: ExportPayload) {
        val dir = File(context.cacheDir, "export_share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, payload.fileName)
        file.writeBytes(payload.utf8Bytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.export", file)
        val chooserText = buildString {
            append("Отчёт из приложения «Атмосфера». ")
            if (payload.appSealed) {
                append("Откройте его в «Атмосфере» на телефоне завуча (кнопка загрузки вверху главного экрана завуча).")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = payload.mimeType.ifBlank { TeacherReportFile.MIME_TYPE }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, payload.fileName)
            putExtra(Intent.EXTRA_TEXT, chooserText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Куда отправить отчёт?"))
    }

    fun shareDeputy(context: Context, payload: ExportPayload) {
        val dir = File(context.cacheDir, "export_share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, payload.fileName)
        file.writeBytes(payload.utf8Bytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.export", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = payload.mimeType.ifBlank { DeputyAnalyticsHtml.MIME_TYPE }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, payload.fileName)
            putExtra(Intent.EXTRA_TEXT, "Отчёт аналитики из приложения «Атмосфера».")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Куда отправить отчёт?"))
    }
}
