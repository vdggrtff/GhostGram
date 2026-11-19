package entity

enum class CallStatus {
    OUTGOING_RINGING, // Идут гудки собеседнику
    INCOMING_RINGING, // Нам звонят (входящий вызов)
    CONNECTING,       // Обмен ключами шифрования (ExchangingKeys)
    ACTIVE,           // Разговор идет!
    ENDED             // Звонок завершен
}

data class ActiveCall(
    val callId: Int,
    val userId: Long,
    val userName: String,
    val userAvatar: String? = null,
    val isOutgoing: Boolean,
    val isVideo: Boolean = false,
    val status: CallStatus,
    val durationSeconds: Int = 0,
    val emojis: List<String> = emptyList(), // 💥 4 эмодзи защиты от прослушки
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false
)

sealed interface CallServerType {
    data class TelegramReflector(val peerTagBase64: String, val isTcp: Boolean) : CallServerType
    data class WebRtc(val username: String, val password: String, val supportsTurn: Boolean, val supportsStun: Boolean) : CallServerType
}

data class CallServer(
    val id: Long,
    val ipAddress: String,
    val ipv6Address: String,
    val port: Int,
    val type: CallServerType
)

data class VoipProtocolInfo(
    val minLayer: Int,
    val maxLayer: Int,
    val udpP2p: Boolean,
    val udpReflector: Boolean,
    val libraryVersions: List<String>
)

data class VoipReadyPayload(
    val callId: Int,
    val userId: Long,
    val isOutgoing: Boolean,
    val encryptionKeyBase64: String,
    val servers: List<CallServer>,
    val allowP2p: Boolean,
    val libraryVersions: List<String>,
    val customParameters: String = ""
)