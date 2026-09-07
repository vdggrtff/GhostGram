package com.ghostgram.app.presentation.components.chips

import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary

@Composable
fun GhostChip(
    isRepliesLoading: Boolean,
    onIntent: (ChatDetailsIntent) -> Unit
){
    SuggestionChip(
        onClick = { onIntent(ChatDetailsIntent.OnGenerateRepliesClick) },
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