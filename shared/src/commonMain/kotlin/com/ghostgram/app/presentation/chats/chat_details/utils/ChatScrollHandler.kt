package com.ghostgram.app.presentation.chats.chat_details.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import entity.Message

class ChatScrollBehavior(
    val listState: LazyListState,
    val showScrollToBottom: Boolean
)

@Composable
fun rememberChatScrollBehavior(
    messages: List<Message>,
    unreadCount: Int,
    onLoadMore: (Long) -> Unit,
    listState: LazyListState = rememberLazyListState()
): ChatScrollBehavior {
    var isInitialScrollDone by remember { mutableStateOf(false) }

    val showScrollToBottom by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 5
        }
    }

    // Пагинация (подгрузка старых сообщений)
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && messages.isNotEmpty()) {
            onLoadMore(messages.first().id)
        }
    }

    // Умный скролл при открытии и приходе новых сообщений
    LaunchedEffect(messages.size) {
        if (messages.isEmpty()) return@LaunchedEffect

        if (!isInitialScrollDone) {
            isInitialScrollDone = true
            val targetIndex = if (unreadCount > 0) (unreadCount - 1).coerceAtLeast(0) else 0
            listState.scrollToItem(targetIndex)
        } else {
            if (listState.firstVisibleItemIndex <= 1) {
                listState.animateScrollToItem(0)
            }
        }
    }

    return remember(listState, showScrollToBottom) {
        ChatScrollBehavior(listState, showScrollToBottom)
    }
}