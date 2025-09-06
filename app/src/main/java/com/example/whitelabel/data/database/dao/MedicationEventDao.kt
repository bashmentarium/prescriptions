package com.example.whitelabel.data.database.dao

import androidx.room.*
import com.example.whitelabel.data.database.entities.MedicationEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationEventDao {
    
    @Query("SELECT * FROM medication_events WHERE prescriptionId = :prescriptionId ORDER BY startTimeMillis ASC")
    fun getEventsByPrescriptionId(prescriptionId: String): Flow<List<MedicationEventEntity>>
    
    @Query("SELECT * FROM medication_events WHERE id = :id")
    suspend fun getEventById(id: String): MedicationEventEntity?
    
    @Query("SELECT * FROM medication_events WHERE startTimeMillis >= :startTime AND startTimeMillis <= :endTime ORDER BY startTimeMillis ASC")
    fun getEventsInTimeRange(startTime: Long, endTime: Long): Flow<List<MedicationEventEntity>>
    
    @Query("SELECT * FROM medication_events WHERE isCompleted = 0 AND startTimeMillis >= :currentTime ORDER BY startTimeMillis ASC")
    fun getUpcomingEvents(currentTime: Long = System.currentTimeMillis()): Flow<List<MedicationEventEntity>>
    
    @Query("SELECT * FROM medication_events WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedEvents(): Flow<List<MedicationEventEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: MedicationEventEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<MedicationEventEntity>)
    
    @Update
    suspend fun updateEvent(event: MedicationEventEntity)
    
    @Query("UPDATE medication_events SET isCompleted = 1, completedAt = :completedAt WHERE id = :eventId")
    suspend fun markEventCompleted(eventId: String, completedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE medication_events SET isCompleted = 0, completedAt = NULL WHERE id = :eventId")
    suspend fun markEventIncomplete(eventId: String)
    
    @Query("UPDATE medication_events SET reminderSent = 1 WHERE id = :eventId")
    suspend fun markReminderSent(eventId: String)
    
    @Query("UPDATE medication_events SET calendarEventId = :calendarEventId WHERE id = :eventId")
    suspend fun updateCalendarEventId(eventId: String, calendarEventId: Long)
    
    @Delete
    suspend fun deleteEvent(event: MedicationEventEntity)
    
    @Query("DELETE FROM medication_events WHERE prescriptionId = :prescriptionId")
    suspend fun deleteEventsByPrescriptionId(prescriptionId: String)
    
    @Query("DELETE FROM medication_events WHERE id = :id")
    suspend fun deleteEventById(id: String)
    
    @Query("SELECT COUNT(*) FROM medication_events WHERE prescriptionId = :prescriptionId")
    suspend fun getEventCountByPrescriptionId(prescriptionId: String): Int
    
    @Query("SELECT COUNT(*) FROM medication_events WHERE prescriptionId = :prescriptionId AND isCompleted = 1")
    suspend fun getCompletedEventCountByPrescriptionId(prescriptionId: String): Int
}
