package com.ghostgram.data.voip

import com.sun.jna.Memory
import com.sun.jna.Pointer
import entity.CallServerType
import entity.VoipEngine
import entity.VoipProtocolInfo
import entity.VoipReadyPayload
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual fun createPlatformVoipEngine(): VoipEngine? = DesktopVoipEngine()

@OptIn(ExperimentalEncodingApi::class)
class DesktopVoipEngine : VoipEngine {

    private val lib: NtgCallsLibrary? = NtgCallsLibrary.loadSafe()
    private var instance: Pointer? = null

    private var activeUserId: Long = 0L
    private var activeCallId: Int = 0
    private var signalingCallback: ((ByteArray) -> Unit)? = null

    // Удерживаем ссылки на буферы и коллбэки, чтобы GC не удалил их!
    private val pinnedBuffers = mutableListOf<Any>()

    private val logCb = object : NtgLogCallback {
        override fun invoke(message: NtgLogMessage.ByValue, userData: Pointer?) {
            // Печатаем только warning/error от WebRTC, чтобы не засорять лог
            if (message.level >= 4) {
                println("🔊 [NTG LOG lvl=${message.level}] ${message.message}")
            }
        }
    }

    private val signalingCb = object : NtgSignalingDataCallback {
        override fun invoke(handle: Pointer?, chatId: Long, data: Pointer?, dataLen: Long, userData: Pointer?) {
            if (data == null || dataLen <= 0) return
            val bytes = data.getByteArray(0, dataLen.toInt())
            println("📤 [NTG -> TDLIB] Отправляем WebRTC-пакет: userId=$chatId bytes=${bytes.size}")
            signalingCallback?.invoke(bytes)
        }
    }

    private val connectionCb = object : NtgConnectionChangeCallback {
        override fun invoke(handle: Pointer?, chatId: Long, state: NtgConnectionInfo.ByValue, userData: Pointer?) {
            val stateName = when (state.state) {
                0 -> "CONNECTING"
                1 -> "CONNECTED 🎉"
                2 -> "FAILED ❌"
                3 -> "TIMEOUT ⏱️"
                4 -> "CLOSED"
                else -> "UNKNOWN(${state.state})"
            }
            println("🌐 [NTG CONNECTION] userId=$chatId -> $stateName")
        }
    }

    init {
        if (lib != null) {
            println("🔊 [NTGCALLS] Версия движка: ${lib.ntg_get_version()}")
            lib.ntg_set_log_callback(logCb, null)
            instance = lib.ntg_instance_create()?.also { handle ->
                lib.ntg_on_signaling_data_callback(handle, signalingCb, null)
                lib.ntg_on_connection_change_callback(handle, connectionCb, null)
            }
        }
    }

    override fun getSupportedProtocol(): VoipProtocolInfo? {
        val ntg = lib ?: return null
        val proto = NtgProtocol()
        val res = ntg.ntg_get_protocol(proto)
        if (res != 0) {
            println("⚠️ [NTG] ntg_get_protocol error=$res (${ntg.ntg_last_error()})")
            return null
        }
        proto.read()
        val versions = if (proto.library_versions != null && proto.library_versions_len > 0) {
            proto.library_versions!!.getStringArray(0, proto.library_versions_len.toInt()).toList()
        } else emptyList()

        val info = VoipProtocolInfo(
            minLayer = proto.min_layer,
            maxLayer = proto.max_layer,
            udpP2p = proto.udp_p2p != 0.toByte(),
            udpReflector = proto.udp_reflector != 0.toByte(),
            libraryVersions = versions
        )
        ntg.ntg_protocol_free(proto.pointer)
        println("🔊 [NTG PROTOCOL] layers=${info.minLayer}..${info.maxLayer} versions=${info.libraryVersions}")
        return info
    }

