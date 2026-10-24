/**
 * Состояние уроков для карточки расписания на главном экране учителя:
 * текущий идёт урок, идёт перемена или уроков сегодня больше нет, плюс ближайший следующий урок.
 * UI-слой (Compose).
 */
package ru.golpom.atmosphere.ui.teacher

import ru.golpom.atmosphere.data.local.entity.ScheduleEntryEntity

data class TeacherLessonState(
    /** Урок, который идёт прямо сейчас (null — идёт перемена или уроков нет). */
    val current: ScheduleEntryEntity? = null,
    /** Ближайший следующий урок (сегодня или в следующем учебном дне). */
    val next: ScheduleEntryEntity? = null,
    /** Есть ли ещё уроки сегодня после текущего момента. */
    val hasMoreToday: Boolean = false,
    /** Были ли уроки сегодня вообще (для разных формулировок «уроков нет»). */
    val hadLessonsToday: Boolean = false,
)
