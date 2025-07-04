package com.simulatedtez.gochat.chat.database

import ILocalStorage
import com.simulatedtez.gochat.chat.remote.models.Message

interface IChatStorage: ILocalStorage<Message> {
    suspend fun loadNextPage(): List<Message>
    override suspend fun store(message: Message)
    override suspend fun store(messages: List<Message>)
}