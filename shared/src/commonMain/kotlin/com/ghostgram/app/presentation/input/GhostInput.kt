package com.ghostgram.app.presentation.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnCatchUpClick
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnGenerateRepliesClick
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSmartReplyClick
import com.ghostgram.app.presentation.components.textfield.GhostTextField
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary

@Composable
fun GhostInput(
    isCatchUpLoading: Boolean,
    unreadCount: Int,
    smartReplies: List<String>,
    isRepliesLoading: Boolean,
    inputText: String,
    isCryptoMode: Boolean,
    onIntent: (ChatDetailsIntent) -> Unit
){
    Column(
        modifier = Modifier.fillMaxWidth().background(GhostBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // 💥 ПЛАВАЮЩИЙ РЯД AI-ЧИПОВ
        LazyRow(
            modifier = Modifier.padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            item {
                SuggestionChip(
                    onClick = { onIntent(OnCatchUpClick) },
                    label = {
                        Text(
                            text = if (isCatchUpLoading) "🧠 Analyzing..."
                            else if (unreadCount > 0) "✨ Catch Up (${unreadCount})"
                            else "✨ Summary",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (unreadCount > 0) GhostPrimary.copy(alpha = 0.3f) else GhostCard
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = GhostPrimary
                    ),
                    enabled = !isCatchUpLoading
                )
            }

            // Чип Smart Reply (оставляем рядом!)
            if (smartReplies.isEmpty()) {
                item {
                    SuggestionChip(
                        onClick = { onIntent(OnGenerateRepliesClick) },
                        label = {
                            Text(
                                if (isRepliesLoading) "🧠 Thinking..." else "⚡️ Smart Reply",
                                color = Color.White
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = GhostCard),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = GhostPrimary.copy(alpha = 0.5f)
                        ),
                        enabled = !isRepliesLoading
                    )
                }
            } else {
                items(smartReplies) { reply ->
                    SuggestionChip(
                        onClick = { onIntent(OnSmartReplyClick(reply)) },
                        label = { Text(reply, color = Color.White) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = GhostPrimary.copy(
                                alpha = 0.2f
                            )
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = GhostPrimary
                        )
                    )
                }
            }

            // Кнопка генерации (если ответов еще нет)
            if (smartReplies.isEmpty()) {
                item {
                    SuggestionChip(
                        onClick = { onIntent(OnGenerateRepliesClick) },
                        label = {
                            Text(
                                if (isRepliesLoading) "🧠 Думает..." else "⚡️ Умный ответ",
                                color = Color.White
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = GhostCard
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = GhostPrimary.copy(alpha = 0.5f)
                        ),
                        enabled = !isRepliesLoading
                    )
                }
            } else {
                // 💥 Выводим 3 готовых варианта от Gemini!
                items(smartReplies) { reply ->
                    SuggestionChip(
                        onClick = { onIntent(OnSmartReplyClick(reply)) },
                        label = { Text(reply, color = Color.White) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = GhostPrimary.copy(alpha = 0.2f)
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = GhostPrimary
                        )
                    )
                }

                // Кнопка сброса (крестик)
                item {
                    IconButton(
                        onClick = { /* TODO: Очистить ответы */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("✖️", color = GhostTextSecondary)
                    }
                }
            }
        }
        GhostTextField(
            inputText = inputText,
            onIntent = onIntent,
            isCryptoMode = isCryptoMode
        )
    }
}