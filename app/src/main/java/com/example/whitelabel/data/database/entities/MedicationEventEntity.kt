package com.example.whitelabel.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_events")
data class MedicationEventEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val prescriptionId: String,
    val title: String,
    val description: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val notes: String? = null,
    val reminderSent: Boolean = false,
    val calendarEventId: Long? = null, // Reference to actual calendar event if synced
    val createdAt: Long = System.currentTimeMillis()
)
