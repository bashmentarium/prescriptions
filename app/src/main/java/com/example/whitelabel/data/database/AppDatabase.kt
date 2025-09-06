package com.example.whitelabel.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 1,
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
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whitelabel_database"
                )
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
