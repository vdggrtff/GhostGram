package com.ghostgram.data.repository

import api.GeminiApiClient
import repository.AiRepository

class AiRepositoryImpl(
    private val geminiClient: GeminiApiClient,
) : AiRepository {

    override suspend fun getChatSummary(chatHistory: String): Result<String> {
        return try {
            // Формируем промпт для нейросети
            val prompt = """
                You are a smart AI assistant embedded in the GhostGram Telegram client.
                Read the provided chat history and generate a concise, highly informative summary of the most important points.
                Do not use unnecessary words, focus purely on facts and key decisions.
                The summary MUST be in the same language as the chat history.

                Chat history:
                $chatHistory
            """.trimIndent()

            // Дергаем метод из твоего клиента, который мы перенесли в core:network
            val response = geminiClient.generateText(prompt = prompt)

            if (response == "ERROR") {
                Result.failure(Exception("Gemini API вернул ошибку"))
            } else {
                Result.success(response)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}