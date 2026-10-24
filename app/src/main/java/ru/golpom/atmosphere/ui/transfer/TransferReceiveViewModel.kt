/**
 * Состояние экрана приёма отчётов у завуча: тумблер приёма, порт, входящие запросы.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.transfer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.golpom.atmosphere.data.preferences.UserPreferencesRepository
import ru.golpom.atmosphere.data.transfer.LanDiscoveryService
import ru.golpom.atmosphere.data.transfer.LanTransferServer
import ru.golpom.atmosphere.data.transfer.TransferPendingRequest
import ru.golpom.atmosphere.data.transfer.TransferProtocol

@HiltViewModel
class TransferReceiveViewModel @Inject constructor(
    private val discovery: LanDiscoveryService,
    private val server: LanTransferServer,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _accepting = MutableStateFlow(false)
    val accepting: StateFlow<Boolean> = _accepting.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _port = MutableStateFlow(0)
    val port: StateFlow<Int> = _port.asStateFlow()

    private val _deviceName = MutableStateFlow("")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    /**
     * Очередь входящих отчётов: на педсовете несколько учителей могут отправить одновременно.
     * Раньше хранился один запрос, и новый затирал предыдущий — тот отклонялся по таймауту.
     */
    private val _queue = MutableStateFlow<List<TransferPendingRequest>>(emptyList())

    /** Запрос, который сейчас показан завучу (первый в очереди). */
    val pending: StateFlow<TransferPendingRequest?> = _queue
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Сколько отчётов ждёт после текущего. */
    val queuedAfterCurrent: StateFlow<Int> = _queue
        .map { (it.size - 1).coerceAtLeast(0) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        _accepting.value = server.isActive
        if (server.isActive) {
            _port.value = server.boundPort.value
            viewModelScope.launch { _deviceName.value = resolveDeviceName() }
        }

        viewModelScope.launch {
            server.pending.collect { request ->
                _queue.update { it + request }
                launch {
                    // Сервер сам отклонит запрос по таймауту — убираем его и с экрана.
                    delay(TransferProtocol.APPROVAL_TIMEOUT_MS)
                    removeFromQueue(request)
                }
            }
        }
    }

    fun setAccepting(enabled: Boolean) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                if (enabled) startAccepting() else stopAccepting()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Log.e(TAG, "setAccepting($enabled) failed", error)
                _messages.emit(
                    "Ошибка приёма: ${error.javaClass.simpleName} ${error.message.orEmpty()}".trim(),
                )
            } finally {
                _busy.value = false
            }
        }
    }

    fun approve(request: TransferPendingRequest) {
        if (!removeFromQueue(request)) return
        server.respond(request.requestId, accept = true)
        viewModelScope.launch {
            _messages.emit("Отчёт принят: ${request.senderLabel}")
        }
    }

    fun reject(request: TransferPendingRequest) {
        if (!removeFromQueue(request)) return
        server.respond(request.requestId, accept = false)
    }

    override fun onCleared() {
        discovery.stopAdvertising()
        server.stopAsync()
    }

    private suspend fun startAccepting() {
        _messages.emit("Включаю приём...")
        val bound = server.start()
        if (bound <= 0) {
            _messages.emit("Не удалось включить приём. Проверьте, что телефон в Wi-Fi.")
            return
        }
        val name = resolveDeviceName()
        _deviceName.value = name
        Log.i(TAG, "startAccepting: advertising as '$name' port=$bound")
        discovery.startAdvertising(
            displayName = name,
            shortId = preferences.getOrCreateTeacherProfileShortId(),
            port = bound,
            isDeputy = true,
        )
        _port.value = bound
        _accepting.value = true
        _messages.emit("Приём включён. Порт $bound")
    }

    /** @return true, если запрос ещё был в очереди (не обработан и не истёк). */
    private fun removeFromQueue(request: TransferPendingRequest): Boolean {
        var removed = false
        _queue.update { list ->
            removed = request in list
            list - request
        }
        return removed
    }

    private suspend fun stopAccepting() {
        _queue.value = emptyList()
        _accepting.value = false
        _port.value = 0
        discovery.stopAdvertising()
        server.stop()
        _messages.emit("Приём выключен")
    }

    private suspend fun resolveDeviceName(): String {
        val userName = preferences.userName.first()
        if (userName.isNotBlank()) return userName
        val lastName = preferences.teacherLastName.first()
        return lastName.ifBlank { "Устройство завуча" }
    }

    private companion object {
        const val TAG = "AtmosphereXfer"
    }
}
