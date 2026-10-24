/**
 * Диалог учителя: выбрать устройство завуча в сети и отправить отчёт.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.transfer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.golpom.atmosphere.data.transfer.LanDevice
import ru.golpom.atmosphere.ui.export.ExportPayload
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

@Composable
fun TransferSendDialog(
    payload: ExportPayload,
    onSnack: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: TransferSendViewModel = hiltViewModel(),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    val sendingOn by viewModel.sendingOn.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.startSearch() }
    DisposableEffect(Unit) {
        onDispose { viewModel.stopSearch() }
    }
    LaunchedEffect(Unit) {
        viewModel.alerts.collect { alert ->
            onSnack(alert.message)
            if (alert.closeDialog) onDismiss()
        }
    }

    val sending = sendingOn != null

    Dialog(
        onDismissRequest = { if (!sending) onDismiss() },
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Отправить завучу",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = TextPrimary,
                )
                Text(
                    "Файл: ${payload.fileName} · ${formatTransferSize(payload.utf8Bytes.size.toLong())}",
                    fontSize = 13.sp,
                    color = TextSecondary,
                )

                if (devices.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (searching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = AtmosphereBrand.SkyMid,
                            )
                        }
                        Text(
                            "Ищем устройство завуча в этой сети…",
                            fontSize = 13.sp,
                            color = TextSecondary,
                        )
                    }
                    Text(
                        "Убедитесь, что завуч включил приём отчётов и телефон подключён " +
                            "к той же сети Wi-Fi.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        devices.forEach { device ->
                            DeviceRow(
                                device = device,
                                enabled = !sending,
                                busy = sendingOn == device.serviceId,
                                onClick = { viewModel.send(device, payload) },
                            )
                        }
                    }
                }

                Text(
                    "Файл передаётся напрямую по школьному Wi-Fi. Интернет и серверы не используются.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp,
                )

                TextButton(
                    onClick = { if (!sending) onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Отмена",
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
private fun DeviceRow(
    device: LanDevice,
    enabled: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = AtmosphereBrand.Mist),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    device.displayName.ifBlank { device.shortId },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                )
                Text(
                    device.shortId,
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = AtmosphereBrand.SkyMid,
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    modifier = Modifier.size(18.dp),
                    tint = AtmosphereBrand.TealAccent,
                )
            }
        }
    }
}
