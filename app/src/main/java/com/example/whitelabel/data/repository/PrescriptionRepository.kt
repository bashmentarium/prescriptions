
package com.example.whitelabel.data.repository

import com.example.whitelabel.data.ParsedPrescription
import com.example.whitelabel.data.ParsedMedicationsOnly
import com.example.whitelabel.data.ScheduleAggregator
import com.example.whitelabel.data.UserSettings
import com.example.whitelabel.data.database.AppDatabase
import com.example.whitelabel.data.database.entities.MedicationEventEntity
import com.example.whitelabel.data.database.entities.PrescriptionEntity
import com.example.whitelabel.data.database.entities.ParsedMedicationEntity
import com.example.whitelabel.service.MedicationReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

class PrescriptionRepository(private val database: AppDatabase) {
    
    private val prescriptionDao = database.prescriptionDao()
    val eventDao = database.medicationEventDao()
    private var reminderScheduler: MedicationReminderScheduler? = null
    
    fun setReminderScheduler(scheduler: MedicationReminderScheduler) {
        this.reminderScheduler = scheduler
    }
    
    // Prescription operations
    fun getAllActivePrescriptions(): Flow<List<PrescriptionEntity>> {
        return prescriptionDao.getAllActivePrescriptions()
    }
    
    suspend fun getAllPrescriptions(): List<PrescriptionEntity> {
        return prescriptionDao.getAllPrescriptions()
    }
    
    suspend fun getPrescriptionById(id: String): PrescriptionEntity? {
        return prescriptionDao.getPrescriptionById(id)
    }
    
    fun getPrescriptionByIdFlow(id: String): Flow<PrescriptionEntity?> {
        return prescriptionDao.getPrescriptionByIdFlow(id)
    }
    
    suspend fun savePrescription(prescription: PrescriptionEntity) {
        prescriptionDao.insertPrescription(prescription)
    }
    
    suspend fun updatePrescription(prescription: PrescriptionEntity) {
        prescriptionDao.updatePrescription(prescription)
    }
    
    suspend fun deactivatePrescription(id: String) {
        prescriptionDao.deactivatePrescription(id)
    }
    
    suspend fun deletePrescription(id: String) {
        prescriptionDao.deletePrescriptionById(id)
        eventDao.deleteEventsByPrescriptionId(id)
    }
    
    // Event operations
    fun getEventsByPrescriptionId(prescriptionId: String): Flow<List<MedicationEventEntity>> {
        return eventDao.getEventsByPrescriptionId(prescriptionId)
    }
    
    fun getUpcomingEvents(): Flow<List<MedicationEventEntity>> {
        return eventDao.getUpcomingEvents()
    }
    
    fun getCompletedEvents(): Flow<List<MedicationEventEntity>> {
        return eventDao.getCompletedEvents()
    }
    
    fun getEventsInTimeRange(startTime: Long, endTime: Long): Flow<List<MedicationEventEntity>> {
        return eventDao.getEventsInTimeRange(startTime, endTime)
    }
    
    suspend fun getEventsInTimeRangeSuspend(startTime: Long, endTime: Long): List<MedicationEventEntity> {
        return eventDao.getEventsInTimeRangeSuspend(startTime, endTime)
    }
    
    suspend fun markEventCompleted(eventId: String) {
        eventDao.markEventCompleted(eventId)
    }
    
    suspend fun markEventIncomplete(eventId: String) {
        eventDao.markEventIncomplete(eventId)
    }
    
    suspend fun getEventById(eventId: String): MedicationEventEntity? {
        return eventDao.getEventById(eventId)
    }
    
    suspend fun updateEventNotes(eventId: String, notes: String) {
        val event = eventDao.getEventById(eventId)
        event?.let {
            eventDao.updateEvent(it.copy(notes = notes))
        }
    }
    
    suspend fun markReminderSent(eventId: String) {
        eventDao.markReminderSent(eventId)
    }
    
    suspend fun updateCalendarEventId(eventId: String, calendarEventId: Long) {
        eventDao.updateCalendarEventId(eventId, calendarEventId)
    }
    
