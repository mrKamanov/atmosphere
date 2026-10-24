/**
 * Состояние диалога отправки отчёта завучу по локальной сети.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.golpom.atmosphere.data.preferences.UserPreferencesRepository
import ru.golpom.atmosphere.data.transfer.LanDevice
import ru.golpom.atmosphere.data.transfer.LanDiscoveryService
import ru.golpom.atmosphere.data.transfer.LanTransferClient
import ru.golpom.atmosphere.data.transfer.TransferSendResult
import ru.golpom.atmosphere.ui.export.ExportPayload

data class TransferAlert(
    val message: String,
    val closeDialog: Boolean,
)

@HiltViewModel
class TransferSendViewModel @Inject constructor(
    private val discovery: LanDiscoveryService,
    private val client: LanTransferClient,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _devices = MutableStateFlow<List<LanDevice>>(emptyList())
    val devices: StateFlow<List<LanDevice>> = _devices.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _sendingOn = MutableStateFlow<String?>(null)
    val sendingOn: StateFlow<String?> = _sendingOn.asStateFlow()

    private val _alerts = MutableSharedFlow<TransferAlert>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val alerts: SharedFlow<TransferAlert> = _alerts.asSharedFlow()

    init {
        viewModelScope.launch {
            discovery.discoverDeputies().collect {
                android.util.Log.i("AtmosphereXfer", "send: deputies=${it.map { d -> d.displayName + "@" + d.host + ":" + d.port }}")
                _devices.value = it
            }
        }
    }

    fun startSearch() {
        android.util.Log.i("AtmosphereXfer", "send: startSearch")
        _searching.value = true
        discovery.startDiscovery()
    }

    fun stopSearch() {
        android.util.Log.i("AtmosphereXfer", "send: stopSearch")
        discovery.stopDiscovery()
        _searching.value = false
    }

    fun send(device: LanDevice, payload: ExportPayload) {
        if (_sendingOn.value != null) return
        viewModelScope.launch {
            _sendingOn.value = device.serviceId
            val senderName = resolveSenderName()
            val result = client.send(
                device = device,
                fileName = payload.fileName,
                senderName = senderName,
                senderShortId = preferences.getOrCreateTeacherProfileShortId(),
                bytes = payload.utf8Bytes,
            )
            _sendingOn.value = null
            when (result) {
                is TransferSendResult.Delivered -> _alerts.emit(
                    TransferAlert(
                        message = result.message ?: "Отчёт отправлен завучу.",
                        closeDialog = true,
                    ),
                )

                TransferSendResult.Rejected -> _alerts.emit(
                    TransferAlert("Завуч отклонил приём отчёта.", closeDialog = false),
                )

                is TransferSendResult.Failed -> _alerts.emit(
                    TransferAlert(result.reason, closeDialog = false),
                )
            }
        }
    }

    private suspend fun resolveSenderName(): String {
        val userName = preferences.userName.first()
        if (userName.isNotBlank()) return userName
        return preferences.teacherLastName.first()
    }
}
