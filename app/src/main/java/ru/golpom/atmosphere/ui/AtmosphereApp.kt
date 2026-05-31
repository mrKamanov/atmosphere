/**
 * Корневой граф навигации Compose: дом учителя/завуча, экран урока.
 * UI-слой.
 */
package ru.golpom.atmosphere.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.golpom.atmosphere.ui.deputy.DeputyClassDetailScreen
import ru.golpom.atmosphere.ui.deputy.DeputyHomeScreen
import ru.golpom.atmosphere.ui.lesson.LessonScreen
import ru.golpom.atmosphere.ui.navigation.NavDestinations
import ru.golpom.atmosphere.ui.navigation.navigateToHome
import ru.golpom.atmosphere.ui.splash.SplashViewModel
import ru.golpom.atmosphere.ui.student.StudentProfileScreen
import ru.golpom.atmosphere.ui.settings.SettingsScreen
import ru.golpom.atmosphere.ui.classes.ClassDetailScreen
import ru.golpom.atmosphere.ui.teacher.TeacherHomeScreen
import ru.golpom.atmosphere.ui.theme.SurfaceBg
import ru.golpom.atmosphere.ui.theme.reportWhenDrawn

@Composable
fun AtmosphereApp(
    splashViewModel: SplashViewModel,
    modifier: Modifier = Modifier,
    onContentReady: () -> Unit = {},
) {
    var startRoute by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startRoute = splashViewModel.resolveStartRoute()
    }

    if (startRoute == null) {
        Box(Modifier.fillMaxSize().background(SurfaceBg))
        return
    }

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = startRoute!!,
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBg),
    ) {
        composable(NavDestinations.TEACHER_HOME) {
            TeacherHomeScreen(
                modifier = Modifier.reportWhenDrawn(onContentReady),
                viewModel = hiltViewModel(),
                onOpenLesson = { classId, subjectKey ->
                    navController.navigate(NavDestinations.lesson(classId, subjectKey))
                },
                onNavigateToHome = { route -> navController.navigateToHome(route) },
                onOpenSettings = {
                    navController.navigate(NavDestinations.SETTINGS)
                },
                onOpenSchedule = {
                    navController.navigate(NavDestinations.TEACHER_SCHEDULE)
                },
                onOpenClasses = {
                    navController.navigate(NavDestinations.TEACHER_CLASSES)
                },
                onOpenClassDetail = { classId ->
                    navController.navigate(NavDestinations.teacherClassDetail(classId))
                },
                onOpenStudents = {
                    navController.navigate(NavDestinations.TEACHER_STUDENTS)
                },
                onOpenMeetings = {
                    navController.navigate(NavDestinations.TEACHER_MEETINGS)
                },
            )
        }
        composable(NavDestinations.DEPUTY_HOME) {
            DeputyHomeScreen(
                modifier = Modifier.reportWhenDrawn(onContentReady),
                viewModel = hiltViewModel(),
                onNavigateToHome = { route -> navController.navigateToHome(route) },
                onOpenClassDetail = { classId ->
                    navController.navigate(NavDestinations.deputyClassDetail(classId))
                },
                onOpenStudentProfile = { studentId ->
                    navController.navigate(NavDestinations.studentProfile(studentId))
                },
            )
        }
        composable(
            route = NavDestinations.DEPUTY_CLASS_DETAIL,
            arguments = listOf(navArgument("classId") { type = NavType.StringType }),
        ) {
            val classId = it.arguments?.getString("classId") ?: ""
            DeputyClassDetailScreen(
                classId = classId,
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onOpenStudentProfile = { studentId ->
                    navController.navigate(NavDestinations.studentProfile(studentId))
                },
            )
        }
        composable(
            route = NavDestinations.LESSON,
            arguments = listOf(
                navArgument("classId") { type = NavType.StringType },
                navArgument("subjectKey") { type = NavType.StringType },
            ),
        ) {
            LessonScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onOpenStudentProfile = { studentId ->
                    navController.navigate(NavDestinations.studentProfile(studentId))
                },
            )
        }
        composable(
            route = NavDestinations.STUDENT_PROFILE,
            arguments = listOf(
                navArgument("studentId") { type = NavType.StringType },
            ),
        ) {
            StudentProfileScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(NavDestinations.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(NavDestinations.TEACHER_SCHEDULE) {
            ru.golpom.atmosphere.ui.schedule.ScheduleScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onOpenLesson = { classId, subjectKey ->
                    navController.navigate(NavDestinations.lesson(classId, subjectKey))
                },
            )
        }
        composable(NavDestinations.TEACHER_CLASSES) {
            ru.golpom.atmosphere.ui.classes.ClassesScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onOpenClassDetail = { classId ->
                    navController.navigate(NavDestinations.teacherClassDetail(classId))
                },
                onOpenLesson = { classId, subjectKey ->
                    navController.navigate(NavDestinations.lesson(classId, subjectKey))
                },
            )
        }
        composable(
            route = NavDestinations.TEACHER_STUDENTS,
        ) {
            ru.golpom.atmosphere.ui.students.StudentsScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onOpenStudentProfile = { studentId ->
                    navController.navigate(NavDestinations.studentProfile(studentId))
                },
            )
        }
        composable(
            route = NavDestinations.TEACHER_CLASS_DETAIL,
            arguments = listOf(navArgument("classId") { type = NavType.StringType }),
        ) {
            val classId = it.arguments?.getString("classId") ?: ""
            ClassDetailScreen(
                classId = classId,
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onOpenStudentProfile = { studentId ->
                    navController.navigate(NavDestinations.studentProfile(studentId))
                },
            )
        }
        composable(NavDestinations.TEACHER_MEETINGS) {
            ru.golpom.atmosphere.ui.meetings.MeetingsScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
