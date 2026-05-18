package com.example.notificationauditor.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.notificationauditor.data.db.dao.DailyInsightDao
import com.example.notificationauditor.data.db.dao.FilterRuleDao
import com.example.notificationauditor.data.db.dao.NotificationEventDao
import com.example.notificationauditor.data.db.dao.StudySessionDao
import com.example.notificationauditor.data.db.entity.DailyInsight
import com.example.notificationauditor.data.db.entity.FilterRule
import com.example.notificationauditor.data.db.entity.NotificationEvent
import com.example.notificationauditor.data.db.entity.StudySession

@Database(
    entities = [StudySession::class, NotificationEvent::class, DailyInsight::class, FilterRule::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studySessionDao(): StudySessionDao
    abstract fun notificationEventDao(): NotificationEventDao
    abstract fun dailyInsightDao(): DailyInsightDao
    abstract fun filterRuleDao(): FilterRuleDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE daily_insights ADD COLUMN lastUpdatedAt INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS filter_rules (
                        ruleId      INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ruleType    TEXT    NOT NULL,
                        pattern     TEXT    NOT NULL,
                        packageName TEXT,
                        channelId   TEXT,
                        action      TEXT    NOT NULL,
                        isEnabled   INTEGER NOT NULL DEFAULT 1,
                        createdAt   INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL(
                    "ALTER TABLE notification_events ADD COLUMN filteredByRuleId INTEGER"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notification_events_filteredByRuleId ON notification_events (filteredByRuleId)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notification_events ADD COLUMN filterEffect TEXT"
                )
                db.execSQL(
                    "ALTER TABLE notification_events ADD COLUMN excludeFromAnalytics INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notification_events_excludeFromAnalytics ON notification_events (excludeFromAnalytics)"
                )
                db.execSQL(
                    "UPDATE filter_rules SET action = 'TAG_ONLY' WHERE action = 'FLAG'"
                )
                db.execSQL(
                    "UPDATE filter_rules SET action = 'SUPPRESS_EXCLUDE' WHERE action = 'SUPPRESS'"
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build().also { instance = it }
            }
        }
    }
}
