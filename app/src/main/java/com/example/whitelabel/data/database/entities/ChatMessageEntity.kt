package com.example.whitelabel.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val conversationId: String,
    val fromUser: Boolean,
    val text: String = "",
    val imageUri: String? = null, // Store URI as string
    val isProcessing: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
