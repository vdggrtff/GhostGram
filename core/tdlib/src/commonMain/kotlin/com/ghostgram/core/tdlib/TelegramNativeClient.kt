package com.ghostgram.core.tdlib

/**
 * Кроссплатформенный интерфейс для общения с C++ ядром TDLib.
 * Реализация (actual) будет написана под каждую платформу отдельно.
 */
expect class TelegramNativeClient() {

    /**
     * Отправляет асинхронный запрос в TDLib.
     * [query] - строка в формате JSON.
     */
    fun send(query: String)

    /**
     * Синхронно получает ответ от TDLib.
     * Должно вызываться в бесконечном цикле в отдельном потоке (Dispatcher.IO).
     * [timeout] - максимальное время ожидания ответа в секундах.
     */
    fun receive(timeout: Double): String?

    /**
     * Уничтожает инстанс C++ клиента при выходе из аккаунта.
     */
    fun destroy()
}