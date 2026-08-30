package com.ghostgram.core.tdlib

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

/**
 * 1. Описываем C-интерфейс TDLib (td_json_client)
 * Имена функций должны ТОЧНО совпадать с теми, что в C++ ядре.
 */
interface TdNative : Library {
    fun td_json_client_create(): Pointer?
    fun td_json_client_send(client: Pointer?, request: String)
    fun td_json_client_receive(client: Pointer?, timeout: Double): String?
    fun td_json_client_destroy(client: Pointer?)
}

actual class TelegramNativeClient actual constructor() {

    private var clientPtr: Pointer? = null
    private lateinit var tdApi: TdNative

    init {
        try {
            // 💥 Kоманда загрузки C++ бинарника!
            // На Windows он будет искать tdjson.dll, на Linux - libtdjson.so, на Mac - libtdjson.dylib
            tdApi = Native.load("tdjson", TdNative::class.java)
            clientPtr = tdApi.td_json_client_create()
            println("TDLib [JVM]: Ядро успешно загружено!")
        } catch (e: UnsatisfiedLinkError) {
            println("TDLib [JVM]: ОШИБКА! Бинарник tdjson не найден. ${e.message}")
        }
    }
    actual fun send(query: String) {
        clientPtr?.let { tdApi.td_json_client_send(it, query) }
    }

    actual fun receive(timeout: Double): String? {
        return clientPtr?.let { tdApi.td_json_client_receive(it, timeout) }
    }

    actual fun destroy() {
        clientPtr?.let {
            tdApi.td_json_client_destroy(it)
            clientPtr = null
        }
    }
}