    // Convert ParsedMedicationsOnly to PrescriptionEntity and create events (new method)
    suspend fun createPrescriptionWithEventsFromMedications(
        parsedMedications: ParsedMedicationsOnly,
        userSettings: UserSettings,
        title: String = "Prescription ${System.currentTimeMillis()}"
    ): String {
        val prescriptionId = UUID.randomUUID().toString()
        
        // Convert ParsedMedication to ParsedMedicationEntity
        val medicationEntities = parsedMedications.medications.map { med ->
            ParsedMedicationEntity(
                prescriptionId = prescriptionId,
                name = med.name,
                dosage = med.dosage,
                frequency = med.frequency,
                duration = med.duration,
                instructions = med.instructions
            )
        }
        
        // Aggregate schedule from medications
        val aggregatedSchedule = ScheduleAggregator.aggregateScheduleFromMedications(parsedMedications.medications)
        
        // Create PrescriptionEntity
        val prescriptionEntity = PrescriptionEntity(
            id = prescriptionId,
            title = title,
            medications = medicationEntities,
            timesPerDay = aggregatedSchedule.times_per_day,
            preferredTimes = aggregatedSchedule.preferred_times,
            foodTiming = aggregatedSchedule.food_timing,
            durationDays = aggregatedSchedule.duration_days,
            startTimeMinutes = aggregatedSchedule.start_time_minutes,
            endTimeMinutes = aggregatedSchedule.end_time_minutes,
            startDateMillis = System.currentTimeMillis(),
            intervalDays = aggregatedSchedule.interval_days
        )
        
        // Save prescription
        prescriptionDao.insertPrescription(prescriptionEntity)
        
        // Create medication events
        val events = createMedicationEvents(prescriptionEntity, userSettings)
        eventDao.insertEvents(events)
        
        // Schedule notification reminders for each event
        reminderScheduler?.let { scheduler ->
            events.forEach { event ->
                scheduler.scheduleSpecificReminder(event, prescriptionEntity.title)
            }
        }
        
        return prescriptionId
    }
    
    // Convert ParsedPrescription to PrescriptionEntity and create events (legacy method for backward compatibility)
    suspend fun createPrescriptionWithEvents(
        parsedPrescription: ParsedPrescription,
        userSettings: UserSettings,
        title: String = "Prescription ${System.currentTimeMillis()}"
    ): String {
        val prescriptionId = UUID.randomUUID().toString()
        
        // Convert ParsedMedication to ParsedMedicationEntity
        val medicationEntities = parsedPrescription.medications.map { med ->
            ParsedMedicationEntity(
                prescriptionId = prescriptionId,
                name = med.name,
                dosage = med.dosage,
                frequency = med.frequency,
                duration = med.duration,
                instructions = med.instructions
            )
        }
        
        // Use provided schedule or aggregate from medications if schedule is null
        val schedule = parsedPrescription.schedule ?: ScheduleAggregator.aggregateScheduleFromMedications(parsedPrescription.medications)
        
        // Create PrescriptionEntity
        val prescriptionEntity = PrescriptionEntity(
            id = prescriptionId,
            title = title,
            medications = medicationEntities,
            timesPerDay = schedule.times_per_day,
            preferredTimes = schedule.preferred_times,
            foodTiming = schedule.food_timing,
            durationDays = schedule.duration_days,
            startTimeMinutes = schedule.start_time_minutes,
            endTimeMinutes = schedule.end_time_minutes,
            startDateMillis = System.currentTimeMillis(),
            intervalDays = schedule.interval_days
        )
        
        // Save prescription
        prescriptionDao.insertPrescription(prescriptionEntity)
        
        // Create medication events
        val events = createMedicationEvents(prescriptionEntity, userSettings)
        eventDao.insertEvents(events)
        
        // Schedule notification reminders for each event
        reminderScheduler?.let { scheduler ->
            events.forEach { event ->
                scheduler.scheduleSpecificReminder(event, prescriptionEntity.title)
            }
        }
        
        return prescriptionId
    }
    
    private fun createMedicationEvents(
        prescription: PrescriptionEntity,
        userSettings: UserSettings
    ): List<MedicationEventEntity> {
        val events = mutableListOf<MedicationEventEntity>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = prescription.startDateMillis
        }
        
