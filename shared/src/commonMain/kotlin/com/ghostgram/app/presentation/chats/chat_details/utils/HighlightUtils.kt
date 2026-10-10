package com.ghostgram.app.presentation.chats.chat_details.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object HighlightUtils {
    /**
     * 💥 Подсвечивает все вхождения поискового запроса неоновым фиолетовым фоном
     */
    fun buildHighlightedText(
        text: String,
        query: String,
        highlightColor: Color = Color(0xFF7C4DFF).copy(alpha = 0.5f)
    ): AnnotatedString {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank() || !text.contains(cleanQuery, ignoreCase = true)) {
            return androidx.compose.ui.text.AnnotatedString(text)
        }

        return buildAnnotatedString {
            var currentIndex = 0
            val lowerText = text.lowercase()
            val lowerQuery = cleanQuery.lowercase()

            while (currentIndex < text.length) {
                val foundIndex = lowerText.indexOf(lowerQuery, currentIndex)
                if (foundIndex == -1) {
                    append(text.substring(currentIndex))
                    break
                }
                // Текст до найденного слова
                if (foundIndex > currentIndex) {
                    append(text.substring(currentIndex, foundIndex))
                }
                // 💥 Само найденное слово — красим в неоновый маркер!
                withStyle(
                    SpanStyle(
                        background = highlightColor,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(text.substring(foundIndex, foundIndex + cleanQuery.length))
                }
                currentIndex = foundIndex + cleanQuery.length
            }
        }
    }
}