    override fun startCall(payload: VoipReadyPayload, onSendSignalingData: (ByteArray) -> Unit) {
        val ntg = lib ?: return
        val handle = instance ?: return

        stopCall()
        activeUserId = payload.userId
        activeCallId = payload.callId
        signalingCallback = onSendSignalingData
        pinnedBuffers.clear()

        println("🎙️ [NTG START] Инициализируем P2P звонок userId=$activeUserId callId=$activeCallId")

        // 1. Создаем P2P звонок в NTgCalls
        checkResult("create_p2p_call", ntg.ntg_create_p2p_call(handle, activeUserId))

        // 2. Ищем микрофон и динамик Linux
        val (micMeta, spkMeta) = findAudioDevices(ntg)
        println("🎙️ [NTG DEVICES] Mic='$micMeta' | Speaker='$spkMeta'")

        // 3. Подключаем микрофон (CAPTURE = 0)
        val micDesc = NtgAudioDescription().apply {
            media_source = 4 // NTG_MEDIA_SOURCE_DEVICE
            sample_rate = 48000
            channel_count = 1
            input = micMeta
            keep_open = 1
            write()
        }
        pinnedBuffers.add(micDesc)
        val captureMedia = NtgMediaDescription().apply {
            microphone = micDesc.pointer
            write()
        }
        checkResult("set_stream_sources(CAPTURE)", ntg.ntg_set_stream_sources(handle, activeUserId, 0, captureMedia))

        // 4. Подключаем динамик для входящего голоса (PLAYBACK = 1)
        // Важно: в NTgCalls 3.x входящий P2P звук идет через поле microphone у PLAYBACK!
        val spkDesc = NtgAudioDescription().apply {
            media_source = 4 // NTG_MEDIA_SOURCE_DEVICE
            sample_rate = 48000
            channel_count = 1
            input = spkMeta
            keep_open = 1
            write()
        }
        pinnedBuffers.add(spkDesc)
        val playbackMedia = NtgMediaDescription().apply {
            microphone = spkDesc.pointer
            write()
        }
        checkResult("set_stream_sources(PLAYBACK)", ntg.ntg_set_stream_sources(handle, activeUserId, 1, playbackMedia))

        // 5. Передаем готовый 256-байтный ключ от TDLib (skip_exchange)
        val keyBytes = Base64.decode(payload.encryptionKeyBase64)
        checkResult(
            "skip_exchange",
            ntg.ntg_skip_exchange(
                handle,
                activeUserId,
                keyBytes,
                keyBytes.size.toLong(),
                if (payload.isOutgoing) 1.toByte() else 0.toByte()
            )
        )

        // 6. Собираем непрерывный C-массив серверов (ntg_rtc_server[])
        val serverCount = payload.servers.size
        @Suppress("UNCHECKED_CAST")
        val serverArray = NtgRtcServer().toArray(serverCount) as Array<NtgRtcServer>
        pinnedBuffers.add(serverArray)

        payload.servers.forEachIndexed { idx, srv ->
            val s = serverArray[idx]
            s.id = srv.id
            s.ipv4 = srv.ipAddress
            s.ipv6 = srv.ipv6Address
            s.port = srv.port.toShort()

            when (val t = srv.type) {
                is CallServerType.TelegramReflector -> {
                    val tagBytes = Base64.decode(t.peerTagBase64)
                    val mem = Memory(tagBytes.size.toLong()).apply { write(0, tagBytes, 0, tagBytes.size) }
                    pinnedBuffers.add(mem)
                    s.peer_tag = mem
                    s.peer_tag_len = tagBytes.size.toLong()
                    s.tcp = if (t.isTcp) 1 else 0
                    s.turn = 0
                    s.stun = 0
                }
                is CallServerType.WebRtc -> {
                    s.username = t.username
                    s.password = t.password
                    s.turn = if (t.supportsTurn) 1 else 0
                    s.stun = if (t.supportsStun) 1 else 0
                    s.tcp = 0
                    s.peer_tag = null
                    s.peer_tag_len = 0
                }
            }
            s.write()
        }

        val versions = payload.libraryVersions.toTypedArray()
        val jsonParams = if (payload.customParameters.isNotBlank()) payload.customParameters else "{}"
        println("🔍 [NTG PARAMS] custom_parameters = '$jsonParams'")

        val serversPointer = if (serverCount > 0) serverArray[0].pointer else null

        checkResult(
            "connect_p2p",
            ntg.ntg_connect_p2p(
                handle,
                activeUserId,
                /*serverArray[0].pointer*/ serversPointer,
                serverCount.toLong(),
                versions,
                versions.size.toLong(),
                if (payload.allowP2p) 1.toByte() else 0.toByte(),
                //payload.customParameters,
                jsonParams
            )
        )
    }

    override fun receiveSignalingData(callId: Int, data: ByteArray) {
        val ntg = lib ?: return
        val handle = instance ?: return
        if (activeUserId == 0L) return
        val res = ntg.ntg_send_signaling_data(handle, activeUserId, data, data.size.toLong())
        if (res != 0) {
            println("⚠️ [NTG SIGNALING IN] error=$res (${ntg.ntg_last_error()})")
        }
    }

    override fun setMuted(muted: Boolean) {
        val ntg = lib ?: return
        val handle = instance ?: return
        if (activeUserId == 0L) return
        val out = ByteArray(1)
        if (muted) ntg.ntg_mute(handle, activeUserId, out) else ntg.ntg_unmute(handle, activeUserId, out)
    }

    override fun stopCall() {
        val ntg = lib ?: return
        val handle = instance ?: return
        if (activeUserId != 0L) {
            try {
                ntg.ntg_stop(handle, activeUserId)
            } catch (e: Throwable) {
                println("⚠️ [NTG STOP] ${e.message}")
            }
            activeUserId = 0L
            activeCallId = 0
            signalingCallback = null
            pinnedBuffers.clear()
        }
    }

    private fun findAudioDevices(ntg: NtgCallsLibrary): Pair<String, String> {
        val devices = NtgMediaDevices()
        val res = ntg.ntg_get_media_devices(devices)
        if (res != 0) {
            println("⚠️ [NTG DEVICES] Ошибка чтения устройств: ${ntg.ntg_last_error()}")
            return "" to ""
        }
        devices.read()
        val stride = NtgDeviceInfo().size().toLong()

        var micMeta = ""
        if (devices.microphone != null && devices.microphone_len > 0) {
            val firstMic = NtgDeviceInfo(devices.microphone!!.share(0))
            micMeta = firstMic.metadata ?: firstMic.name ?: ""
            println("🎤 Найдено микрофонов: ${devices.microphone_len}, выбран: '${firstMic.name}'")
        }

        var spkMeta = ""
        if (devices.speaker != null && devices.speaker_len > 0) {
            val firstSpk = NtgDeviceInfo(devices.speaker!!.share(0))
            spkMeta = firstSpk.metadata ?: firstSpk.name ?: ""
            println("🔊 Найдено динамиков: ${devices.speaker_len}, выбран: '${firstSpk.name}'")
        }

        ntg.ntg_media_devices_free(devices.pointer)
        return micMeta to spkMeta
    }

    private fun checkResult(step: String, res: Int) {
        if (res != 0) {
            println("❌ [NTG ERROR] $step вернул код $res: ${lib?.ntg_last_error()}")
        } else {
            println("✅ [NTG OK] $step")
        }
    }
}