/**
 * Модели и константы протокола локального обмена.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.transfer

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object TransferProtocol {
    const val SERVICE_TYPE = "_atmo._tcp."
    const val TRANSFER_PATH = "/transfer"
    const val HEALTH_PATH = "/health"

    const val HEADER_FILE_NAME = "X-Atmo-File-Name"
    const val HEADER_SENDER_NAME = "X-Atmo-Sender-Name"
    const val HEADER_SENDER_SHORT_ID = "X-Atmo-Sender-Id"

    const val ATTR_NAME = "name"
    const val ATTR_SHORT_ID = "sid"
    const val ATTR_ROLE = "role"

    const val ROLE_DEPUTY = "deputy"
    const val ROLE_TEACHER = "teacher"

    const val MAX_BYTES = 20L * 1024 * 1024
    const val APPROVAL_TIMEOUT_MS = 90_000L
    const val REQUEST_TIMEOUT_MS = 180_000L
    const val CONNECT_TIMEOUT_MS = 5_000L

    fun encodeHeaderValue(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    fun decodeHeaderValue(value: String?): String =
        value?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }.orEmpty()
}

data class TransferPendingRequest(
    val requestId: String,
    val senderName: String,
    val senderShortId: String,
    val senderHost: String,
    val fileName: String,
    val sizeBytes: Long,
) {
    val senderLabel: String
        get() = when {
            senderName.isBlank() && senderShortId.isBlank() -> senderHost
            senderShortId.isBlank() -> senderName
            senderName.isBlank() -> senderShortId
            else -> "$senderName · $senderShortId"
        }
}

sealed interface TransferSendResult {
    data class Delivered(val message: String?) : TransferSendResult
    data object Rejected : TransferSendResult
    data class Failed(val reason: String) : TransferSendResult
}
