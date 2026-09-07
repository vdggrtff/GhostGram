package com.ghostgram.app.presentation.chats.chat_details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghostgram.app.presentation.components.bauble.GhostMessageBubble
import com.ghostgram.app.presentation.components.bottombar.GhostBottomBar
import com.ghostgram.app.presentation.components.dialog.GhostAlertDialog
import com.ghostgram.app.presentation.components.fab.FabGetDown
import com.ghostgram.app.presentation.components.topbar.GhostTopBar
import com.ghostgram.app.ui.theme.GhostBackground
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

// 💥 Тот самый Route
@Composable
fun ChatDetailsRoute(
    viewModel: ChatDetailsViewModel = koinViewModel(),
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    ChatDetailsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick
    )
}


// 💥 Тупой (Dumb) Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailsScreen(
    state: ChatDetailsState,
    onIntent: (ChatDetailsIntent) -> Unit,
    onBackClick: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {

    val coroutineScope = rememberCoroutineScope()

    // 💥 Кнопка "Вниз" видна, если мы отскроллили наверх больше чем на 3 сообщения
    val showScrollToBottom by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    var isInitialScrollDone by remember { mutableStateOf(false) }

    // 💥 УМНЫЙ СКРОЛЛ: если есть непрочитанные — скроллим к началу непрочитанных, если нет — в самый низ (к 0)
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty() && listState.firstVisibleItemIndex <= 1) {
            val targetIndex =
                if (state.unreadCount > 0) (state.unreadCount - 1).coerceAtLeast(0) else 0
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isEmpty()) return@LaunchedEffect

        if (!isInitialScrollDone) {
            // 💥 Только при ПЕРВОМ входе прыгаем к началу непрочитанных
            isInitialScrollDone = true
            val targetIndex = if (state.unreadCount > 0) (state.unreadCount - 1).coerceAtLeast(0) else 0
            listState.scrollToItem(targetIndex)
        } else {
            // 💥 А когда чат УЖЕ открыт и приходит НОВОЕ сообщение:
            // Мягко остаемся внизу (index 0), НИКАКИХ ПРЫЖКОВ НАВЕРХ!
            if (listState.firstVisibleItemIndex <= 1) {
                listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        containerColor = GhostBackground,
        topBar = {
            GhostTopBar(
                avatarPath = state.avatarPath,
                chatTitle = state.chatTitle,
                isGhostMode = state.isGhostMode,
                onIntent = onIntent,
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            GhostBottomBar(
                isCatchUpLoading = state.isCatchUpLoading,
                unreadCount = state.unreadCount,
                smartReplies = state.smartReplies,
                isRepliesLoading = state.isRepliesLoading,
                inputText = state.inputText,
                onIntent = onIntent,
                isCryptoMode = state.isCryptoMode
            )
        },
        floatingActionButton = {
            FabGetDown(
                showScrollToBottom = showScrollToBottom,
                onClick = {coroutineScope.launch { listState.animateScrollToItem(0) }}
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            reverseLayout = true, // 💥 Инвертированный список (низ = index 0)
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), // Только системные отступы баров
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ), // Идеальные отступы внутри списка
            verticalArrangement = Arrangement.spacedBy(8.dp) // Компактное расстояние между сообщениями
        ) {
            // asReversed переворачивает список, чтобы внизу (index 0) было самое последнее сообщение
            items(state.messages.asReversed(), key = { it.id }) { message ->
                GhostMessageBubble(
                    message = message,
                    chatAvatarPath = state.avatarPath,
                    myAvatarPath = state.myAvatarPath, // 💥 Передаем твою аватарку из стейта!
                    chatTitle = state.chatTitle
                )
            }
        }
        if (state.catchUpSummary != null) {
            GhostAlertDialog(
                catchUpSummary = state.catchUpSummary,
                onIntent = onIntent
            )
        }
    }
}