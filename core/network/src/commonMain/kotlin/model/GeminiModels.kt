package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    @SerialName("contents") val contents: List<GeminiContent>,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiGenerationConfig(
    @SerialName("temperature") val temperature: Float
)

@Serializable
data class GeminiContent(
    @SerialName("parts") val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    @SerialName("text") val text: String? = null,
    @SerialName("inline_data") val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    @SerialName("mime_type") val mimeType: String,
    @SerialName("data") val data: String // Сюда мы будем класть картинку в Base64
)
