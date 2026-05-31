/**
 * Сборка и распаковка ZIP-пакета отчёта.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.export

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Внутренний ZIP (manifest + CSV); снаружи шифруется [AtmosphereReportCodec]. */
object TeacherReportZip {
    const val MANIFEST_ENTRY = "manifest.json"
    const val LOGS_ENTRY = "behavior_logs.csv"

    data class Contents(
        val manifest: ExportManifestDto?,
        val csv: String?,
    )

    fun pack(manifestJson: String, csvBody: String): ByteArray {
        ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(manifestJson.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry(LOGS_ENTRY))
                zip.write(csvBody.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            return baos.toByteArray()
        }
    }

    fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

    fun unpack(bytes: ByteArray): Contents {
        var manifest: ExportManifestDto? = null
        var csv: String? = null
        ZipInputStream(bytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/')
                val data = zis.readBytes()
                when {
                    name.equals(MANIFEST_ENTRY, ignoreCase = true) ->
                        manifest = runCatching {
                            ExportManifestCodec.decode(String(data, Charsets.UTF_8))
                        }.getOrNull()
                    name.equals(LOGS_ENTRY, ignoreCase = true) ||
                        name.endsWith(".csv", ignoreCase = true) ->
                        if (csv == null) csv = String(data, Charsets.UTF_8)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return Contents(manifest, csv)
    }
}
