package com.ghostgram.core.tdlib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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

    private val clientScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null

    init {
        // 💥 Автоматический старт при создании клиента!
        startReceiving()
    }

    /**
     * Устанавливает уровень логирования C++ ядра.
     * 0 - тишина, 1 - только фатальные ошибки, 2 - предупреждения, 5 - адский спам
     */
    fun setLogVerbosityLevel(level: Int = 1) {
        send("""{"@type": "setLogVerbosityLevel", "new_verbosity_level": $level}""")
    }

    /**
     * Запускает бесконечный цикл прослушивания C++ ядра в фоновом потоке.
     */
    fun startReceiving() {
        if (pollingJob?.isActive == true) return

        pollingJob = clientScope.launch {

            setLogVerbosityLevel(1)

            sendInitParameters()

            while (isActive) {
                // TDLib рекомендует таймаут около 1.0 - 10.0 секунд
                val jsonResponse = nativeClient.receive(1.0)

                if (!jsonResponse.isNullOrBlank()) {

                    if (jsonResponse.contains("updateAuthorizationState")) {
                        println("🔥🔥🔥 AUTH STATE: $jsonResponse")
                    } else if (jsonResponse.contains("error")) {
                        println("❌ TDLib ERROR: $jsonResponse")
                    }

                    _updates.emit(jsonResponse)
                }
            }
        }
    }

    /**
     * Отправляет системные параметры в TDLib.
     * Вызывается один раз при старте ядра.
     */
    fun sendInitParameters() {
        // ВАЖНО: api_id и api_hash нужно получить на https://my.telegram.org
        // Пока я вставил дефолтные тестовые (могут работать нестабильно, лучше сделай свои)
        val apiId = 2040
        val apiHash = "b18441a1ff607e10a989891a5462e627"
        val dbPath = "ghostgram_tdlib_data"

        val request = """
            {
                "@type": "setTdlibParameters",
                "database_directory": "$dbPath",
                "use_message_database": true,
                "use_secret_chats": true,
                "api_id": $apiId,
                "api_hash": "$apiHash",
                "system_language_code": "ru",
                "device_model": "Desktop",
                "system_version": "Linux",
                "application_version": "1.0.0"
            }
        """.trimIndent()

        send(request)
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