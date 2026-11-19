package entity

interface VoipEngine {
    fun getSupportedProtocol(): VoipProtocolInfo? = null
    fun startCall(payload: VoipReadyPayload, onSendSignalingData: (ByteArray) -> Unit)
    fun receiveSignalingData(callId: Int, data: ByteArray)
    fun setMuted(muted: Boolean)
    fun stopCall()
}