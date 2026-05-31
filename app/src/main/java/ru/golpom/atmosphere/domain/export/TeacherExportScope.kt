/**
 * Область выгрузки данных учителя.
 */
package ru.golpom.atmosphere.domain.export

sealed interface TeacherExportScope {
    data class Student(val studentId: String) : TeacherExportScope
    data class Class(val classId: String) : TeacherExportScope
    data object AllMyClasses : TeacherExportScope
    data class Subject(val subjectKey: String) : TeacherExportScope
    data object AllData : TeacherExportScope

    fun scopeTag(): String = when (this) {
        is Student -> "student-$studentId"
        is Class -> "class-$classId"
        AllMyClasses -> "classes"
        is Subject -> "subject-$subjectKey"
        AllData -> "all"
    }
}

data class TeacherExportRequest(
    val scope: TeacherExportScope,
    val period: ExportPeriodSelection,
    val options: TeacherExportOptions = TeacherExportOptions(),
)
