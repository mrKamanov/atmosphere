/**
 * Переключение между домашними экранами учителя и завуча.
 * UI-слой (навигация).
 */
package ru.golpom.atmosphere.ui.navigation

import androidx.navigation.NavController

fun NavController.navigateToHome(route: String) {
    require(route == NavDestinations.TEACHER_HOME || route == NavDestinations.DEPUTY_HOME) {
        "Home navigation only supports teacher_home or deputy_home, got: $route"
    }
    val popTarget = if (route == NavDestinations.DEPUTY_HOME) {
        NavDestinations.TEACHER_HOME
    } else {
        NavDestinations.DEPUTY_HOME
    }
    navigate(route) {
        popUpTo(popTarget) { inclusive = true }
        launchSingleTop = true
    }
}
