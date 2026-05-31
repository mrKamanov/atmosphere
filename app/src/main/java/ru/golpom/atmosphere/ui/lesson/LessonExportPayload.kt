/**
 * UTF-8 тело CSV и предлагаемое имя файла после выбора места сохранения (SAF CreateDocument).
 * UI-слой; создаётся в [LessonViewModel].
 */
package ru.golpom.atmosphere.ui.lesson

data class LessonExportPayload(
    val fileName: String,
    val utf8Bytes: ByteArray,
)
