package com.ghostgram.app.presentation.chats.chat_details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.LoadMoreMessages
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSwipeToReply
import com.ghostgram.app.presentation.chats.chat_details.utils.ChatDetailsDialogs
import com.ghostgram.app.presentation.chats.chat_details.utils.MessageListItem
import com.ghostgram.app.presentation.chats.chat_details.utils.getSenderKey
import com.ghostgram.app.presentation.chats.chat_details.utils.groupMessagesIntoAlbums
import com.ghostgram.app.presentation.chats.chat_details.utils.rememberChatScrollBehavior
import com.ghostgram.app.presentation.chats.chat_details.utils.shouldShowDateHeader
import com.ghostgram.app.presentation.components.bauble.GhostAlbumBubble
import com.ghostgram.app.presentation.components.bauble.GhostMessageBubble
import com.ghostgram.app.presentation.components.fab.FabGetDown
import com.ghostgram.app.presentation.components.input.GhostInput
import com.ghostgram.app.presentation.components.topbar.GhostTopBar
import com.ghostgram.app.presentation.components.topbar.InChatSearchBar
import com.ghostgram.app.presentation.components.utils.SwipeToReplyWrapper
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.utils.TimeFormatter
import entity.Message
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatDetailsRoute(
    viewModel: ChatDetailsViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onNavigateToProfile: (Long) -> Unit,
    scrollToMessageId: Long?,            // 💥 Получили из графа
    onMessageScrolled: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    ChatDetailsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        onProfileClick = { onNavigateToProfile(viewModel.chatId) },
        scrollToMessageId = scrollToMessageId, // 💥 Отдали в Dumb Screen
        onMessageScrolled = onMessageScrolled
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailsScreen(
    state: ChatDetailsState,
    onIntent: (ChatDetailsIntent) -> Unit,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    scrollToMessageId: Long? = null,        // 💥 Чистый параметр!
    onMessageScrolled: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {

    val coroutineScope = rememberCoroutineScope()
    var fullScreenImage by remember { mutableStateOf<String?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    val groupedMessages = remember(state.messages) { groupMessagesIntoAlbums(state.messages) }

    val scrollBehavior = rememberChatScrollBehavior(
        messages = state.messages,
        unreadCount = state.unreadCount,
        onLoadMore = { onIntent(LoadMoreMessages(it)) },
        listState = listState
    )

    val fileLauncher = rememberFilePickerLauncher(
        type = PickerType.ImageAndVideo,
        mode = PickerMode.Multiple()
    ) { files ->
        if (!files.isNullOrEmpty()) {
            coroutineScope.launch {
                val mediaList = files.map { file ->
                    MediaItem(file.readBytes(), file.name.substringAfterLast('.', "jpg"))
                }
                onIntent(ChatDetailsIntent.OnMediaSelected(mediaList))
            }
        }
    }

    var retryCount by remember { mutableStateOf(0) }

    LaunchedEffect(scrollToMessageId, groupedMessages.size) {
        if (scrollToMessageId != null && scrollToMessageId != 0L) {

            // Ищем сообщение в памяти
            val targetIndex = groupedMessages.indexOfFirst { item ->
                when (item) {
                    is MessageListItem.Single -> item.message.id == scrollToMessageId
                    is MessageListItem.Album -> item.messages.any { it.id == scrollToMessageId }
                }
            }

            if (targetIndex != -1) {
                // 💥 НАШЛИ! Летим прямо к нему!
                println("📍 [SCROLL] Сообщение найдено на позиции $targetIndex! Скроллим...")
                scrollBehavior.listState.animateScrollToItem(targetIndex)
                onMessageScrolled() // Сбрасываем ID
                retryCount = 0
            } else if (retryCount < 2) {
                // Если не нашли с первого раза — просим подгрузить историю вокруг него
                println("📍 [SCROLL] Загружаем историю вокруг ID=$scrollToMessageId (Попытка ${retryCount + 1})")
                retryCount++
                onIntent(ChatDetailsIntent.LoadMoreMessages(scrollToMessageId))
            } else {
                // Если после подгрузки так и не нашли — сбрасываем, чтобы не зависать
                println("⚠️ [SCROLL] Сообщение не удалось загрузить в память.")
                onMessageScrolled()
                retryCount = 0
            }
        }
    }

    LaunchedEffect(state.currentSearchIndex, state.inChatSearchResults) {
        val targetMessage = state.inChatSearchResults.getOrNull(state.currentSearchIndex)
        if (targetMessage != null) {
            // Ищем сообщение в текущем отображаемом списке
            val index = groupedMessages.indexOfFirst { item ->
                when (item) {
                    is MessageListItem.Single -> item.message.id == targetMessage.id
                    is MessageListItem.Album -> item.messages.any { it.id == targetMessage.id }
                }
            }
            if (index != -1) {
                scrollBehavior.listState.animateScrollToItem(index)
            } else {
                // Если сообщение старое — наш проверенный LoadMore подкачает его!
                onIntent(ChatDetailsIntent.LoadMoreMessages(targetMessage.id))
            }
        }
    }

    // Вспомогательная функция, чтобы достать дату из элемента
    fun getDateFromItem(item: MessageListItem): Int {
        return when (item) {
            is MessageListItem.Single -> item.message.date
            is MessageListItem.Album -> item.messages.first().date
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = GhostBackground,
        topBar = {
            if (state.isSearchOpen) {
                InChatSearchBar(
                    query = state.inChatSearchQuery,
                    totalCount = state.inChatSearchResults.size,
                    currentIndex = state.currentSearchIndex,
                    isSearching = state.isSearchingInChat,
                    isAiSearching = state.isAiSearching,
                    onQueryChanged = { onIntent(ChatDetailsIntent.OnInChatSearchQueryChanged(it)) },
                    onPrevious = { onIntent(ChatDetailsIntent.OnSearchPrevious) },
                    onNext = { onIntent(ChatDetailsIntent.OnSearchNext) },
                    onRunAiSearch = { onIntent(ChatDetailsIntent.OnRunAiSearch) },
                    onClose = { onIntent(ChatDetailsIntent.OnToggleInChatSearch(false)) }
                )
            } else {
                GhostTopBar(
                    avatarPath = state.avatarPath,
                    chatTitle = state.chatTitle,
                    isGhostMode = state.isGhostMode,
                    isCryptoMode = state.isCryptoMode,
                    isGroup = state.isGroup,
                    onIntent = onIntent,
                    onBackClick = onBackClick,
                    onProfileClick = onProfileClick,
                )
            }
        },
        bottomBar = {
            GhostInput(
                isCatchUpLoading = state.isCatchUpLoading,
                unreadCount = state.unreadCount,
                smartReplies = state.smartReplies,
                isRepliesLoading = state.isRepliesLoading,
                inputText = state.inputText,
                onIntent = onIntent,
                fileLauncher = fileLauncher,
                replyingToMessage = state.replyingToMessage,
                editingMessage = state.editingMessage
            )
        },
        floatingActionButton = {
            FabGetDown(
                showScrollToBottom = scrollBehavior.showScrollToBottom,
                onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()), /*.padding(innerPadding)*/
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = innerPadding.calculateBottomPadding() + 28.dp  /*8.dp*/
            ),
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

                val currentSenderKey = getSenderKey(item)

                // Кто автор сообщения ВЫШЕ на экране (старее в массиве: index + 1)?
                val topNeighborKey = groupedMessages.getOrNull(index + 1)?.let { getSenderKey(it) }

                // Кто автор сообщения НИЖЕ на экране (свежее в массиве: index - 1)?
                val bottomNeighborKey =
                    groupedMessages.getOrNull(index - 1)?.let { getSenderKey(it) }

                // 2. ПРАВИЛЬНЫЙ РАСЧЕТ ГРАНИЦ СООБЩЕНИЙ
                // Первое сообщение человека в пачке (над ним рисуем цветное имя):
                val isFirstInGroup = currentSenderKey != topNeighborKey

                // Последнее сообщение человека в пачке (рядом с ним рисуем его аватарку):
                val isLastInGroup = currentSenderKey != bottomNeighborKey

                // 3. ДАТА ТЕПЕРЬ СЧИТАЕТСЯ КОРРЕКТНО ДЛЯ АЛЬБОМОВ
                val showDateHeader = if (index == groupedMessages.size - 1) {
                    true
                } else {
                    val olderItem = groupedMessages[index + 1]
                    !TimeFormatter.isSameDay(itemDate, getDateFromItem(olderItem))
                }

                // 4. РИСУЕМ ПУЗЫРЬ ИЛИ ЦЕЛЫЙ АЛЬБОМ
                when (item) {
                    is MessageListItem.Single -> {
                        val repliedMsg = if (item.message.replyToMessageId != 0L) {
                            state.messages.find { it.id == item.message.replyToMessageId }
                        } else null
                        SwipeToReplyWrapper(
                            onSwipe = { onIntent(OnSwipeToReply(item.message)) }
                        ) {
                            GhostMessageBubble(
                                message = item.message,
                                isGroup = state.isGroup,
                                isFirstInGroup = isFirstInGroup, // 👈
                                isLastInGroup = isLastInGroup,   // 👈
                                onMediaClick = { fullScreenImage = it },
                                onLongClick = { selectedMessageForMenu = item.message },
                                replyMessage = repliedMsg,
                            )
                        }
                    }

                    is MessageListItem.Album -> {
                        val baseMsg = item.messages.first()
                        val repliedMsg = if (baseMsg.replyToMessageId != 0L) {
                            state.messages.find { it.id == baseMsg.replyToMessageId }
                        } else null
                        SwipeToReplyWrapper(
                            onSwipe = { onIntent(OnSwipeToReply(baseMsg)) }
                        ) {
                            GhostAlbumBubble(
                                albumMessages = item.messages,
                                onMediaClick = { fullScreenImage = it },
                                isGroup = state.isGroup,
                                isLastInGroup = isLastInGroup,
                                isFirstInGroup = isFirstInGroup,
                                replyMessage = repliedMsg,
                                onLongClick = { selectedMessageForMenu = baseMsg }
                            )
                        }
                    }
                }

                // ПЛАШКА ДАТЫ
                if (shouldShowDateHeader(index, groupedMessages)) {
                    val dateText = TimeFormatter.formatDateHeader(getDateFromItem(item))
                    if (dateText.isNotBlank()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dateText,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                    .background(GhostCard.copy(alpha = 0.6f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    ChatDetailsDialogs(
        state = state,
        fullScreenImage = fullScreenImage,
        selectedMessageForMenu = selectedMessageForMenu,
        clipboardManager = clipboardManager,
        onDismissFullScreenImage = { fullScreenImage = null },
        onDismissMessageMenu = { selectedMessageForMenu = null },
        onIntent = onIntent
    )
}
