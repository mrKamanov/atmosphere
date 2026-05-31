/**
 * Точка входа Activity: системный splash до первого кадра главного экрана.
 * UI-слой.
 */
package ru.golpom.atmosphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import ru.golpom.atmosphere.ui.AtmosphereApp
import ru.golpom.atmosphere.ui.theme.AtmosphereTheme
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val contentReady = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !contentReady.get() }
        splashScreen.setOnExitAnimationListener { provider ->
            provider.remove()
        }
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.atmosphere_mist)
        val mistArgb = ContextCompat.getColor(this, R.color.atmosphere_mist)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(mistArgb, mistArgb),
            navigationBarStyle = SystemBarStyle.light(mistArgb, mistArgb),
        )
        setContent {
            AtmosphereTheme {
                AtmosphereApp(
                    splashViewModel = hiltViewModel(),
                    onContentReady = { contentReady.set(true) },
                )
            }
        }
    }
}
