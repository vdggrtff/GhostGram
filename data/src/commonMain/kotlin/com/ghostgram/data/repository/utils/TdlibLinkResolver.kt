package com.ghostgram.data.repository.utils

import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.core.tdlib.sendAndAwait
import entity.ChatInvitePreview
import entity.InviteLinkResult
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class TdlibLinkResolver(
    private val tdlibClient: TelegramFlowClient
) {
    suspend fun resolveUsername(username: String): Long? {
        val clean = username
            .removePrefix("@")
            .substringAfterLast("t.me/")
            .substringAfterLast("domain=")
            .substringBefore("?")
            .substringBefore("/")
            .trim()

        if (clean.isBlank()) return null

        val response = tdlibClient.sendAndAwait(
            requestType = "searchPublicChat",
            parameters = mapOf("username" to clean),
            timeoutMs = 4000
        ) ?: return null

        return response["id"]?.jsonPrimitive?.longOrNull
    }

    suspend fun checkInvite(inviteLink: String): InviteLinkResult {
        val trimmed = inviteLink.trim()
        val fullLink = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("t.me/") -> "https://$trimmed"
            trimmed.startsWith("+") -> "https://t.me/$trimmed"
            else -> trimmed
        }

        val response = tdlibClient.sendAndAwait(
            requestType = "checkChatInviteLink",
            parameters = mapOf("invite_link" to fullLink),
            timeoutMs = 5000
        ) ?: return InviteLinkResult.Error("Сервер не ответил вовремя")

        if (response["@type"]?.jsonPrimitive?.content == "chatInviteLinkInfo") {
            val chatId = response["chat_id"]?.jsonPrimitive?.longOrNull ?: 0L
            val createsJoinRequest = response["creates_join_request"]?.jsonPrimitive?.booleanOrNull ?: false

            if (chatId != 0L && !createsJoinRequest) {
                return InviteLinkResult.AlreadyJoined(chatId)
            }

            val title = response["title"]?.jsonPrimitive?.content ?: "Приватный чат"
            val description = response["description"]?.jsonPrimitive?.content ?: ""
            val memberCount = response["member_count"]?.jsonPrimitive?.intOrNull ?: 0
            val isPublic = response["is_public"]?.jsonPrimitive?.booleanOrNull ?: false
            val typeObj = response["type"]?.jsonObject?.get("@type")?.jsonPrimitive?.content
            val isChannel = typeObj == "chatTypeSupergroup" && isPublic

            return InviteLinkResult.NeedsJoin(
                ChatInvitePreview(
                    inviteLink = fullLink,
                    title = title,
                    description = description,
                    memberCount = memberCount,
                    isChannel = isChannel,
                    createsJoinRequest = createsJoinRequest
                )
            )
        }

        return InviteLinkResult.Error("Ссылка-приглашение недействительна")
    }

    suspend fun joinChat(inviteLink: String): Long? {
        val response = tdlibClient.sendAndAwait(
            requestType = "joinChatByInviteLink",
            parameters = mapOf("invite_link" to inviteLink),
            timeoutMs = 8000
        )
        return response?.get("id")?.jsonPrimitive?.longOrNull
    }
}