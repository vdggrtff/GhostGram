package com.ghostgram.data.repository.handlers

import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.data.repository.utils.UserMemoryCache
import entity.ActiveCall
import entity.CallServer
import entity.CallServerType
import entity.CallStatus
import entity.VoipEngine
import entity.VoipReadyPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import repository.CallRepository
import kotlin.io.encoding.Base64

class CallUpdateHandler(
    private val tdlibClient: TelegramFlowClient,
    private val userCache: UserMemoryCache,
    private val scope: CoroutineScope,
    private val voipEngine: VoipEngine? = null
) : TdlibUpdateHandler, CallRepository {

    private val _currentCall = MutableStateFlow<ActiveCall?>(null)
    override val currentCall = _currentCall.asStateFlow()

    private var durationTimerJob: Job? = null

    private val ntgProto = voipEngine?.getSupportedProtocol()
    private val libraryVersions = ntgProto?.libraryVersions?.takeIf { it.isNotEmpty() }
        ?: listOf("9.0.0", "5.0.0", "4.0.2", "4.0.0", "3.0.0", "2.4.4")

    private val protocolJson: String = run {
        val versionsJson = libraryVersions.joinToString(",") { "\"$it\"" }
        val minL = ntgProto?.minLayer ?: 65
        val maxL = ntgProto?.maxLayer ?: 92
        """
    {
        "@type": "callProtocol",
        "udp_p2p": true,
        "udp_reflector": true,
        "min_layer": $minL,
        "max_layer": $maxL,
        "library_versions": [$versionsJson]
    }
    """.trimIndent()
    }

    override fun startCall(userId: Long, isVideo: Boolean) {
        val user = userCache.getUser(userId)
        val name = user?.name ?: "Собеседник"
        val avatar = user?.avatarPath

        println("📞 [CALL] Инициируем вызов пользователю $userId ($name)...")

        _currentCall.value = ActiveCall(
            callId = 0,
            userId = userId,
            userName = name,
            userAvatar = avatar,
            isOutgoing = true,
            isVideo = isVideo,
            status = CallStatus.OUTGOING_RINGING
        )

        tdlibClient.send("""
            {
                "@type": "createCall",
                "user_id": $userId,
                "protocol": $protocolJson,
                "is_video": $isVideo
            }
        """.trimIndent())
        // 💥 Лишний tdlibClient.send(protocolJson) удален!
    }

    override fun acceptCall(callId: Int) {
        println("📞 [CALL] Принимаем вызов callId=$callId...")
        tdlibClient.send("""
            {
                "@type": "acceptCall",
                "call_id": $callId,
                "protocol": $protocolJson
            }
        """.trimIndent())
    }

    override fun discardCall(callId: Int) {
        println("📞 [CALL] Завершаем вызов callId=$callId...")
        durationTimerJob?.cancel()
        val duration = _currentCall.value?.durationSeconds ?: 0

        tdlibClient.send("""
            {
                "@type": "discardCall",
                "call_id": $callId,
                "is_disconnected": false,
                "duration": $duration,
                "is_video": false,
                "connection_id": 0
            }
        """.trimIndent())

        voipEngine?.stopCall()
        _currentCall.value = null
    }

    private fun sendSignalingData(callId: Int, data: ByteArray) {
        val current = _currentCall.value ?: return
        if (current.callId != callId || current.status == CallStatus.ENDED) return

        val base64 = Base64.encode(data)
        tdlibClient.send("""
            {
                "@type": "sendCallSignalingData",
                "call_id": $callId,
                "data": "$base64"
            }
        """.trimIndent())
    }

    override fun toggleMute() {
        _currentCall.update { current ->
            if (current == null) return@update null
            val nextMuted = !current.isMuted
            voipEngine?.setMuted(nextMuted)
            current.copy(isMuted = nextMuted)
        }
    }

    override fun toggleSpeaker() {
        _currentCall.update { it?.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    override fun handle(type: String, jsonObject: JsonObject): Boolean {
        if (type == "updateNewCallSignalingData") {
            val callId = jsonObject["call_id"]?.jsonPrimitive?.intOrNull ?: return true
            val dataBase64 = jsonObject["data"]?.jsonPrimitive?.content ?: return true
            val bytes = Base64.decode(dataBase64)
            println("📡 [WEBRTC] Входящий пакет от собеседника: callId=$callId, bytes=${bytes.size}")
            voipEngine?.receiveSignalingData(callId, bytes)
            return true
        }
        if (type != "updateCall") return false

        val callObj = jsonObject["call"]?.jsonObject ?: return true
        val callId = callObj["id"]?.jsonPrimitive?.intOrNull ?: return true
        val userId = callObj["user_id"]?.jsonPrimitive?.longOrNull ?: return true
        val isOutgoing = callObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false
        val stateObj = callObj["state"]?.jsonObject ?: return true
        val stateType = stateObj["@type"]?.jsonPrimitive?.content ?: ""

        println("📞 [TDLIB CALL STATE] callId=$callId state=$stateType")

        val user = userCache.getUser(userId)
        val name = user?.name ?: "Собеседник"
        val avatar = user?.avatarPath

        when (stateType) {
            "callStatePending" -> {
                _currentCall.update {
                    it?.copy(callId = callId, status = if (isOutgoing) CallStatus.OUTGOING_RINGING else CallStatus.INCOMING_RINGING)
                        ?: ActiveCall(callId, userId, name, avatar, isOutgoing, status = if (isOutgoing) CallStatus.OUTGOING_RINGING else CallStatus.INCOMING_RINGING)
                }
            }

            "callStateExchangingKeys" -> {
                _currentCall.update { it?.copy(callId = callId, status = CallStatus.CONNECTING) }
            }

            "callStateReady" -> {
                val emojisList = stateObj["emojis"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                val encryptionKey = stateObj["encryption_key"]?.jsonPrimitive?.content ?: ""
                val allowP2p = stateObj["allow_p2p"]?.jsonPrimitive?.booleanOrNull ?: true
                val serversArray = stateObj["servers"]?.jsonArray
                //val customParameters = stateObj["custom_parameters"]?.jsonPrimitive?.content ?: ""
                val customParameters = stateObj["custom_parameters"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: "{}"

                val negotiatedVersions = stateObj["protocol"]?.jsonObject
                    ?.get("library_versions")?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.content }
                    ?.takeIf { it.isNotEmpty() }
                    ?: libraryVersions

                println("🎉 [CALL READY] Эмодзи: $emojisList | Согласованные версии: $negotiatedVersions | Серверов: ${serversArray?.size ?: 0}")

                // 💥 РЕАЛЬНЫЙ ПАРСИНГ СЕРВЕРОВ ИЗ TDLIB:
                val parsedServers = serversArray?.mapNotNull { elem ->
                    val srv = elem.jsonObject
                    val id = srv["id"]?.jsonPrimitive?.longOrNull ?: 0L
                    val ip = srv["ip_address"]?.jsonPrimitive?.content ?: ""
                    val ipv6 = srv["ipv6_address"]?.jsonPrimitive?.content ?: ""
                    val port = srv["port"]?.jsonPrimitive?.intOrNull ?: 0
                    val srvTypeObj = srv["type"]?.jsonObject ?: return@mapNotNull null
                    val srvTypeName = srvTypeObj["@type"]?.jsonPrimitive?.content ?: ""

                    val callServerType = when (srvTypeName) {
                        "callServerTypeTelegramReflector" -> CallServerType.TelegramReflector(
                            peerTagBase64 = srvTypeObj["peer_tag"]?.jsonPrimitive?.content ?: "",
                            isTcp = srvTypeObj["is_tcp"]?.jsonPrimitive?.booleanOrNull ?: false
                        )
                        "callServerTypeWebrtc" -> CallServerType.WebRtc(
                            username = srvTypeObj["username"]?.jsonPrimitive?.content ?: "",
                            password = srvTypeObj["password"]?.jsonPrimitive?.content ?: "",
                            supportsTurn = srvTypeObj["supports_turn"]?.jsonPrimitive?.booleanOrNull ?: false,
                            supportsStun = srvTypeObj["supports_stun"]?.jsonPrimitive?.booleanOrNull ?: false
                        )
                        else -> return@mapNotNull null
                    }
                    CallServer(id, ip, ipv6, port, callServerType)
                } ?: emptyList()

                val readyPayload = VoipReadyPayload(
                    callId = callId,
                    userId = userId,
                    isOutgoing = isOutgoing,
                    encryptionKeyBase64 = encryptionKey,
                    servers = parsedServers,
                    allowP2p = allowP2p,
                    libraryVersions = negotiatedVersions,
                    customParameters = customParameters
                )

                println("🎉 [CALL READY] Эмодзи: $emojisList | Серверов: ${parsedServers.size} | Отправляем в VoipEngine=${voipEngine != null}")

                // 💥 ЗАПУСКАЕМ РЕАЛЬНЫЙ МЕДИА-ДВИЖОК!
                voipEngine?.startCall(readyPayload) { bytes -> sendSignalingData(callId, bytes) }

                _currentCall.update {
                    it?.copy(callId = callId, status = CallStatus.ACTIVE, emojis = emojisList)
                }

                durationTimerJob?.cancel()
                durationTimerJob = scope.launch {
                    while (isActive) {
                        delay(1000)
                        _currentCall.update { it?.copy(durationSeconds = it.durationSeconds + 1) }
                    }
                }
            }

            "callStateError" -> {
                val errObj = stateObj["error"]?.jsonObject
                println("❌ [CALL ERROR] code=${errObj?.get("code")} msg=${errObj?.get("message")}")
                durationTimerJob?.cancel()
                voipEngine?.stopCall()
                _currentCall.update { it?.copy(status = CallStatus.ENDED) }
                scope.launch {
                    delay(1500)
                    _currentCall.value = null
                }
            }

            "callStateDiscarded" -> {
                val reason = stateObj["reason"]?.jsonObject?.get("@type")?.jsonPrimitive?.content ?: ""
                println("📞 [CALL DISCARDED] Причина: $reason")
                durationTimerJob?.cancel()
                voipEngine?.stopCall()
                _currentCall.update { it?.copy(status = CallStatus.ENDED) }
                scope.launch {
                    delay(1200)
                    _currentCall.value = null
                }
            }
        }
        return true
    }
}