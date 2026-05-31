/**
 * Модуль AtmosphereReportCodec.
 * Слой по пакету.
 */
package ru.golpom.atmosphere.data.export

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Контейнер отчёта «только для Атмосферы»: ATMO + версия + nonce + AES-GCM(внутренний ZIP).
 * В мессенджере/почте файл не открывается как архив или таблица.
 */
internal object AtmosphereReportCodec {
    private const val MAGIC = "ATMO"
    private const val VERSION: Byte = 1
    private const val NONCE_LEN = 12
    private const val TAG_BITS = 128
    private const val KEY_SALT = "ru.golpom.atmosphere::report-seal-v1"

    private val key: ByteArray by lazy {
        MessageDigest.getInstance("SHA-256").digest(KEY_SALT.toByteArray(Charsets.UTF_8))
    }

    fun isSealed(bytes: ByteArray): Boolean =
        bytes.size >= MAGIC.length + 1 + NONCE_LEN + TAG_BITS / 8 &&
            bytes.copyOfRange(0, MAGIC.length).decodeToString() == MAGIC

    fun seal(plainZip: ByteArray): ByteArray {
        val nonce = ByteArray(NONCE_LEN).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        val encrypted = cipher.doFinal(plainZip)
        return MAGIC.encodeToByteArray() + byteArrayOf(VERSION) + nonce + encrypted
    }

    fun open(sealed: ByteArray): ByteArray? {
        if (!isSealed(sealed)) return null
        var offset = MAGIC.length
        val version = sealed[offset++]
        if (version != VERSION) return null
        val nonce = sealed.copyOfRange(offset, offset + NONCE_LEN)
        offset += NONCE_LEN
        val ciphertext = sealed.copyOfRange(offset, sealed.size)
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.doFinal(ciphertext)
        }.getOrNull()
    }
}
