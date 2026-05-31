/**
 * Стартовый маршрут по сохранённой роли пользователя.
 * UI-слой (ViewModel).
 */
package ru.golpom.atmosphere.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.golpom.atmosphere.data.preferences.UserPreferencesRepository
import ru.golpom.atmosphere.domain.AppRole
import ru.golpom.atmosphere.ui.navigation.NavDestinations

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    suspend fun resolveStartRoute(): String = withContext(Dispatchers.IO) {
        when (val role = userPreferencesRepository.appRole.first()) {
            AppRole.DEPUTY -> NavDestinations.DEPUTY_HOME
            AppRole.TEACHER -> NavDestinations.TEACHER_HOME
            AppRole.NOT_SET -> {
                userPreferencesRepository.setRole(AppRole.TEACHER)
                NavDestinations.TEACHER_HOME
            }
        }
    }
}
