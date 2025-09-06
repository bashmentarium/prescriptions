package com.example.whitelabel.data.database.dao

import androidx.room.*
import com.example.whitelabel.data.database.entities.PrescriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionDao {
    
    @Query("SELECT * FROM prescriptions WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActivePrescriptions(): Flow<List<PrescriptionEntity>>
    
    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getPrescriptionById(id: String): PrescriptionEntity?
    
    @Query("SELECT * FROM prescriptions WHERE id = :id")
    fun getPrescriptionByIdFlow(id: String): Flow<PrescriptionEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: PrescriptionEntity)
    
    @Update
    suspend fun updatePrescription(prescription: PrescriptionEntity)
    
    @Query("UPDATE prescriptions SET isActive = 0 WHERE id = :id")
    suspend fun deactivatePrescription(id: String)
    
    @Delete
    suspend fun deletePrescription(prescription: PrescriptionEntity)
    
    @Query("DELETE FROM prescriptions WHERE id = :id")
    suspend fun deletePrescriptionById(id: String)
    
    @Query("SELECT COUNT(*) FROM prescriptions WHERE isActive = 1")
    suspend fun getActivePrescriptionCount(): Int
}
