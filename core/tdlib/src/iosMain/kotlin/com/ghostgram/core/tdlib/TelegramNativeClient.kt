package com.ghostgram.core.tdlib

actual class TelegramNativeClient actual constructor() {
    actual fun send(query: String) {
        println("TDLib [STUB]: Отправлен запрос: $query")
    }

    actual fun receive(timeout: Double): String? {
        // Заглушка. Когда подключим C++, здесь будет ожидание ответа от ядра.
        return null
    }

    actual fun destroy() {
        println("TDLib [STUB]: Клиент уничтожен")
    }
}