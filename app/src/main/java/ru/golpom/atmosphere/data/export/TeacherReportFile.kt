/**
 * Имена и расширения файлов отчёта учителя.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.export

/**
 * Упаковка/распаковка отчёта учителя: sealed [.atmo] или legacy ZIP/CSV.
 */
object TeacherReportFile {
    const val MIME_TYPE = "application/vnd.atmosphere.report"
    const val EXTENSION = "atmo"

    data class Contents(
        val manifest: ExportManifestDto?,
        val csv: String?,
        val unrecognized: Boolean = false,
    )

    fun seal(manifestJson: String, csvBody: String): ByteArray {
        val zip = TeacherReportZip.pack(manifestJson, csvBody)
        return AtmosphereReportCodec.seal(zip)
    }

    fun open(bytes: ByteArray): Contents {
        val zipBytes = AtmosphereReportCodec.open(bytes)
            ?: if (TeacherReportZip.isZip(bytes)) bytes else null
        if (zipBytes == null) {
            return Contents(null, null, unrecognized = true)
        }
        val inner = TeacherReportZip.unpack(zipBytes)
        return Contents(inner.manifest, inner.csv)
    }

    fun isSealed(bytes: ByteArray): Boolean = AtmosphereReportCodec.isSealed(bytes)
}
