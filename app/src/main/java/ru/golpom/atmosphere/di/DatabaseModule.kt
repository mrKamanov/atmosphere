/**
 * Hilt-модуль: Room-база и DAO.
 * DI-слой.
 */
package ru.golpom.atmosphere.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.golpom.atmosphere.data.local.AtmosphereDatabase
import ru.golpom.atmosphere.data.local.dao.BehaviorLogDao
import ru.golpom.atmosphere.data.local.dao.ClassDao
import ru.golpom.atmosphere.data.local.dao.ImportBatchDao
import ru.golpom.atmosphere.data.local.dao.MeetingDao
import ru.golpom.atmosphere.data.local.dao.NotificationDao
import ru.golpom.atmosphere.data.local.dao.ScheduleEntryDao
import ru.golpom.atmosphere.data.local.dao.StudentDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AtmosphereDatabase =
        Room.databaseBuilder(
            context,
            AtmosphereDatabase::class.java,
            "atmosphere.db",
        ).addMigrations(
            AtmosphereDatabase.MIGRATION_3_4,
            AtmosphereDatabase.MIGRATION_4_5,
            AtmosphereDatabase.MIGRATION_5_6,
            AtmosphereDatabase.MIGRATION_6_7,
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideClassDao(db: AtmosphereDatabase): ClassDao = db.classDao()

    @Provides
    fun provideStudentDao(db: AtmosphereDatabase): StudentDao = db.studentDao()

    @Provides
    fun provideBehaviorLogDao(db: AtmosphereDatabase): BehaviorLogDao = db.behaviorLogDao()

    @Provides
    fun provideImportBatchDao(db: AtmosphereDatabase): ImportBatchDao = db.importBatchDao()

    @Provides
    fun provideScheduleEntryDao(db: AtmosphereDatabase): ScheduleEntryDao = db.scheduleEntryDao()

    @Provides
    fun provideMeetingDao(db: AtmosphereDatabase): MeetingDao = db.meetingDao()

    @Provides
    fun provideNotificationDao(db: AtmosphereDatabase): NotificationDao = db.notificationDao()
}
