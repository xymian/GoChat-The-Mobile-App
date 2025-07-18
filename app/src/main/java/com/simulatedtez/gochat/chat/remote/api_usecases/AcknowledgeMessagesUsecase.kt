package com.simulatedtez.gochat.chat.remote.api_usecases

import ChatServiceErrorResponse
import com.simulatedtez.gochat.chat.remote.api_services.IChatApiService
import com.simulatedtez.gochat.chat.remote.models.AckResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import utils.ChatEndpointCallerWithData
import utils.ResponseCallback

class AcknowledgeMessagesUsecase(
    private val chatInfo: ChatInfo,
    private val chatApiService: IChatApiService
): ChatEndpointCallerWithData<List<Message>, AckResponse> {

    override suspend fun call(data: List<Message>?, handler: ResponseCallback<AckResponse>) {
        data?.let { messages ->
            call(createAckParams(messages), handler)
        }
    }

    private suspend fun call(
        request: AckParams,
        chatServiceHandler: ResponseCallback<AckResponse>
    ) {
        val res = chatApiService.acknowledgeMessage(request)
        if (res is IResponse.Success) {
            chatServiceHandler.onResponse(AckResponse(
                data = res.data.data,
                isSuccessful = true,
                message = null
            ))
        } else {
            val error = (res as IResponse.Failure<ParentResponse<List<Message>>>)
            chatServiceHandler.onFailure(
                ChatServiceErrorResponse(
                    statusCode = error.response?.statusCode ?: HttpStatusCode.NotFound.value,
                    exception = error.exception,
                    reason = error.reason,
                    message = ""
                )
            )
        }
    }

    private fun createAckParams(messages: List<Message>): AckParams {
        val sortedList = messages.sortedBy { it.timestamp }
        return AckParams(
            request = AckParams.Request(
                from = sortedList.first().timestamp,
                to = sortedList.last().timestamp,
                chatReference = chatInfo.chatReference,
                yourUsername = chatInfo.username
            )
        )
    }
}

data class AckParams(
    val request: Request
) {

    @Serializable
    class Request(
        @SerialName("from")
        val from: String,
        @SerialName("to")
        val to: String,
        @SerialName("chatReference")
        val chatReference: String,
        @SerialName("username")
        val yourUsername: String,
    )
}