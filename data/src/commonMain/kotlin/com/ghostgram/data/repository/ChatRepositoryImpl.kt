package com.ghostgram.data.repository

import com.ghostgram.core.tdlib.TelegramFlowClient
import entity.Chat
import entity.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import repository.ChatRepository

class ChatRepositoryImpl(
    private val tdlibClient: TelegramFlowClient
) : ChatRepository {

    /*init {
        // Запускаем прослушивание ядра C++ (потребуется GlobalScope или кастомный scope для синглтона)
        tdlibClient.startReceiving(kotlinx.coroutines.GlobalScope)
    }*/


    override fun observeChats(): Flow<List<Chat>> {
        // Запрос к TDLib на загрузку списка чатов
        tdlibClient.send("""{"@type": "loadChats", "chat_list": {"@type": "chatListMain"}, "limit": 20}""")

        // Пока нет парсера JSON от TDLib, возвращаем фейковые данные для теста UI
        return flow {
            emit(
                listOf(
                    Chat(
                        id = 1,
                        title = "GhostGRAM Team 👻",
                        unreadCount = 5,
                        lastMessage = Message(1, 1, "Алексей", "Ну что, когда выкатываем бету?")
                    )
                )
            )
        }
    }

    override suspend fun getChatHistory(chatId: Long, limit: Int): String {
        // Реальный запрос в TDLib выглядит так:
        val requestJson = """
            {
                "@type": "getChatHistory",
                "chat_id": $chatId,
                "from_message_id": 0,
                "offset": 0,
                "limit": $limit,
                "only_local": false
            }
        """.trimIndent()

        tdlibClient.send(requestJson)

        // Временно возвращаем мок-историю для тестирования Gemini AI,
        // пока не настроим перехват ответа из tdlibClient.updates
        return """
            Алексей: Привет! Как там фича с AI?
            Тимлид: Ку, бро! Модуль data почти готов.
            Алексей: Отлично, я тогда настраиваю Koin.
            Тимлид: Давай, и переходим к UI на Compose!
        """.trimIndent()
    }
}
