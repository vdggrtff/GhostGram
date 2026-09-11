package com.ghostgram.app.presentation.chats.chat_details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.ghostgram.app.presentation.components.bauble.GhostAlbumBubble
import com.ghostgram.app.presentation.components.bauble.GhostMessageBubble
import com.ghostgram.app.presentation.components.dialog.GhostAlertDialog
import com.ghostgram.app.presentation.components.fab.FabGetDown
import com.ghostgram.app.presentation.components.input.GhostInput
import com.ghostgram.app.presentation.components.topbar.GhostTopBar
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.utils.TimeFormatter
import entity.Message
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

sealed class MessageListItem {
    data class Single(val message: Message) : MessageListItem()
    data class Album(val messages: List<Message>) : MessageListItem()
}

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

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 5
        }
    }

    var fullScreenImage by remember { mutableStateOf<String?>(null) }

    // 💥 УМНЫЙ СКРОЛЛ: если есть непрочитанные — скроллим к началу непрочитанных, если нет — в самый низ (к 0)
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty() && listState.firstVisibleItemIndex <= 1) {
            val targetIndex =
                if (state.unreadCount > 0) (state.unreadCount - 1).coerceAtLeast(0) else 0
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && state.messages.isNotEmpty()) {
            // 💥 БЕРЕМ .first(), ТАК КАК ОНО САМОЕ СТАРОЕ В БАЗЕ!
            val oldestMessage = state.messages.first()
            onIntent(ChatDetailsIntent.LoadMoreMessages(oldestMessage.id))
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isEmpty()) return@LaunchedEffect

        if (!isInitialScrollDone) {
            // 💥 Только при ПЕРВОМ входе прыгаем к началу непрочитанных
            isInitialScrollDone = true
            val targetIndex =
                if (state.unreadCount > 0) (state.unreadCount - 1).coerceAtLeast(0) else 0
            listState.scrollToItem(targetIndex)
        } else {
            // 💥 А когда чат УЖЕ открыт и приходит НОВОЕ сообщение:
            // Мягко остаемся внизу (index 0), НИКАКИХ ПРЫЖКОВ НАВЕРХ!
            if (listState.firstVisibleItemIndex <= 1) {
                listState.animateScrollToItem(0)
            }
        }
    }

    val groupedMessages = remember(state.messages) {
        val result = mutableListOf<MessageListItem>()
        var currentAlbumId = 0L
        var currentAlbum = mutableListOf<Message>()

        // Идем по списку от старых к новым
        for (msg in state.messages) {
            if (msg.mediaAlbumId != 0L) {
                if (msg.mediaAlbumId == currentAlbumId) {
                    currentAlbum.add(msg)
                } else {
                    if (currentAlbum.isNotEmpty()) result.add(MessageListItem.Album(currentAlbum))
                    currentAlbumId = msg.mediaAlbumId
                    currentAlbum = mutableListOf(msg)
                }
            } else {
                if (currentAlbum.isNotEmpty()) {
                    result.add(MessageListItem.Album(currentAlbum))
                    currentAlbum = mutableListOf()
                    currentAlbumId = 0L
                }
                result.add(MessageListItem.Single(msg))
            }
        }
        if (currentAlbum.isNotEmpty()) result.add(MessageListItem.Album(currentAlbum))

        result.asReversed() // Переворачиваем для LazyColumn (самые новые внизу)
    }

    // Вспомогательная функция, чтобы достать дату из элемента
    fun getDateFromItem(item: MessageListItem): Int {
        return when (item) {
            is MessageListItem.Single -> item.message.date
            is MessageListItem.Album -> item.messages.first().date
        }
    }

    Scaffold(
        containerColor = GhostBackground,
        topBar = {
            GhostTopBar(
                avatarPath = state.avatarPath,
                chatTitle = state.chatTitle,
                isGhostMode = state.isGhostMode,
                isCryptoMode = state.isCryptoMode,
                onIntent = onIntent,
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            GhostInput(
                isCatchUpLoading = state.isCatchUpLoading,
                unreadCount = state.unreadCount,
                smartReplies = state.smartReplies,
                isRepliesLoading = state.isRepliesLoading,
                inputText = state.inputText,
                onIntent = onIntent,
            )
        },
        floatingActionButton = {
            FabGetDown(
                showScrollToBottom = showScrollToBottom,
                onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Итерируемся по альбомам и одиночным сообщениям
            items(groupedMessages.size, key = { index ->
                when (val item = groupedMessages[index]) {
                    is MessageListItem.Single -> item.message.id
                    is MessageListItem.Album -> item.messages.first().id
                }
            }) { index ->

                val item = groupedMessages[index]
                val itemDate = getDateFromItem(item)

                // 💥 3. ДАТА ТЕПЕРЬ СЧИТАЕТСЯ КОРРЕКТНО ДЛЯ АЛЬБОМОВ
                val showDateHeader = if (index == groupedMessages.size - 1) {
                    true
                } else {
                    val olderItem = groupedMessages[index + 1]
                    !TimeFormatter.isSameDay(itemDate, getDateFromItem(olderItem))
                }

                // 💥 4. РИСУЕМ ПУЗЫРЬ ИЛИ ЦЕЛЫЙ АЛЬБОМ
                when (item) {
                    is MessageListItem.Single -> {
                        GhostMessageBubble(
                            message = item.message,
                            chatAvatarPath = state.avatarPath,
                            myAvatarPath = state.myAvatarPath,
                            chatTitle = state.chatTitle,
                            onMediaClick = { fullScreenImage = it }
                        )
                    }
                    is MessageListItem.Album -> {
                        GhostAlbumBubble(
                            albumMessages = item.messages,
                            chatAvatarPath = state.avatarPath,
                            myAvatarPath = state.myAvatarPath,
                            chatTitle = state.chatTitle,
                            onMediaClick = { fullScreenImage = it }
                        )
                    }
                }

                // ПЛАШКА ДАТЫ
                if (showDateHeader) {
                    val dateText = TimeFormatter.formatDateHeader(itemDate)
                    if (dateText.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = dateText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(GhostCard.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        if (state.catchUpSummary != null) {
            GhostAlertDialog(
                catchUpSummary = state.catchUpSummary,
                onIntent = onIntent
            )
        }
    }
    if (fullScreenImage != null) {
        Dialog(
            onDismissRequest = { fullScreenImage = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { fullScreenImage = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(model = fullScreenImage, contentDescription = "Full Screen", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                IconButton(
                    onClick = { fullScreenImage = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Text("✖", color = Color.White)
                }
            }
        }
    }
}