package usecase

import entity.Message
import kotlinx.coroutines.flow.firstOrNull
import repository.ChatRepository
import repository.AiRepository
import kotlin.collections.emptyList

class SearchMessagesSemanticUseCase(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(
        chatId: Long,
        query: String,
        chatRepository: ChatRepository
    ): List<Message> {
        if (query.isBlank()) return emptyList()

        // 💥 1. Берем сообщения прямо через чистый интерфейс репозитория (уже готовые Message!)
        val messages = chatRepository.observeMessages(chatId).firstOrNull() ?: emptyList()
        if (messages.isEmpty()) return emptyList()

        // 💥 2. Форматируем историю для Gemini с ID каждого сообщения
        val formattedHistory = messages.takeLast(100).joinToString("\n") {
            "[ID: ${it.id}] ${it.senderName}: ${it.text}"
        }

        // 💥 3. Спрашиваем у Gemini подходящие ID сообщений
        val matchingIds = aiRepository.findSemanticMatches(query, formattedHistory)
            .getOrDefault(emptyList())

        // 💥 4. Достаем найденные сообщения по ID в порядке релевантности
        return matchingIds.mapNotNull { id ->
            messages.find { it.id == id }
        }
    }
}