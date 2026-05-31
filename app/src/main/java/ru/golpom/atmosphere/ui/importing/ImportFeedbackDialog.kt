/**
 * Диалог результата загрузки файла (расписание, ученики, отчёты).
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.importing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

@Composable
fun ImportFeedbackDialog(
    message: String,
    onDismiss: () -> Unit,
    title: String = "Результат загрузки",
) {
    val lines = message.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val hasErrors = lines.any { it.looksLikeImportError() }
    val summary = lines.firstOrNull()?.takeIf { lines.size == 1 }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasErrors) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (hasErrors) AtmosphereBrand.Negative else AtmosphereBrand.PositiveDeep,
                    )
                    Text(
                        title,
                        modifier = Modifier.padding(start = 10.dp),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = TextPrimary,
                    )
                }

                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (summary != null) {
                        Text(summary, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
                    } else {
                        lines.forEach { line ->
                            when {
                                line.endsWith(':') && line.length <= 32 ->
                                    Text(
                                        line,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                else -> ImportFeedbackLine(line)
                            }
                        }
                    }
                }

                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Понятно", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ImportFeedbackLine(text: String) {
    val isError = text.looksLikeImportError()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "•",
            fontSize = 14.sp,
            color = if (isError) AtmosphereBrand.Negative else AtmosphereBrand.TealAccent,
        )
        Text(
            text,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = if (isError) AtmosphereBrand.Negative else TextPrimary,
            lineHeight = 18.sp,
        )
    }
}

private fun String.looksLikeImportError(): Boolean {
    val lower = lowercase()
    return lower.startsWith("ошибка") ||
        lower.contains("не удалось") ||
        lower.contains("не содержит") ||
        lower.contains(": не ")
}

/**
 * Показывает [ImportFeedbackDialog] вместо snackbar для длинных или многострочных сообщений импорта.
 */
@Composable
fun ImportFeedbackEffect(
    message: String?,
    onConsumed: () -> Unit,
    title: String = "Результат загрузки",
    onShortMessage: suspend (String) -> Unit = {},
) {
    if (message == null) return

    if (message.shouldUseImportDialog()) {
        ImportFeedbackDialog(
            message = message,
            title = title,
            onDismiss = onConsumed,
        )
    } else {
        androidx.compose.runtime.LaunchedEffect(message) {
            onShortMessage(message)
            onConsumed()
        }
    }
}

fun String.shouldUseImportDialog(): Boolean {
    val lower = lowercase()
    return contains('\n') ||
        length > 72 ||
        looksLikeImportError() ||
        lower.startsWith("загружено") ||
        lower.contains("отметок") ||
        lower.contains("уроков") ||
        lower.contains("учеников")
}
