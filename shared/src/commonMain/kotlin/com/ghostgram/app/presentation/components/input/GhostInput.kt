package com.ghostgram.app.presentation.components.input

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
import com.ghostgram.app.presentation.components.input.textfield.GhostTextField
import com.ghostgram.app.presentation.components.tabs.AiTabs
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary
import io.github.vinceglb.filekit.compose.PickerResultLauncher

@Composable
fun GhostInput(
    isCatchUpLoading: Boolean,
    unreadCount: Int,
    smartReplies: List<String>,
    isRepliesLoading: Boolean,
    inputText: String,
    fileLauncher: PickerResultLauncher,
    onIntent: (ChatDetailsIntent) -> Unit
){
    Column(
        modifier = Modifier.fillMaxWidth().background(GhostBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // 💥 ПЛАВАЮЩИЙ РЯД AI-ЧИПОВ
        AiTabs(
            smartReplies = smartReplies,
            isCatchUpLoading = isCatchUpLoading,
            isRepliesLoading = isRepliesLoading,
            unreadCount = unreadCount,
            onIntent = onIntent
        )
        GhostTextField(
            inputText = inputText,
            onIntent = onIntent,
            fileLauncher
        )
    }
}