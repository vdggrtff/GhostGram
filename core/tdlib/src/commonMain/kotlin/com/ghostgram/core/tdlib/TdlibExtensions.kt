package com.ghostgram.core.tdlib

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.random.Random
import kotlin.time.Clock.System
import kotlin.time.Duration.Companion.milliseconds

private val defaultJsonParser = Json { ignoreUnknownKeys = true }

/**
 * 💥 Extension-функция для TelegramFlowClient:
 * Отправляет любой запрос в TDLib и асинхронно ждет ответ по уникальному @extra
 */
suspend fun TelegramFlowClient.sendAndAwait(
    requestType: String,
    parameters: Map<String, Any> = emptyMap(),
    timeoutMs: Long = 4000
): JsonObject? {
    val extraId = "req_${System.now().toEpochMilliseconds()}_${Random.nextInt(1000, 9999)}"

    val requestJson = buildJsonObject {
        put("@type", requestType)
        put("@extra", extraId)
        parameters.forEach { (key, value) ->
            when (value) {
                is String -> put(key, value)
                is Number -> put(key, value)
                is Boolean -> put(key, value)
                is JsonElement -> put(key, value)
            }
        }
    }.toString()

    return withTimeoutOrNull(timeoutMs.milliseconds) {
        val deferred = async {
            updates
                .filter { it.contains(extraId) }
                .mapNotNull { raw ->
                    runCatching { defaultJsonParser.parseToJsonElement(raw).jsonObject }.getOrNull()
                }
                .first { it["@extra"]?.jsonPrimitive?.content == extraId }
        }

        send(requestJson)
        deferred.await()
    }
}