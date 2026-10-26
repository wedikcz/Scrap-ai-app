package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AiChatMessage
import com.example.data.model.PinoutItem
import com.example.data.model.RagDocument
import com.example.data.model.SalvageItem
import com.example.data.model.SalvageProject
import com.example.data.model.SystemErrorLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SalvageItem::class,
        PinoutItem::class,
        SalvageProject::class,
        RagDocument::class,
        AiChatMessage::class,
        SystemErrorLog::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ScrapMasterDatabase : RoomDatabase() {

    abstract fun scrapMasterDao(): ScrapMasterDao
    abstract fun pinoutDao(): PinoutDao

    companion object {
        /**
         * Adds the assistant memory table without touching the user's inventory or
         * Kanban data. A destructive fallback here would wipe a salvage vault on
         * update, which is unacceptable for a workshop app.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ai_chat_messages` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`sessionId` TEXT NOT NULL, " +
                        "`role` TEXT NOT NULL, " +
                        "`content` TEXT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL)"
                )
            }
        }

        @Volatile
        private var INSTANCE: ScrapMasterDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ScrapMasterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScrapMasterDatabase::class.java,
                    "scrapmaster_omega.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(true)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.scrapMasterDao())
                    }
                }
            }

            suspend fun populateDatabase(dao: ScrapMasterDao) {
                dao.insertSalvageItems(SeedData.INITIAL_INVENTORY)
                dao.insertPinouts(SeedData.INITIAL_PINOUTS)
                dao.insertProjects(SeedData.INITIAL_PROJECTS)
                dao.insertRagDocuments(SeedData.INITIAL_RAG_DOCS)
                dao.insertErrorLog(
                    SystemErrorLog(
                        errorCode = "SYS_INIT_SUCCESS",
                        message = "ScrapMaster AI Database v6.0 Initialized with Self-Healing schema",
                        component = "RoomDatabase"
                    )
                )
            }
        }
    }
}
