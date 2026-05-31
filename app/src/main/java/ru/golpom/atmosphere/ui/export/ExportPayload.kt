/**
 * Результат выгрузки отчёта для сохранения или «Поделиться».
 */
package ru.golpom.atmosphere.ui.export

data class ExportPayload(
    val fileName: String,
    val utf8Bytes: ByteArray,
    val mimeType: String,
    val recordCount: Int = 0,
    val periodLabel: String = "",
    /** Файл .atmo — читается только в «Атмосфере». */
    val appSealed: Boolean = false,
    /** HTML, оптимизированный под PDF; если null — используется [utf8Bytes]. */
    val pdfUtf8Bytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ExportPayload
        return fileName == other.fileName &&
            utf8Bytes.contentEquals(other.utf8Bytes) &&
            mimeType == other.mimeType &&
            recordCount == other.recordCount &&
            periodLabel == other.periodLabel &&
            appSealed == other.appSealed &&
            (pdfUtf8Bytes == null && other.pdfUtf8Bytes == null ||
                pdfUtf8Bytes != null && other.pdfUtf8Bytes != null &&
                pdfUtf8Bytes.contentEquals(other.pdfUtf8Bytes))
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + utf8Bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + recordCount
        result = 31 * result + periodLabel.hashCode()
        result = 31 * result + appSealed.hashCode()
        result = 31 * result + (pdfUtf8Bytes?.contentHashCode() ?: 0)
        return result
    }
}
