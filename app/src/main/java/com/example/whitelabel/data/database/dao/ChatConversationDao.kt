package com.example.whitelabel.data.database.dao

import androidx.room.*
import com.example.whitelabel.data.database.entities.ChatConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatConversationDao {
    
    @Query("SELECT * FROM chat_conversations WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getAllActiveConversations(): Flow<List<ChatConversationEntity>>
    
    @Query("SELECT * FROM chat_conversations WHERE isActive = 1 ORDER BY updatedAt DESC")
    suspend fun getAllActiveConversationsSuspend(): List<ChatConversationEntity>
    
    @Query("SELECT * FROM chat_conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ChatConversationEntity?
    
    @Query("SELECT * FROM chat_conversations WHERE id = :id")
    fun getConversationByIdFlow(id: String): Flow<ChatConversationEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ChatConversationEntity)
    
    @Update
    suspend fun updateConversation(conversation: ChatConversationEntity)
    
    @Query("UPDATE chat_conversations SET isActive = 0 WHERE id = :id")
    suspend fun deactivateConversation(id: String)
    
    @Delete
    suspend fun deleteConversation(conversation: ChatConversationEntity)
    
    @Query("DELETE FROM chat_conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)
    
    @Query("UPDATE chat_conversations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateConversationTimestamp(id: String, updatedAt: Long)
}
