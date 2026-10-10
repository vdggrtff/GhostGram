package com.ghostgram.core.tdlib

import com.ghostgram.core.tdlib.di.TdlibConfig
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
    private val nativeClient: TelegramNativeClient,
    private val config: TdlibConfig
) {
    private val _updates = MutableSharedFlow<String>(
        replay = 50,
        extraBufferCapacity = 100
    )
    val updates: SharedFlow<String> = _updates.asSharedFlow()

    private val clientScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null

    init {
        startReceiving()
    }

    fun setLogVerbosityLevel(level: Int = 1) {
        send("""{"@type": "setLogVerbosityLevel", "new_verbosity_level": $level}""")
    }

    fun startReceiving() {
        if (pollingJob?.isActive == true) return

        pollingJob = clientScope.launch {
            setLogVerbosityLevel(1)

            // Отправляем первичные параметры
            sendInitParameters()

            while (isActive) {
                val jsonResponse = nativeClient.receive(1.0)

                if (!jsonResponse.isNullOrBlank()) {
                    // 💥 ЛОГИРУЕМ ЧЕРЕЗ СИСТЕМНЫЙ ЛОГГЕР (ВИДНО ДАЖЕ В РЕЛИЗЕ!)
                    try {
                        if (jsonResponse.contains("updateAuthorizationState")) {
                            //Log.e("GHOST_TDLIB", "🔥🔥🔥 AUTH STATE: $jsonResponse")
                        } else if (jsonResponse.contains("error")) {
                            //Log.e("GHOST_TDLIB", "❌ TDLib ERROR: $jsonResponse")
                        }
                    } catch (e: Throwable) {
                        // Для Desktop, где нет android.util.Log
                        println(jsonResponse)
                    }

                    // 💥 1. ЕСЛИ ТЕЛЕГРАМ ПРОСИТ ПАРАМЕТРЫ — ОТПРАВЛЯЕМ!
                    if (jsonResponse.contains("authorizationStateWaitTdlibParameters")) {
                        sendInitParameters()
                    }

                    // 💥 2. ТОТ САМЫЙ ПРОПУЩЕННЫЙ ШАГ: РАЗБЛОКИРУЕМ БАЗУ ДАННЫХ!
                    if (jsonResponse.contains("authorizationStateWaitEncryptionKey")) {
                        send("""{"@type": "checkDatabaseEncryptionKey"}""")
                    }

                    _updates.emit(jsonResponse)
                }
            }
        }
    }

    private fun sendInitParameters() {
        val apiId = 2040
        val apiHash = "b18441a1ff607e10a989891a5462e627"
        val dbPath = config.databasePath

        val request = """
            {
                "@type": "setTdlibParameters",
                "database_directory": "$dbPath",
                "use_message_database": true,
                "use_secret_chats": true,
                "api_id": $apiId,
                "api_hash": "$apiHash",
                "system_language_code": "ru",
                "device_model": "GhostGRAM Mobile",
                "system_version": "Android",
                "application_version": "1.0.0"
            }
        """.trimIndent()
        send(request)
    }

    fun send(jsonQuery: String) {
        nativeClient.send(jsonQuery)
    }

    fun stop() {
        pollingJob?.cancel()
        nativeClient.destroy()
    }
}