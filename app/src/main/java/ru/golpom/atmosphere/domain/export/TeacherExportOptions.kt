/**
 * Параметры передачи отчёта (мессенджер, почта).
 */
package ru.golpom.atmosphere.domain.export

data class TeacherExportOptions(
    /** Имя файла без фамилии и названий классов — меньше ПДн в превью чата. */
    val neutralFileName: Boolean = true,
)
