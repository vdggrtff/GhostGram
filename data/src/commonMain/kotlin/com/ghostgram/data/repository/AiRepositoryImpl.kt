package com.ghostgram.data.repository

import api.GeminiApiClient
import entity.LocalSettingsManager
import repository.AiRepository

class AiRepositoryImpl(
    private val geminiClient: GeminiApiClient
) : AiRepository {

    override suspend fun getChatSummary(chatHistory: String): Result<String> {
        return try {
            // Формируем промпт для нейросети
            val prompt = """
                You are an advanced AI assistant in GhostGRAM.
                Your task is to summarize the unread messages in this chat for the user so they can catch up instantly.

                CRITICAL RULES:
                1. Capture ALL key topics, events, questions, and decisions discussed across the ENTIRE history, not just the last message.
                2. If something unusual, funny, or out of the ordinary happened (e.g., someone shaved an animal, made a weird joke, shared news), you MUST mention it.
                3. If a problem was discussed and then resolved (e.g., a broken keyboard that got fixed), state both the issue AND its final resolution.
                4. Structure the summary with bullet points using relevant emojis.
                5. Respond STRICTLY in the same language as the chat messages. Be concise, informative, and engaging.

                Chat messages:
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

    override suspend fun getSmartReplies(chatHistory: String): Result<List<String>> {
        return try {
            val prompt = """
                You are an AI assistant built into the GhostGRAM messenger.
                Read the recent chat history and generate exactly 3 short, natural, and contextually appropriate reply options for the current user.
                Output ONLY the 3 options, separated by the '|' character. Do not use numbering or extra words.
                The replies MUST be in the same language as the chat history.
                Example format: Yes, sure!|I can't today.|Let's discuss it later.
                
                Chat history:
                $chatHistory
            """.trimIndent()

            val response = geminiClient.generateText(prompt = prompt)
            if (response == "ERROR") return Result.failure(Exception("Gemini Error"))

            // Парсим ответ по разделителю "|"
            val replies = response.split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(3)

            Result.success(replies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findSemanticMatches(query: String, chatHistory: String): Result<List<Long>> {
        return try {
            val prompt = """
                You are a smart semantic search engine in Telegram client GhostGRAM.
                User search query: "$query"
                
                Chat history:
                $chatHistory
                
                Task:
                Find all messages from the history that match the meaning, context, slang, synonyms, or intent of the user query.
                For example, if query is "клава", match messages containing "клавиатура".
                If query is "встреча", match messages where people agree on time/place.
                
                CRITICAL INSTRUCTION:
                Return ONLY a JSON array of message IDs (Long numbers) in order of relevance, for example: [1048576, 2097152].
                If no messages match, return [].
                DO NOT add any markdown formatting, no explanations, no backticks. Only raw JSON array like [123, 456].
            """.trimIndent()

            val response = geminiClient.generateText(prompt)

            // 💥 Очищаем ответ от возможных пробелов и бэктиков
            val cleanJson = response.replace("```json", "").replace("```", "").trim()

            // Достаем все числа из ответа через регулярку (железобетонный парсинг!)
            val ids = Regex("\\d+").findAll(cleanJson).mapNotNull { it.value.toLongOrNull() }.toList()

            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}