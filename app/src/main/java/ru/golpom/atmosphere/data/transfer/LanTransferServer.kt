/**
 * Локальный HTTP-сервер приёма отчётов. Поднимается только на устройстве завуча,
 * на время, пока включён приём. Каждый входящий файл ждёт подтверждения на экране.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.transfer

import android.util.Log
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.golpom.atmosphere.data.export.DeputyReportImporter
import ru.golpom.atmosphere.data.transfer.TransferProtocol.HEADER_FILE_NAME
import ru.golpom.atmosphere.data.transfer.TransferProtocol.HEADER_SENDER_NAME
import ru.golpom.atmosphere.data.transfer.TransferProtocol.HEADER_SENDER_SHORT_ID
import ru.golpom.atmosphere.data.transfer.TransferProtocol.MAX_BYTES
import ru.golpom.atmosphere.data.transfer.TransferProtocol.TRANSFER_PATH
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

@Singleton
class LanTransferServer @Inject constructor(
    private val importer: DeputyReportImporter,
) {
    private val _pending = MutableSharedFlow<TransferPendingRequest>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val pending: SharedFlow<TransferPendingRequest> = _pending.asSharedFlow()

    private val _boundPort = MutableStateFlow(0)
    val boundPort: StateFlow<Int> = _boundPort.asStateFlow()

    private val waiters = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private val mutex = Mutex()
    private val teardownScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: EmbeddedServer<*, *>? = null

    val isActive: Boolean get() = server != null

    /**
     * Остановка без ожидания: нужна при уходе с экрана, когда вызывающий
     * корутинный скоуп уже завершается.
     */
    fun stopAsync() {
        teardownScope.launch { stop() }
    }

    suspend fun start(): Int = mutex.withLock {
        server?.let { return _boundPort.value }
        try {
            val started = createAndStartEngine()
            val port = withTimeoutOrNull(RESOLVE_TIMEOUT_MS) {
                started.engine.resolvedConnectors().firstOrNull()?.port ?: 0
            } ?: 0
            if (port <= 0) {
                withContext(Dispatchers.IO) { runCatching { started.stop(0, 1_000) } }
                _boundPort.value = 0
                return@withLock 0
            }
            server = started
            _boundPort.value = port
            port
        } catch (error: Throwable) {
            Log.e(TAG, "start: failed", error)
            throw error
        }
    }

    /**
     * Ktor CIO берёт `parentCoroutineContext` приложения как родительский job для
     * внутреннего LAZY-джоба сервера. Если запускать движок внутри чужого
     * `withContext`, родителем становится вложенная корутина, и внутренний джоб
     * не стартует: selector-поток не создаётся, `resolvedConnectors()` не завершается.
     * Поэтому движок создаётся и стартует на отдельном потоке вне вызывающего
     * корутинного скоупа, чтобы `parentCoroutineContext` оставался пустым.
     */
    private suspend fun createAndStartEngine(): EmbeddedServer<*, *> {
        val result = CompletableDeferred<Result<EmbeddedServer<*, *>>>()
        Thread({
            val outcome = runCatching {
                val embedded = embeddedServer(CIO, port = 0, host = HOST) {
                    routing {
                        get(TransferProtocol.HEALTH_PATH) {
                            call.respondText("atmo", ContentType.Text.Plain)
                        }
                        post(TRANSFER_PATH) { handleTransfer(call) }
                    }
                }
                embedded.start(wait = false)
                embedded
            }
            result.complete(outcome)
        }, "atmo-engine-start").start()
        return result.await().getOrElse {
            Log.e(TAG, "start: failed", it)
            throw it
        }
    }

    suspend fun stop() = mutex.withLock {
        waiters.values.forEach { it.complete(false) }
        waiters.clear()
        val current = server ?: return@withLock
        server = null
        _boundPort.value = 0
        withContext(Dispatchers.IO) { runCatching { current.stop(0, 1_000) } }
    }

    fun respond(requestId: String, accept: Boolean) {
        waiters.remove(requestId)?.complete(accept)
    }

    private suspend fun handleTransfer(call: ApplicationCall) {
        val contentLength = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
        if (contentLength > MAX_BYTES) {
            call.respond(HttpStatusCode.PayloadTooLarge, "Файл слишком большой")
            return
        }

        val fileName = TransferProtocol.decodeHeaderValue(
            call.request.headers[HEADER_FILE_NAME],
        ).ifBlank { "отчёт.atmo" }
        val senderName = TransferProtocol.decodeHeaderValue(call.request.headers[HEADER_SENDER_NAME])
        val senderShortId = TransferProtocol.decodeHeaderValue(call.request.headers[HEADER_SENDER_SHORT_ID])
        val senderHost = call.request.local.remoteHost

        val body = try {
            call.receive<ByteArray>()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            call.respond(HttpStatusCode.BadRequest, "Не удалось прочитать файл")
            return
        }

        if (body.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "Пустой файл")
            return
        }
        if (body.size > MAX_BYTES) {
            call.respond(HttpStatusCode.PayloadTooLarge, "Файл слишком большой")
            return
        }

        val requestId = UUID.randomUUID().toString()
        val decision = CompletableDeferred<Boolean>()
        waiters[requestId] = decision

        _pending.emit(
            TransferPendingRequest(
                requestId = requestId,
                senderName = senderName,
                senderShortId = senderShortId,
                senderHost = senderHost,
                fileName = fileName,
                sizeBytes = body.size.toLong(),
            ),
        )

        val accepted = withTimeoutOrNull(TransferProtocol.APPROVAL_TIMEOUT_MS) {
            decision.await()
        } ?: false
        waiters.remove(requestId)

        if (!accepted) {
            call.respond(HttpStatusCode.Forbidden, "Отклонено на устройстве завуча")
            return
        }

        val result = runCatching { importer.importFile(fileName, body) }
            .getOrElse {
                call.respond(HttpStatusCode.InternalServerError, "Ошибка при сохранении отчёта")
                return
            }
        if (result.batchId == null) {
            call.respond(
                HttpStatusCode.InternalServerError,
                result.errorMessage ?: "Не удалось прочитать отчёт",
            )
            return
        }
        call.respond(
            HttpStatusCode.OK,
            result.label.ifBlank { "Отчёт принят" },
        )
    }

    private companion object {
        const val TAG = "AtmosphereXfer"
        const val HOST = "0.0.0.0"
        const val RESOLVE_TIMEOUT_MS = 5_000L
    }
}
