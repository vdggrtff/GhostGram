package com.ghostgram.app.presentation.chats.chat_details.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipboardManager
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnToggleStickers
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsState
import com.ghostgram.app.presentation.components.bottom_sheet.StickerBottomSheet
import com.ghostgram.app.presentation.components.dialog.*
import entity.Message

@Composable
fun ChatDetailsDialogs(
    state: ChatDetailsState,
    //fullScreenImage: String?,
    selectedMessageForMenu: Message?,
    clipboardManager: ClipboardManager,
    //onDismissFullScreenImage: () -> Unit,
    onDismissMessageMenu: () -> Unit,
    onIntent: (ChatDetailsIntent) -> Unit
) {
    if (state.catchUpSummary != null) {
        GhostAlertDialog(catchUpSummary = state.catchUpSummary, onIntent = onIntent)
    }
    /*if (fullScreenImage != null) {
        FullScreenImageDialog(imageUrl = fullScreenImage, onDismiss = onDismissFullScreenImage)
    }*/
    if (state.pendingMedia.isNotEmpty()) {
        PendingMediaDialog(
            pendingMedia = state.pendingMedia,
            sendAsDocument = state.sendAsDocument,
            pendingCaption = state.pendingCaption,
            onIntent = onIntent
        )
    }
    if (selectedMessageForMenu != null) {
        SelectedMessageForMenuDialog(
            msg = selectedMessageForMenu,
            clipboardManager = clipboardManager,
            onIntent = onIntent,
            onDismiss = onDismissMessageMenu
        )
    }
    if (state.isStickersOpen) {
        StickerBottomSheet(
            stickers = state.recentStickers,
            onDismiss = { onIntent(OnToggleStickers) },
            onIntent = onIntent
        )
    }
}