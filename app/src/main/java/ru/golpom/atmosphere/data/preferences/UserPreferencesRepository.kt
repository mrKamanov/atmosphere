/**
 * DataStore: роль, настройки экспорта и локальных данных.
 * Data-слой.
 */
package ru.golpom.atmosphere.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.golpom.atmosphere.domain.AppRole

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore get() = context.dataStore

    val appRole: Flow<AppRole> = dataStore.data.map { prefs ->
        when (prefs[KEY_ROLE]) {
            "teacher" -> AppRole.TEACHER
            "deputy" -> AppRole.DEPUTY
            else -> AppRole.NOT_SET
        }
    }

    val teacherLastName: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_TEACHER_LAST_NAME]?.trim()?.takeIf { it.isNotEmpty() } ?: "Учитель"
    }

    val userName: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME]?.trim() ?: ""
    }

    val meetingRemindersEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_MEETING_REMINDERS] ?: true
    }

    val lessonRemindersEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_LESSON_REMINDERS] ?: true
    }

    val systemNotificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SYSTEM_NOTIFICATIONS] ?: true
    }

    /** Нейтральное имя файла без фамилии и меток класса. */
    val exportNeutralFileName: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_EXPORT_NEUTRAL_FILENAME] ?: true
    }

    suspend fun setRole(role: AppRole) {
        dataStore.edit { prefs ->
            prefs[KEY_ROLE] = when (role) {
                AppRole.TEACHER -> "teacher"
                AppRole.DEPUTY -> "deputy"
                AppRole.NOT_SET -> ""
            }
        }
    }

    suspend fun setTeacherLastName(value: String) {
        dataStore.edit { prefs ->
            prefs[KEY_TEACHER_LAST_NAME] = value.trim().ifEmpty { "Учитель" }
        }
    }

    /**
     * Стабильный идентификатор "профиля учителя" на этом устройстве.
     * Используется в имени экспортируемого файла, чтобы различать нескольких учителей с одинаковой фамилией.
     */
    suspend fun getOrCreateTeacherProfileId(): String {
        val existing = dataStore.data.first()[KEY_TEACHER_PROFILE_ID]?.takeIf { it.isNotBlank() }
        if (existing != null) return existing

        val created = UUID.randomUUID().toString()
        dataStore.edit { prefs ->
            prefs[KEY_TEACHER_PROFILE_ID] = created
        }
        return created
    }

    /**
     * Укороченная (без `-`) версия UUID — удобна для встраивания в имя файла.
     */
    suspend fun getOrCreateTeacherProfileShortId(
        shortLen: Int = TEACHER_PROFILE_SHORT_ID_LEN,
    ): String {
        val id = getOrCreateTeacherProfileId().replace("-", "")
        return id.take(shortLen)
    }

    suspend fun setUserName(value: String) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = value.trim()
        }
    }

    suspend fun setMeetingRemindersEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_MEETING_REMINDERS] = enabled
        }
    }

    suspend fun setLessonRemindersEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_LESSON_REMINDERS] = enabled
        }
    }

    suspend fun setSystemNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SYSTEM_NOTIFICATIONS] = enabled
        }
    }

    /** Показывать в аналитике завуча отметки с этого телефона (не из отчётов учителей). */
    val deputyLocalDataEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DEPUTY_LOCAL_DATA] ?: true
    }

    suspend fun setDeputyLocalDataEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DEPUTY_LOCAL_DATA] = enabled
        }
    }

    suspend fun setExportNeutralFileName(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_EXPORT_NEUTRAL_FILENAME] = enabled
        }
    }

    companion object {
        /** Длина короткого ID в имени файла экспорта (12 hex ≈ 48 бит). */
        const val TEACHER_PROFILE_SHORT_ID_LEN = 12

        private val KEY_ROLE = stringPreferencesKey("app_role")
        private val KEY_TEACHER_LAST_NAME = stringPreferencesKey("teacher_last_name")
        private val KEY_TEACHER_PROFILE_ID = stringPreferencesKey("teacher_profile_id")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_MEETING_REMINDERS = booleanPreferencesKey("meeting_reminders")
        private val KEY_LESSON_REMINDERS = booleanPreferencesKey("lesson_reminders")
        private val KEY_SYSTEM_NOTIFICATIONS = booleanPreferencesKey("system_notifications")
        private val KEY_EXPORT_NEUTRAL_FILENAME = booleanPreferencesKey("export_neutral_filename")
        private val KEY_DEPUTY_LOCAL_DATA = booleanPreferencesKey("deputy_local_data_enabled")
    }
}
