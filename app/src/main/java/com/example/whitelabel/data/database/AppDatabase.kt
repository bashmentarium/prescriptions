package com.example.whitelabel.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.whitelabel.data.database.dao.MedicationEventDao
import com.example.whitelabel.data.database.dao.PrescriptionDao
import com.example.whitelabel.data.database.dao.ChatConversationDao
import com.example.whitelabel.data.database.dao.ChatMessageDao
import com.example.whitelabel.data.database.entities.MedicationEventEntity
import com.example.whitelabel.data.database.entities.PrescriptionEntity
import com.example.whitelabel.data.database.entities.ParsedMedicationEntity
import com.example.whitelabel.data.database.entities.ChatConversationEntity
import com.example.whitelabel.data.database.entities.ChatMessageEntity
import com.example.whitelabel.data.database.converters.MedicationListConverter
import com.example.whitelabel.data.database.converters.StringListConverter
import com.example.whitelabel.data.database.converters.FoodTimingConverter

@Database(
    entities = [
        PrescriptionEntity::class,
        ParsedMedicationEntity::class,
        MedicationEventEntity::class,
        ChatConversationEntity::class,
        ChatMessageEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(
    MedicationListConverter::class,
    StringListConverter::class,
    FoodTimingConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun medicationEventDao(): MedicationEventDao
    abstract fun chatConversationDao(): ChatConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the new startDateMillis column to prescriptions table
                database.execSQL("ALTER TABLE prescriptions ADD COLUMN startDateMillis INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        // Migration from version 2 to 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create chat_conversations table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_conversations (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        prescriptionPreview TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1
                    )
                """)
                
                // Create chat_messages table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        conversationId TEXT NOT NULL,
                        fromUser INTEGER NOT NULL,
                        text TEXT NOT NULL DEFAULT '',
                        imageUri TEXT,
                        isProcessing INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }
        
        // Migration from version 3 to 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the new intervalDays column to prescriptions table
                database.execSQL("ALTER TABLE prescriptions ADD COLUMN intervalDays INTEGER NOT NULL DEFAULT 1")
            }
        }
        
        // Migration from version 4 to 5
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the new foodTiming column to prescriptions table
                database.execSQL("ALTER TABLE prescriptions ADD COLUMN foodTiming TEXT NOT NULL DEFAULT 'NEUTRAL'")
                // Drop the old withFood column
                database.execSQL("ALTER TABLE prescriptions DROP COLUMN withFood")
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whitelabel_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration() // For development - remove in production
                .allowMainThreadQueries() // Allow main thread queries for development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
