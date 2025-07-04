package com.simulatedtez.gochat.chat.models

import com.simulatedtez.gochat.chat.remote.models.Message

data class ChatPage(
    val messages: List<Message>,
    val paginationCount: Int,
    val size: Int,
)