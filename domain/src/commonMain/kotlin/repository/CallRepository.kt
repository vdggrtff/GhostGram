package repository

import entity.ActiveCall
import kotlinx.coroutines.flow.StateFlow

interface CallRepository {
    val currentCall: StateFlow<ActiveCall?>

    fun startCall(userId: Long, isVideo: Boolean = false)
    fun acceptCall(callId: Int)
    fun discardCall(callId: Int)
    fun toggleMute()
    fun toggleSpeaker()
}