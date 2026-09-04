package com.ghostgram.core.tdlib

import android.util.Log
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface TdNativeAndroid : Library {
    fun td_json_client_create(): Pointer?
    fun td_json_client_send(client: Pointer?, request: String)
    fun td_json_client_receive(client: Pointer?, timeout: Double): String?
    fun td_json_client_destroy(client: Pointer?)
}


actual class TelegramNativeClient actual constructor() {

    private var clientPtr: Pointer? = null
    private var tdApi: TdNativeAndroid? = null

    init {
        try {
            // Android загрузит libtdjson.so из папки jniLibs
            tdApi = Native.load("tdjson", TdNativeAndroid::class.java)
            clientPtr = tdApi?.td_json_client_create()
            Log.d("GhostGram_TDLib", "🔥 C++ ядро TDLib успешно загружено в Android!")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("GhostGram_TDLib", "❌ Не удалось найти libtdjson.so в jniLibs: ${e.message}")
        }
    }

    actual fun send(query: String) {
        clientPtr?.let { tdApi?.td_json_client_send(it, query) }
    }

    actual fun receive(timeout: Double): String? {
        return clientPtr?.let { tdApi?.td_json_client_receive(it, timeout) }
    }

    actual fun destroy() {
        clientPtr?.let {
            tdApi?.td_json_client_destroy(it)
            clientPtr = null
        }
    }
}