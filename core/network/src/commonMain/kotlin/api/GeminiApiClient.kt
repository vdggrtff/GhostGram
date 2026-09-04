package api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import model.GeminiContent
import model.GeminiGenerationConfig
import model.GeminiInlineData
import model.GeminiPart
import model.GeminiRequest
import response.GeminiResponse
import kotlin.io.encoding.Base64

class GeminiApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    suspend fun generateText(
        prompt: String,
        modelName: String = "gemini-3.1-flash-lite-preview",
    ): String {
        val requestBody = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            generationConfig = GeminiGenerationConfig(temperature = 0.1f)
        )

            val response: GeminiResponse =
            httpClient.post("$baseUrl/$modelName:generateContent?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "ERROR"
    }

    suspend fun analyzeImage(
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
    }
}