package com.ghostgram.app.presentation.components.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.presentation.components.input.textfield.GhostTextField
import com.ghostgram.app.presentation.components.tabs.AiTabs
import entity.Message
import io.github.vinceglb.filekit.compose.PickerResultLauncher

@Composable
fun GhostInput(
    isCatchUpLoading: Boolean,
    unreadCount: Int,
    smartReplies: List<String>,
    isRepliesLoading: Boolean,
    inputText: String,
    replyingToMessage: Message?,
    editingMessage: Message?,
    fileLauncher: PickerResultLauncher,
    onIntent: (ChatDetailsIntent) -> Unit
){
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
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
            fileLauncher = fileLauncher,
            replyingToMessage = replyingToMessage,
            editingMessage = editingMessage
        )
    }
}