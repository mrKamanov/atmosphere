/**
 * Процесс приложения с Hilt: граф зависимостей на уровне Application.
 * Android entry point.
 */
package ru.golpom.atmosphere

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AtmosphereApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
    }
}

object NotificationChannels {
    const val CHANNEL_ID = "atmosphere_notifications"

    fun create(context: android.content.Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Атмосфера",
                android.app.NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Уведомления Атмосферы"
            }
            val manager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
