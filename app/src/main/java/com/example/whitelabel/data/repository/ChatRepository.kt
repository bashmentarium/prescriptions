package com.example.whitelabel.data.repository

import com.example.whitelabel.data.database.AppDatabase
import com.example.whitelabel.data.database.entities.ChatConversationEntity
import com.example.whitelabel.data.database.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val database: AppDatabase) {
    
    private val conversationDao = database.chatConversationDao()
    private val messageDao = database.chatMessageDao()
    
    // Conversation operations
    fun getAllActiveConversations(): Flow<List<ChatConversationEntity>> {
        return conversationDao.getAllActiveConversations()
    }
    
    suspend fun getAllActiveConversationsSuspend(): List<ChatConversationEntity> {
        return conversationDao.getAllActiveConversationsSuspend()
    }
    
    suspend fun getConversationById(id: String): ChatConversationEntity? {
        return conversationDao.getConversationById(id)
    }
    
    suspend fun createConversation(title: String, prescriptionPreview: String = "New prescription - details will be added after processing"): String {
        val conversationId = java.util.UUID.randomUUID().toString()
        val conversation = ChatConversationEntity(
            id = conversationId,
            title = title,
            prescriptionPreview = prescriptionPreview,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        conversationDao.insertConversation(conversation)
        return conversationId
    }
    
    suspend fun updateConversation(conversation: ChatConversationEntity) {
        conversationDao.updateConversation(conversation)
    }
    
    suspend fun updateConversationTimestamp(conversationId: String) {
        conversationDao.updateConversationTimestamp(conversationId, System.currentTimeMillis())
    }
    
    suspend fun deleteConversation(conversationId: String) {
        // Delete all messages first
        messageDao.deleteMessagesByConversationId(conversationId)
        // Then delete the conversation
        conversationDao.deleteConversationById(conversationId)
    }
    
    // Message operations
    fun getMessagesByConversationId(conversationId: String): Flow<List<ChatMessageEntity>> {
        return messageDao.getMessagesByConversationId(conversationId)
    }
    
    suspend fun getMessagesByConversationIdSuspend(conversationId: String): List<ChatMessageEntity> {
        return messageDao.getMessagesByConversationIdSuspend(conversationId)
    }
    
    suspend fun addMessage(conversationId: String, fromUser: Boolean, text: String = "", imageUri: String? = null, isProcessing: Boolean = false): String {
        val messageId = java.util.UUID.randomUUID().toString()
        val message = ChatMessageEntity(
            id = messageId,
            conversationId = conversationId,
            fromUser = fromUser,
            text = text,
            imageUri = imageUri,
            isProcessing = isProcessing,
            createdAt = System.currentTimeMillis()
        )
        messageDao.insertMessage(message)
        
        // Update conversation timestamp
        updateConversationTimestamp(conversationId)
        
        return messageId
    }
    
    suspend fun updateMessage(message: ChatMessageEntity) {
        messageDao.updateMessage(message)
    }
    
    suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessageById(messageId)
    }
}
