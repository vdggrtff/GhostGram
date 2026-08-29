package response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import model.GeminiContent

@Serializable
data class GeminiResponse(
    @SerialName("candidates") val candidates: List<GeminiCandidate>? = null
)

@Serializable
data class GeminiCandidate(
    @SerialName("content") val content: GeminiContent? = null
)