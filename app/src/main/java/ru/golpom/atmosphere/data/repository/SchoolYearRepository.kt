/**
 * Переход на новый учебный год: построение плана и применение.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import ru.golpom.atmosphere.data.local.dao.SchoolYearDao
import ru.golpom.atmosphere.data.preferences.UserPreferencesRepository
import ru.golpom.atmosphere.domain.schoolyear.GraduateAction
import ru.golpom.atmosphere.domain.schoolyear.SchoolYearRollover
import ru.golpom.atmosphere.domain.schoolyear.SchoolYearRolloverPlan

@Singleton
class SchoolYearRepository @Inject constructor(
    private val schoolYearDao: SchoolYearDao,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    val lastRolloverMillis: Flow<Long?> = userPreferencesRepository.lastSchoolYearRolloverMillis

    suspend fun buildPlan(
        graduationGrade: Int = SchoolYearRollover.DEFAULT_GRADUATION_GRADE,
    ): SchoolYearRolloverPlan {
        val counts = schoolYearDao.countTeacherActiveByClass()
            .associate { it.classId to it.studentCount }
        val scheduleClassIds = schoolYearDao.listScheduleClassIds().toSet()
        return SchoolYearRollover.buildPlan(counts, scheduleClassIds, graduationGrade)
    }

    suspend fun apply(
        plan: SchoolYearRolloverPlan,
        graduateAction: GraduateAction,
        shiftSchedule: Boolean,
    ): SchoolYearDao.RolloverResult {
        val result = schoolYearDao.applyRollover(plan, graduateAction, shiftSchedule)
        userPreferencesRepository.setLastSchoolYearRolloverMillis(System.currentTimeMillis())
        return result
    }
}
