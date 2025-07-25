package com.simulatedtez.gochat.conversations.repository

import com.simulatedtez.gochat.conversations.ConversationDatabase
import com.simulatedtez.gochat.conversations.Conversation_db
import com.simulatedtez.gochat.conversations.models.Conversation
import com.simulatedtez.gochat.conversations.remote.api_usecases.StartNewChatParams
import com.simulatedtez.gochat.conversations.remote.api_usecases.StartNewChatUsecase
import com.simulatedtez.gochat.conversations.remote.models.NewChatResponse
import com.simulatedtez.gochat.conversations.toConversations
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ConversationsRepository(
    private val startNewChatUsecase: StartNewChatUsecase,
    private val conversationDB: ConversationDatabase
) {
    private var conversationsListener: ConversationsListener? = null

    private val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    operator fun invoke(listener: ConversationsListener) {
        conversationsListener = listener
    }

    fun setListener(listener: ConversationsListener) {
        conversationsListener = listener
    }

    suspend fun getConversations(): List<Conversation> {
        return conversationDB.getConversations().toConversations()
    }

    suspend fun addNewChat(username: String, otherUser: String) {
        val params = StartNewChatParams(
            request = StartNewChatParams.Request(
                user = username, other = otherUser
            )
        )
        startNewChatUsecase.call(
            params, object: IResponseHandler<ParentResponse<NewChatResponse>, IResponse<ParentResponse<NewChatResponse>>> {
            override fun onResponse(response: IResponse<ParentResponse<NewChatResponse>>) {
               when (response) {
                   is IResponse.Success -> {
                       response.data.data?.let {
                           context.launch(Dispatchers.IO) {
                               conversationDB.insertConversation(
                                   Conversation_db(
                                       otherUser = it.other,
                                       chatReference = it.chatReference,
                                       lastMessage = "",
                                       timestamp = "",
                                       unreadCount = 0,
                                       contactAvi = ""
                                   )
                               )
                           }
                           context.launch(Dispatchers.Main) {
                               conversationsListener?.onNewChatAdded(it)
                           }
                       }
                   }
                   is IResponse.Failure -> {
                       context.launch(Dispatchers.Main) {
                           conversationsListener?.onAddNewChatFailed(response)
                       }
                   }

                   is Response<*> -> {}
               }
            }
        })
    }

    fun cancel() {
        context.cancel()
    }
}

interface ConversationsListener {
    fun onAddNewChatFailed(error: IResponse.Failure<ParentResponse<NewChatResponse>>)
    fun onNewChatAdded(chat: NewChatResponse)
}