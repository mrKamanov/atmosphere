/**
 * Отправка отчёта на устройство завуча по локальной сети.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.transfer

import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.golpom.atmosphere.data.transfer.TransferProtocol.CONNECT_TIMEOUT_MS
import ru.golpom.atmosphere.data.transfer.TransferProtocol.HEADER_FILE_NAME
import ru.golpom.atmosphere.data.transfer.TransferProtocol.HEADER_SENDER_NAME
import ru.golpom.atmosphere.data.transfer.TransferProtocol.HEADER_SENDER_SHORT_ID
import ru.golpom.atmosphere.data.transfer.TransferProtocol.REQUEST_TIMEOUT_MS
import ru.golpom.atmosphere.data.transfer.TransferProtocol.TRANSFER_PATH
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

@Singleton
class LanTransferClient @Inject constructor() {

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = REQUEST_TIMEOUT_MS
        }
    }

    suspend fun send(
        device: LanDevice,
        fileName: String,
        senderName: String,
        senderShortId: String,
        bytes: ByteArray,
    ): TransferSendResult = withContext(Dispatchers.IO) {
        try {
            val url = "http://${device.host}:${device.port}$TRANSFER_PATH"
            val response = client.post(url) {
                header(HEADER_FILE_NAME, TransferProtocol.encodeHeaderValue(fileName))
                header(HEADER_SENDER_NAME, TransferProtocol.encodeHeaderValue(senderName))
                header(HEADER_SENDER_SHORT_ID, TransferProtocol.encodeHeaderValue(senderShortId))
                contentType(ContentType.Application.OctetStream)
                setBody(bytes)
            }
            when (response.status) {
                HttpStatusCode.OK ->
                    TransferSendResult.Delivered(response.bodyAsText().ifBlank { null })
                HttpStatusCode.Forbidden ->
                    TransferSendResult.Rejected
                HttpStatusCode.PayloadTooLarge ->
                    TransferSendResult.Failed("Файл слишком большой")
                else ->
                    TransferSendResult.Failed("Устройство ответило ошибкой ${response.status.value}")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            TransferSendResult.Failed(describe(error))
        }
    }

    private fun describe(error: Exception): String = when (error) {
        is ConnectException -> "Устройство недоступно. Проверьте, включён ли приём у завуча."
        is SocketTimeoutException -> "Устройство не отвечает. Возможно, ждут подтверждения слишком долго."
        else -> error.message?.takeIf { it.isNotBlank() } ?: "Не удалось отправить файл"
    }
}
