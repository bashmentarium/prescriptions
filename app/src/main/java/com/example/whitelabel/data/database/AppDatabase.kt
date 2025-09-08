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
import com.example.whitelabel.data.database.entities.MedicationEventEntity
import com.example.whitelabel.data.database.entities.PrescriptionEntity
import com.example.whitelabel.data.database.entities.ParsedMedicationEntity
import com.example.whitelabel.data.database.converters.MedicationListConverter
import com.example.whitelabel.data.database.converters.StringListConverter

@Database(
    entities = [
        PrescriptionEntity::class,
        ParsedMedicationEntity::class,
        MedicationEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(
    MedicationListConverter::class,
    StringListConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun medicationEventDao(): MedicationEventDao
    
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
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whitelabel_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
