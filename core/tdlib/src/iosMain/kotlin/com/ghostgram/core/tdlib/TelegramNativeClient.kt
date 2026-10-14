package com.ghostgram.core.tdlib

actual class TelegramNativeClient actual constructor() {
    actual fun send(query: String) {
        println("TDLib [STUB]: Send: $query")
    }

    actual fun receive(timeout: Double): String? {
        return null
    }

    actual fun destroy() {
        println("TDLib [STUB]: Client destroy")
    }
}