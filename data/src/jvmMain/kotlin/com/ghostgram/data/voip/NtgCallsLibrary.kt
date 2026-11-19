package com.ghostgram.data.voip

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import java.io.File

// =============================================================================
// JNA СТРУКТУРЫ (Точно по ntgcalls.h, sizeof(C bool) = 1 byte -> Kotlin Byte!)
// =============================================================================

open class NtgLogMessage : Structure() {
    @JvmField var level: Int = 0
    @JvmField var source: Int = 0
    @JvmField var file: String? = null
    @JvmField var line: Int = 0
    @JvmField var message: String? = null

    class ByValue : NtgLogMessage(), Structure.ByValue
    override fun getFieldOrder() = listOf("level", "source", "file", "line", "message")
}

open class NtgConnectionInfo : Structure() {
    @JvmField var state: Int = 0
    @JvmField var kind: Int = 0

    class ByValue : NtgConnectionInfo(), Structure.ByValue
    override fun getFieldOrder() = listOf("state", "kind")
}

open class NtgAudioDescription : Structure() {
    @JvmField var media_source: Int = 4 // NTG_MEDIA_SOURCE_DEVICE
    @JvmField var sample_rate: Int = 48000
    @JvmField var channel_count: Byte = 1
    @JvmField var input: String? = null
    @JvmField var keep_open: Byte = 1

    override fun getFieldOrder() = listOf("media_source", "sample_rate", "channel_count", "input", "keep_open")
}

open class NtgMediaDescription : Structure(), Structure.ByValue {
    @JvmField var microphone: Pointer? = null
    @JvmField var speaker: Pointer? = null
    @JvmField var camera: Pointer? = null
    @JvmField var screen: Pointer? = null

    override fun getFieldOrder() = listOf("microphone", "speaker", "camera", "screen")
}

open class NtgDeviceInfo(p: Pointer? = null) : Structure(p) {
    @JvmField var name: String? = null
    @JvmField var metadata: String? = null

    init { if (p != null) read() }
    override fun getFieldOrder() = listOf("name", "metadata")
}

open class NtgMediaDevices : Structure() {
    @JvmField var microphone: Pointer? = null
    @JvmField var microphone_len: Long = 0L
    @JvmField var speaker: Pointer? = null
    @JvmField var speaker_len: Long = 0L
    @JvmField var camera: Pointer? = null
    @JvmField var camera_len: Long = 0L
    @JvmField var screen: Pointer? = null
    @JvmField var screen_len: Long = 0L

    override fun getFieldOrder() = listOf(
        "microphone", "microphone_len", "speaker", "speaker_len",
        "camera", "camera_len", "screen", "screen_len"
    )
}

open class NtgProtocol : Structure() {
    @JvmField var min_layer: Int = 0
    @JvmField var max_layer: Int = 0
    @JvmField var udp_p2p: Byte = 0
    @JvmField var udp_reflector: Byte = 0
    @JvmField var library_versions: Pointer? = null
    @JvmField var library_versions_len: Long = 0L

    override fun getFieldOrder() = listOf(
        "min_layer", "max_layer", "udp_p2p", "udp_reflector",
        "library_versions", "library_versions_len"
    )
}

open class NtgRtcServer : Structure() {
    @JvmField var id: Long = 0L
    @JvmField var ipv4: String? = null
    @JvmField var ipv6: String? = null
    @JvmField var port: Short = 0
    @JvmField var username: String? = null
    @JvmField var password: String? = null
    @JvmField var turn: Byte = 0
    @JvmField var stun: Byte = 0
    @JvmField var tcp: Byte = 0
    @JvmField var peer_tag: Pointer? = null
    @JvmField var peer_tag_len: Long = 0L

    override fun getFieldOrder() = listOf(
        "id", "ipv4", "ipv6", "port", "username", "password",
        "turn", "stun", "tcp", "peer_tag", "peer_tag_len"
    )
}

// =============================================================================
// JNA КОЛЛБЭКИ ИЗ C++ В KOTLIN
// =============================================================================

interface NtgLogCallback : Callback {
    fun invoke(message: NtgLogMessage.ByValue, userData: Pointer?)
}

interface NtgSignalingDataCallback : Callback {
    fun invoke(handle: Pointer?, chatId: Long, data: Pointer?, dataLen: Long, userData: Pointer?)
}

interface NtgConnectionChangeCallback : Callback {
    fun invoke(handle: Pointer?, chatId: Long, state: NtgConnectionInfo.ByValue, userData: Pointer?)
}

// =============================================================================
// C-ИНТЕРФЕЙС libntgcalls.so
// =============================================================================

interface NtgCallsLibrary : Library {
    fun ntg_set_log_callback(callback: NtgLogCallback?, userData: Pointer?)
    fun ntg_get_version(): String?
    fun ntg_last_error(): String?

    fun ntg_instance_create(): Pointer?
    fun ntg_instance_destroy(handle: Pointer)

    fun ntg_get_protocol(out: NtgProtocol): Int
    fun ntg_protocol_free(value: Pointer)

    fun ntg_get_media_devices(out: NtgMediaDevices): Int
    fun ntg_media_devices_free(value: Pointer)

    fun ntg_on_signaling_data_callback(handle: Pointer, callback: NtgSignalingDataCallback, userData: Pointer?): Int
    fun ntg_on_connection_change_callback(handle: Pointer, callback: NtgConnectionChangeCallback, userData: Pointer?): Int

    fun ntg_create_p2p_call(handle: Pointer, userId: Long): Int
    fun ntg_skip_exchange(handle: Pointer, userId: Long, encryptionKey: ByteArray, encryptionKeyLen: Long, isOutgoing: Byte): Int
    fun ntg_connect_p2p(
        handle: Pointer,
        userId: Long,
        servers: Pointer?,
        serversLen: Long,
        versions: Array<String>,
        versionsLen: Long,
        p2pAllowed: Byte,
        customParameters: String
    ): Int

    fun ntg_set_stream_sources(handle: Pointer, chatId: Long, mode: Int, media: NtgMediaDescription): Int
    fun ntg_send_signaling_data(handle: Pointer, chatId: Long, data: ByteArray, dataLen: Long): Int
    fun ntg_mute(handle: Pointer, chatId: Long, out: ByteArray): Int
    fun ntg_unmute(handle: Pointer, chatId: Long, out: ByteArray): Int
    fun ntg_stop(handle: Pointer, chatId: Long): Int

    companion object {
        fun loadSafe(): NtgCallsLibrary? {
            val candidates = listOf(
                File("libntgcalls.so"),
                File("desktopApp/libntgcalls.so"),
                File("lib/libntgcalls.so"),
                File("desktopApp/lib/libntgcalls.so")
            )
            return try {
                val found = candidates.firstOrNull { it.exists() }
                if (found != null) {
                    println("🔊 [NTGCALLS] Загружаем библиотеку: ${found.absolutePath}")
                    Native.load(found.absolutePath, NtgCallsLibrary::class.java)
                } else {
                    println("🔊 [NTGCALLS] Ищем ntgcalls в системных путях...")
                    Native.load("ntgcalls", NtgCallsLibrary::class.java)
                }
            } catch (e: Throwable) {
                println("❌ [NTGCALLS] Не удалось загрузить libntgcalls.so: ${e.message}")
                null
            }
        }
    }
}