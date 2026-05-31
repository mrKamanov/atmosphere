/**
 * Диалог передачи файла экспорта (поделиться/сохранить).
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

@Composable
fun ExportHandoffDialog(
    payload: ExportPayload,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    recordsLine: String? = null,
    hintText: String? = null,
    onSavePdf: (() -> Unit)? = null,
    saveLabel: String = "Сохранить",
    savePdfLabel: String = "PDF",
) {
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
                Text(
                    "Отчёт готов",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = TextPrimary,
                )
                val summaryLine = recordsLine ?: if (payload.recordCount == 0) {
                    "За выбранный период отметок нет — файл всё равно можно отправить завучу."
                } else {
                    "В отчёте ${payload.recordCount} отметок · ${payload.periodLabel}."
                }
                Text(summaryLine, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
                Text(
                    hintText ?: (
                        "«Отправить» — передайте файл завучу с этого телефона.\n" +
                            "«Сохранить» — оставьте файл в памяти устройства."
                        ),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp,
                )
                if (payload.appSealed) {
                    Text(
                        "Открывается только в «Атмосфера» на устройстве завуча.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp,
                    )
                }

                if (onSavePdf != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onSave,
                            modifier = Modifier.weight(1f),
                        ) {
                            FormatActionLabel(Icons.Default.Description, "HTML")
                        }
                        OutlinedButton(
                            onClick = onSavePdf,
                            modifier = Modifier.weight(1f),
                        ) {
                            FormatActionLabel(Icons.Default.PictureAsPdf, savePdfLabel)
                        }
                    }
                    FilledTonalButton(
                        onClick = onShare,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Отправить HTML",
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(saveLabel, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Отправить",
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Закрыть",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatActionLabel(
    icon: ImageVector,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = AtmosphereBrand.TealAccent,
        )
        Text(
            label,
            modifier = Modifier.padding(start = 6.dp),
            fontWeight = FontWeight.Medium,
        )
    }
}
