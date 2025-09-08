package com.example.whitelabel.data

import android.net.Uri
import com.example.whitelabel.data.database.entities.ChatConversationEntity
import com.example.whitelabel.data.database.entities.ChatMessageEntity
import com.example.whitelabel.ui.screen.ChatMessage
import com.example.whitelabel.ui.screen.CourseItem

// Extension functions to convert between UI data classes and database entities

fun ChatConversationEntity.toCourseItem(): CourseItem {
    return CourseItem(
        name = this.title,
        prescriptionPreview = this.prescriptionPreview
    )
}

fun CourseItem.toChatConversationEntity(): ChatConversationEntity {
    return ChatConversationEntity(
        id = java.util.UUID.randomUUID().toString(),
        title = this.name,
        prescriptionPreview = this.prescriptionPreview,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

fun ChatMessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(
        fromUser = this.fromUser,
        text = this.text,
        image = this.imageUri?.let { Uri.parse(it) },
        isProcessing = this.isProcessing
    )
}

fun ChatMessage.toChatMessageEntity(conversationId: String): ChatMessageEntity {
    return ChatMessageEntity(
        id = java.util.UUID.randomUUID().toString(),
        conversationId = conversationId,
        fromUser = this.fromUser,
        text = this.text,
        imageUri = this.image?.toString(),
        isProcessing = this.isProcessing,
        createdAt = System.currentTimeMillis()
    )
}
