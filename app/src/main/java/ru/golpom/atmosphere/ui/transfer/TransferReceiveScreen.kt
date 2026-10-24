/**
 * Экран завуча «Приём отчётов»: тумблер приёма и диалог подтверждения входящего файла.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.golpom.atmosphere.data.transfer.TransferPendingRequest
import ru.golpom.atmosphere.ui.theme.AtmosphereBrand
import ru.golpom.atmosphere.ui.theme.CardBg
import ru.golpom.atmosphere.ui.theme.TextPrimary
import ru.golpom.atmosphere.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferReceiveScreen(
    viewModel: TransferReceiveViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accepting by viewModel.accepting.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val port by viewModel.port.collectAsStateWithLifecycle()
    val deviceName by viewModel.deviceName.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val queuedAfterCurrent by viewModel.queuedAfterCurrent.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = AtmosphereBrand.Mist,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Приём отчётов",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = TextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "Принимать отчёты",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = TextPrimary,
                        )
                        Text(
                            if (accepting) {
                                "Телефон виден учителям в этой сети"
                            } else {
                                "Приём выключен"
                            },
                            fontSize = 13.sp,
                            color = TextSecondary,
                        )
                    }
                    Switch(
                        checked = accepting,
                        enabled = !busy,
                        onCheckedChange = viewModel::setAccepting,
                    )
                }
            }

            if (accepting) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Как это работает",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextPrimary,
                        )
                        StepLine(1, "Учитель выгружает отчёт и выбирает «Отправить завучу».")
                        StepLine(2, "Отчёт приходит сюда — вы видите имя и подтверждаете приём.")
                        StepLine(3, "Файл идёт напрямую по школьному Wi-Fi и никуда не выгружается.")
                    }
                }

                Text(
                    buildString {
                        append("Имя в сети: ")
                        append(deviceName.ifBlank { "—" })
                        if (port > 0) append(" · порт $port")
                    },
                    fontSize = 12.sp,
                    color = AtmosphereBrand.InkMuted,
                )
            } else {
                Text(
                    "Включите приём перед совещанием и убедитесь, что телефон подключён " +
                        "к той же сети Wi-Fi, что и телефоны учителей.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp,
                )
            }
        }
    }

    pending?.let { request ->
        TransferApprovalDialog(
            request = request,
            queuedAfter = queuedAfterCurrent,
            onApprove = { viewModel.approve(request) },
            onReject = { viewModel.reject(request) },
        )
    }
}

@Composable
private fun StepLine(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "$number.",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AtmosphereBrand.Ink,
        )
        Text(text, fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp)
    }
}

@Composable
private fun TransferApprovalDialog(
    request: TransferPendingRequest,
    queuedAfter: Int,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    AlertDialog(
        // Случайный тап мимо окна не должен отклонять отчёт — только явные кнопки.
        onDismissRequest = {},
        title = {
            Text(
                "Отчёт от учителя",
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    request.senderLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                )
                InfoLine("Устройство в сети", request.senderHost)
                InfoLine("Файл", request.fileName)
                InfoLine("Размер", formatTransferSize(request.sizeBytes))
                Text(
                    "Добавить отметки из этого отчёта в аналитику?",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (queuedAfter > 0) {
                    Text(
                        "В очереди ещё: $queuedAfter",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onApprove) { Text("Принять") }
        },
        dismissButton = {
            TextButton(onClick = onReject) { Text("Отклонить", color = TextSecondary) }
        },
        containerColor = CardBg,
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Text(
        "$label: $value",
        fontSize = 13.sp,
        color = TextSecondary,
        lineHeight = 18.sp,
    )
}

internal fun formatTransferSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes Б"
    bytes < 1024L * 1024 -> "${bytes / 1024} КБ"
    else -> String.format(java.util.Locale.US, "%.1f МБ", bytes / 1024.0 / 1024.0)
}
