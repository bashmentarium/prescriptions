package com.example.whitelabel.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.whitelabel.data.database.converters.MedicationListConverter
import com.example.whitelabel.data.database.converters.StringListConverter

@Entity(tableName = "prescriptions")
@TypeConverters(MedicationListConverter::class, StringListConverter::class)
data class PrescriptionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val medications: List<ParsedMedicationEntity>,
    val timesPerDay: Int,
    val preferredTimes: List<String>,
    val withFood: Boolean,
    val durationDays: Int,
    val startTimeMinutes: Int,
    val endTimeMinutes: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "parsed_medications")
data class ParsedMedicationEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val prescriptionId: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val instructions: String
)
