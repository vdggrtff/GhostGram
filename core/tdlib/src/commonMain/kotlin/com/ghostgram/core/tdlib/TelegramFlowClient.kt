package com.ghostgram.core.tdlib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Реактивная обертка над нативным TDLib клиентом.
 * Превращает синхронный C++ поллинг в асинхронный SharedFlow.
 */
class TelegramFlowClient(
    private val nativeClient: TelegramNativeClient
) {
    // SharedFlow для трансляции обновлений от Telegram на весь проект
    private val _updates = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val updates: SharedFlow<String> = _updates.asSharedFlow()

    private var pollingJob: Job? = null

    /**
     * Запускает бесконечный цикл прослушивания C++ ядра в фоновом потоке.
     */
    fun startReceiving(scope: CoroutineScope) {
        if (pollingJob?.isActive == true) return

        pollingJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                // TDLib рекомендует таймаут около 1.0 - 10.0 секунд
                val jsonResponse = nativeClient.receive(1.0)

                if (!jsonResponse.isNullOrBlank()) {
                    _updates.emit(jsonResponse)
                }
            }
        }
    }

    /**
     * Отправляет JSON-запрос в ядро.
     */
    fun send(jsonQuery: String) {
        nativeClient.send(jsonQuery)
    }

    /**
     * Останавливает прослушивание и убивает клиент.
     */
    fun stop() {
        pollingJob?.cancel()
        nativeClient.destroy()
    }
}