package com.example.notificationauditor.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.notificationauditor.data.db.dao.DailyInsightDao
import com.example.notificationauditor.data.db.dao.NotificationEventDao
import com.example.notificationauditor.data.db.dao.StudySessionDao
import com.example.notificationauditor.data.db.entity.DailyInsight
import com.example.notificationauditor.data.db.entity.NotificationEvent
import com.example.notificationauditor.data.db.entity.StudySession

@Database(
    entities = [StudySession::class, NotificationEvent::class, DailyInsight::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studySessionDao(): StudySessionDao
    abstract fun notificationEventDao(): NotificationEventDao
    abstract fun dailyInsightDao(): DailyInsightDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE daily_insights ADD COLUMN channelBreakdownJson TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notification_auditor.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
            }
        }
    }
}
