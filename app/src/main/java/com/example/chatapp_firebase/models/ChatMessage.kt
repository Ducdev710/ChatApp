package com.example.chatapp_firebase.models

import java.util.Date

data class ChatMessage (
    var messageId: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var message: String = "",
    var dateTime: String = "",
    var dateObject: Date = Date(),
    var conversionId: String = "",
    var conversionName: String = "",
    var conversionImage: String = "",
    var senderName: String = "",
    var receiverName: String = "",
    var messageType: String = "",
    var isSeen: Boolean = false,
    var lastMessageSenderId: String = ""
)