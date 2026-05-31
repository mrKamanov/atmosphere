/**
 * Константы маршрутов Navigation Compose и построение пути экрана урока с URL-кодированием `classId`.
 * UI-слой (навигация).
 */
package ru.golpom.atmosphere.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object NavDestinations {
    const val TEACHER_HOME = "teacher_home"
    const val DEPUTY_HOME = "deputy_home"
    const val LESSON = "lesson/{classId}/{subjectKey}"
    const val STUDENT_PROFILE = "student_profile/{studentId}"
    const val SETTINGS = "settings"
    const val TEACHER_SCHEDULE = "teacher_schedule"
    const val TEACHER_CLASSES = "teacher_classes"
    const val TEACHER_CLASS_DETAIL = "teacher_class_detail/{classId}"
    const val TEACHER_STUDENTS = "teacher_students"
    const val TEACHER_MEETINGS = "teacher_meetings"
    const val DEPUTY_CLASS_DETAIL = "deputy_class_detail/{classId}"

    fun lesson(classId: String, subjectKey: String): String =
        "lesson/${URLEncoder.encode(classId, StandardCharsets.UTF_8.name())}/$subjectKey"

    fun studentProfile(studentId: String): String =
        "student_profile/${URLEncoder.encode(studentId, StandardCharsets.UTF_8.name())}"

    fun teacherClassDetail(classId: String): String =
        "teacher_class_detail/${URLEncoder.encode(classId, StandardCharsets.UTF_8.name())}"

    fun deputyClassDetail(classId: String): String =
        "deputy_class_detail/${URLEncoder.encode(classId, StandardCharsets.UTF_8.name())}"
}
