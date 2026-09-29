package api

import com.ghostgram.core.network.BuildConfig
import entity.LocalSettingsManager
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GeminiApiClient(
    private val httpClient: HttpClient,
    private val settingsManager: LocalSettingsManager
) {

    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    suspend fun generateText(prompt: String): String {
        // ДОСТАЕМ ЛИЧНЫЙ КЛЮЧ ЮЗЕРА
        val personalKey = settingsManager.getGeminiKey().trim()

        println("🤖 [GEMINI CLIENT] Старт генерации! Длина промпта: ${prompt.length} символов")
        println("🔑 [GEMINI CLIENT] Личный ключ: ${if (personalKey.isNotBlank()) "ЕСТЬ (${personalKey.take(6)}...)" else "НЕТ (пусто)"}")

        return try {
            if (personalKey.isNotBlank()) {
                println("🚀 [GEMINI CLIENT] Отправляем запрос НАПРЯМУЮ в Google Gemini API...")
                // 🚀 ПУТЬ 1: ПРЯМОЙ ЗАПРОС В GOOGLE (BYOK)
                val response = httpClient.post("$baseUrl/gemini-3.1-flash-lite-preview:generateContent?key=$personalKey") {
                    contentType(ContentType.Application.Json)
                    val bodyStr = """{"contents": [{"parts": [{"text": ${Json.encodeToString(kotlinx.serialization.serializer(), prompt)}}]}]}"""
                    setBody(bodyStr)
                }

                val status = response.status.value
                val rawBody = response.bodyAsText()
                println("📡 [GOOGLE API] HTTP Status: $status")
                println("📡 [GOOGLE API] Ответ сервера: $rawBody")

                if (status != 200) {
                    println("❌ [GOOGLE API] Ошибка: Сервер вернул код $status")
                    return "ERROR"
                }

                val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(response.bodyAsText()).jsonObject
                json["candidates"]?.jsonArray?.get(0)?.jsonObject
                    ?.get("content")?.jsonObject
                    ?.get("parts")?.jsonArray?.get(0)?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content ?: "ERROR"

            } else {
                println("🛡️ [GEMINI CLIENT] Личного ключа нет. Отправляем через Supabase Прокси...")
                // 🛡️ ПУТЬ 2: ЗАПРОС ЧЕРЕЗ SUPABASE (Прокси)
                val response = httpClient.post("$supabaseUrl/functions/v1/ghost-ai") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $supabaseAnonKey")
                    // Отправляем на наш сервер только текст!
                    val bodyStr = """{"prompt": ${Json.encodeToString(kotlinx.serialization.serializer(), prompt)}}"""
                    setBody(bodyStr)
                }

                val status = response.status.value
                val rawBody = response.bodyAsText()
                println("📡 [SUPABASE] HTTP Status: $status")
                println("📡 [SUPABASE] Ответ сервера: $rawBody")

                val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(response.bodyAsText()).jsonObject
                json["result"]?.jsonPrimitive?.content ?: "ERROR"
            }
        } catch (e: Exception) {
            println("❌ [GEMINI CLIENT] ИСКЛЮЧЕНИЕ / КРАШ: ${e.message}")
            e.printStackTrace()
            "ERROR"
        }
    }

    /*suspend fun analyzeImage(
        prompt: String,
        imageBytes: ByteArray,
        modelName: String = "gemini-3.1-flash-lite-preview",
    ): String {

        val base64Image = Base64.encode(imageBytes)

        val requestBody = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt),
                        GeminiPart(
                            inlineData = GeminiInlineData(
                                mimeType = "image/jpeg",
                                data = base64Image
                            )
                        )
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(temperature = 0.1f)
        )

        val response: GeminiResponse = httpClient.post("$baseUrl/$modelName:generateContent?key=$apiKey") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()

        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "ERROR"
    }*/
}