        // Calculate total number of doses based on interval
        val totalDoses = (prescription.durationDays + prescription.intervalDays - 1) / prescription.intervalDays
        
        repeat(totalDoses) { doseIndex ->
            val dayCal = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, doseIndex * prescription.intervalDays)
            }
            
            // Create multiple events per day based on prescription and user settings
            val timesPerDay = prescription.timesPerDay
            val prescriptionStartTime = prescription.startTimeMinutes
            val prescriptionEndTime = prescription.endTimeMinutes
            
            // Determine time bounds with proper precedence:
            // 1. If prescription has specific times (not defaults), use them
            // 2. Otherwise, use user settings as the primary source
            val isPrescriptionSpecific = prescriptionStartTime != 480 || prescriptionEndTime != 1200
            
            val startTime = if (isPrescriptionSpecific) {
                // Prescription has specific times, but still respect user bounds as minimum/maximum
                maxOf(prescriptionStartTime, userSettings.earliestTimeMinutes)
            } else {
                // Prescription uses defaults, use user settings
                userSettings.earliestTimeMinutes
            }
            
            val endTime = if (isPrescriptionSpecific) {
                // Prescription has specific times, but still respect user bounds as minimum/maximum
                minOf(prescriptionEndTime, userSettings.latestTimeMinutes)
            } else {
                // Prescription uses defaults, use user settings
                userSettings.latestTimeMinutes
            }
            
            // Calculate time slots with better distribution
            val timeSlots = when (timesPerDay) {
                1 -> listOf(startTime)
                2 -> {
                    // For 2 times per day, use morning and evening
                    val morningTime = maxOf(startTime, 7 * 60 + 30) // 7:30 AM
                    val eveningTime = minOf(endTime, 22 * 60) // 10:00 PM
                    listOf(morningTime, eveningTime)
                }
                3 -> {
                    // For 3 times per day, use morning, afternoon, evening
                    val morningTime = maxOf(startTime, 8 * 60) // 8:00 AM
                    val afternoonTime = (startTime + endTime) / 2 // Midday
                    val eveningTime = minOf(endTime, 20 * 60) // 8:00 PM
                    listOf(morningTime, afternoonTime, eveningTime)
                }
                else -> {
                    // For more than 3 times, distribute evenly
                    val timeRange = endTime - startTime
                    val interval = timeRange / (timesPerDay - 1)
                    (0 until timesPerDay).map { index ->
                        startTime + (interval * index)
                    }
                }
            }
            
            timeSlots.forEachIndexed { timeSlotIndex, timeInMinutes ->
                val eventCal = (dayCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, timeInMinutes / 60)
                    set(Calendar.MINUTE, timeInMinutes % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                val start = eventCal.timeInMillis
                val end = start + userSettings.eventDurationMinutes * 60 * 1000
                
                // Create event title and description with only relevant medications for this time slot
                val title = buildEventTitle(prescription, timeSlotIndex, timeSlots.size)
                val description = buildEventDescription(prescription, userSettings, timeSlotIndex, timeSlots.size)
                
                val event = MedicationEventEntity(
                    prescriptionId = prescription.id,
                    title = title,
                    description = description,
                    startTimeMillis = start,
                    endTimeMillis = end
                )
                
                events.add(event)
            }
        }
        
        return events
    }
    
    private fun buildEventTitle(prescription: PrescriptionEntity, timeSlotIndex: Int, totalTimeSlots: Int): String {
        val medications = prescription.medications
        if (medications.isEmpty()) return "Medication"
        
        // For prescriptions with multiple medications, show all medications in each event
        // This matches the expected behavior where each event shows all medications
        return when {
            medications.size == 1 -> medications.first().name
            else -> medications.joinToString(", ") { it.name }
        }
    }
    
    private fun buildEventDescription(prescription: PrescriptionEntity, userSettings: UserSettings, timeSlotIndex: Int, totalTimeSlots: Int): String {
        // Show all medications in each event description
        val medications = prescription.medications.joinToString("\n") { med ->
            "• ${med.name}: ${med.dosage} - ${med.frequency}"
        }
        
        val scheduleInfo = buildString {
            append("Schedule: ${prescription.timesPerDay} times per day")
            
            // Use prescription's food timing or user's default if prescription is neutral
            val foodTiming = if (prescription.foodTiming == com.example.whitelabel.data.FoodTiming.NEUTRAL) {
                userSettings.foodTimingDefault
            } else {
                prescription.foodTiming
            }
            
            val foodTimingText = when (foodTiming) {
                com.example.whitelabel.data.FoodTiming.BEFORE_MEAL -> " (before meal)"
                com.example.whitelabel.data.FoodTiming.DURING_MEAL -> " (during meal)"
                com.example.whitelabel.data.FoodTiming.AFTER_MEAL -> " (after meal)"
                com.example.whitelabel.data.FoodTiming.NEUTRAL -> ""
            }
            append(foodTimingText)
            
            if (prescription.preferredTimes.isNotEmpty()) {
                append("\nPreferred times: ${prescription.preferredTimes.joinToString(", ")}")
            }
            
            // Add reminder info
            if (userSettings.reminderMinutes > 0) {
                append("\nReminder: ${userSettings.reminderMinutes} minutes before")
            }
        }
        
        return "$medications\n\n$scheduleInfo"
    }
    
    
    // Update prescription and recalculate events
    suspend fun updatePrescriptionAndRecalculateEvents(
        prescription: PrescriptionEntity,
        userSettings: UserSettings,
        preservePastEvents: Boolean = true
    ) {
        // Update the prescription
        prescriptionDao.updatePrescription(prescription)
        
        if (preservePastEvents) {
            // Get current events to preserve past ones
            val currentEvents = eventDao.getEventsByPrescriptionIdSuspend(prescription.id)
            val currentTime = System.currentTimeMillis()
            
            // Separate past and future events
            val pastEvents = currentEvents.filter { it.startTimeMillis < currentTime }
            val futureEvents = currentEvents.filter { it.startTimeMillis >= currentTime }
            
            // Delete only future events
            futureEvents.forEach { event ->
                eventDao.deleteEventById(event.id)
                reminderScheduler?.cancelReminder(event.id)
            }
            
            // Create new events from the updated prescription
            val newEvents = createMedicationEvents(prescription, userSettings)
            
            // Filter out events that are in the past (shouldn't happen with new prescription, but safety check)
            val validNewEvents = newEvents.filter { it.startTimeMillis >= currentTime }
            
            // Insert new events
            eventDao.insertEvents(validNewEvents)
            
            // Schedule reminders for new events
            reminderScheduler?.let { scheduler ->
                validNewEvents.forEach { event ->
                    scheduler.scheduleSpecificReminder(event, prescription.title)
                }
            }
        } else {
            // Delete all events and recreate them
            eventDao.deleteEventsByPrescriptionId(prescription.id)
            val newEvents = createMedicationEvents(prescription, userSettings)
            eventDao.insertEvents(newEvents)
            
            // Schedule reminders for all new events
            reminderScheduler?.let { scheduler ->
                newEvents.forEach { event ->
                    scheduler.scheduleSpecificReminder(event, prescription.title)
                }
            }
        }
    }
    
    // Helper method to get events synchronously
    private suspend fun getEventsByPrescriptionIdSuspend(prescriptionId: String): List<MedicationEventEntity> {
        return eventDao.getEventsByPrescriptionIdSuspend(prescriptionId)
    }

    // Statistics
    suspend fun getPrescriptionStats(prescriptionId: String): PrescriptionStats {
        val totalEvents = eventDao.getEventCountByPrescriptionId(prescriptionId)
        val completedEvents = eventDao.getCompletedEventCountByPrescriptionId(prescriptionId)
        val completionRate = if (totalEvents > 0) (completedEvents.toFloat() / totalEvents * 100).toInt() else 0
        
        return PrescriptionStats(
            totalEvents = totalEvents,
            completedEvents = completedEvents,
            completionRate = completionRate
        )
    }
}

data class PrescriptionStats(
    val totalEvents: Int,
    val completedEvents: Int,
    val completionRate: Int
)
