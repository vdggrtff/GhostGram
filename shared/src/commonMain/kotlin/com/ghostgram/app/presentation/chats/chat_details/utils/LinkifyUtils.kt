package com.ghostgram.app.presentation.chats.chat_details.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.NonCancellable.start

object LinkifyUtils {

    // Регулярки для ссылок и юзернеймов Telegram
    private val URL_REGEX = Regex("""(https?://[^\s]+|t\.me/[^\s]+)""")
    private val MENTION_REGEX = Regex("""@([a-zA-Z0-9_]{4,32})""")

    /**
     * Превращает обычный текст в форматированный:
     * 1. Подсвечивает поисковый запрос (если есть).
     * 2. Делает @username и t.me/... кликабельными ссылками приятного голубого цвета Telegram!
     */
    fun createLinkedText(
        text: String,
        searchQuery: String = "",
        linkColor: Color = Color(0xFF64B5F6) // Фирменный лазурный цвет ссылок Telegram
    ): AnnotatedString {
        if (text.isBlank()) return AnnotatedString(text)

        // 1. Сначала накладываем подсветку поиска
        val baseAnnotated = HighlightUtils.buildHighlightedText(text, searchQuery)

        return buildAnnotatedString {
            append(baseAnnotated)

            val linkStyles = TextLinkStyles(
                style = SpanStyle(
                    color = linkColor,
                    fontWeight = FontWeight.SemiBold
                )
            )

            // 💥 2. Находим все Web-ссылки и t.me
            URL_REGEX.findAll(text).forEach { matchResult ->
                val range = matchResult.range
                val url = matchResult.value
                val fullUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url

                addLink(
                    url = LinkAnnotation.Url(
                        url = fullUrl,
                        styles = linkStyles
                    ),
                    start = range.first,
                    end = range.last + 1
                )
            }

            // 💥 3. Находим все @username упоминания
            MENTION_REGEX.findAll(text).forEach { matchResult ->
                val range = matchResult.range
                val username = matchResult.value // Например "@durov"

                addLink(
                    url = LinkAnnotation.Url( // 💥 БЫЛО clickable =, СТАЛО url =
                        url = "tg://resolve?domain=${username.removePrefix("@")}",
                        styles = linkStyles
                    ),
                    start = range.first,
                    end = range.last + 1
                )
            }
        }
    }
}