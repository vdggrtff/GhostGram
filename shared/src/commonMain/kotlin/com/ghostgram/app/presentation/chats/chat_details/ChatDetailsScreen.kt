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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.*
import com.ghostgram.app.presentation.chats.chat_details.utils.*
import com.ghostgram.app.presentation.components.topbar.PinnedMessageBar
import com.ghostgram.app.presentation.components.animations.FlyingReactionOverlay
import com.ghostgram.app.presentation.components.bauble.GhostAlbumBubble
import com.ghostgram.app.presentation.components.bauble.GhostMessageBubble
import com.ghostgram.app.presentation.components.dialog.ChatInviteDialog
import com.ghostgram.app.presentation.components.dialog.SafetyVerificationDialog
import com.ghostgram.app.presentation.components.dialog.TelegramContextMenu
import com.ghostgram.app.presentation.components.fab.FabGetDown
import com.ghostgram.app.presentation.components.input.ChannelBottomBar
import com.ghostgram.app.presentation.components.input.GhostInput
import com.ghostgram.app.presentation.components.topbar.GhostTopBar
import com.ghostgram.app.presentation.components.topbar.InChatSearchBar
import com.ghostgram.app.presentation.components.utils.GhostMediaViewer
import com.ghostgram.app.presentation.components.utils.SwipeToReplyWrapper
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.utils.TimeFormatter
import entity.Message
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel


// =============================================================================
// 1. ENTRY POINT ROUTE (Навигационный роут экрана чата)
// =============================================================================
@Composable
fun ChatDetailsRoute(
    viewModel: ChatDetailsViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onNavigateToProfile: (Long) -> Unit,
    onNavigateToChat: (Long) -> Unit,
    scrollToMessageId: Long?,
    onMessageScrolled: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    ChatDetailsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        onProfileClick = { onNavigateToProfile(viewModel.chatId) },
        onDeepLinkClick = { link -> viewModel.onDeepLinkClick(link, onNavigateToChat) },
        onNavigateToChat = onNavigateToChat,
        scrollToMessageId = scrollToMessageId,
        onMessageScrolled = onMessageScrolled,
        onLoadCustomEmoji = { emojiId -> viewModel.getCustomEmojiPath(emojiId) }
    )
}

// =============================================================================
// 2. MAIN SCREEN COMPOSABLE
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailsScreen(
    state: ChatDetailsState,
    onIntent: (ChatDetailsIntent) -> Unit,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    onDeepLinkClick: (String) -> Unit,
    onNavigateToChat: (Long) -> Unit,
    scrollToMessageId: Long? = null,
    onMessageScrolled: () -> Unit = {},
    onLoadCustomEmoji: suspend (Long) -> String? = { null },
    listState: LazyListState = rememberLazyListState(),
) {
    val coroutineScope = rememberCoroutineScope()
    val defaultUriHandler = LocalUriHandler.current

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    // -------------------------------------------------------------------------
    // А. Локальное UI-состояние экрана
    // -------------------------------------------------------------------------
    var activeViewerMessageId by remember { mutableStateOf<Long?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }
    var selectedMessageOffset by remember { mutableStateOf(Offset.Zero) }
    var retryScrollCount by remember { mutableStateOf(0) }

    // Группировка потока сообщений в альбомы (2+ фото/видео)
    val groupedMessages = remember(state.messages) { groupMessagesIntoAlbums(state.messages) }

    // Контроллер скролла, пагинации и кнопки «Вниз»
    val scrollBehavior = rememberChatScrollBehavior(
        messages = state.messages,
        unreadCount = state.unreadCount,
        onLoadMore = { onIntent(LoadMoreMessages(it)) },
        listState = listState
    )

    // Перехватчик Telegram DeepLinks (t.me, tg://, @mentions)
    val ghostUriHandler = remember {
        object : androidx.compose.ui.platform.UriHandler {
            override fun openUri(uri: String) {
                if (uri.startsWith("tg://") || uri.contains("t.me/") || uri.startsWith("@")) {
                    onDeepLinkClick(uri)
                } else {
                    defaultUriHandler.openUri(uri)
                }
            }
        }
    }

    // Системный FilePicker для отправки фото и видео (FileKit KMP)
    val fileLauncher = rememberFilePickerLauncher(
        type = PickerType.ImageAndVideo,
        mode = PickerMode.Multiple()
    ) { files ->
        if (!files.isNullOrEmpty()) {
            coroutineScope.launch {
                val mediaList = files.map { file ->
                    MediaItem(file.readBytes(), file.name.substringAfterLast('.', "jpg"))
                }
                onIntent(OnMediaSelected(mediaList))
            }
        }
    }

    // -------------------------------------------------------------------------
    // Б. Сайд-эффекты: Навигация, Скролл к сообщению и Поиск
    // -------------------------------------------------------------------------
    // Бесшовный переход в чат по инвайту
    LaunchedEffect(state.pendingNavigationChatId) {
        val targetId = state.pendingNavigationChatId
        if (targetId != null && targetId != 0L) {
            onNavigateToChat(targetId)
            onIntent(OnResetPendingNavigation)
        }
    }

    // Программный скролл к целевому сообщению (из поиска, цитаты или профиля)
    LaunchedEffect(scrollToMessageId, groupedMessages.size) {
        if (scrollToMessageId != null && scrollToMessageId != 0L) {
            val targetIndex = groupedMessages.indexOfFirst { item ->
                when (item) {
                    is MessageListItem.Single -> item.message.id == scrollToMessageId
                    is MessageListItem.Album -> item.messages.any { it.id == scrollToMessageId }
                }
            }

            if (targetIndex != -1) {
                scrollBehavior.listState.animateScrollToItem(targetIndex)
                onMessageScrolled()
                retryScrollCount = 0
            } else if (retryScrollCount < 2) {
                retryScrollCount++
                onIntent(LoadMoreMessages(scrollToMessageId))
            } else {
                onMessageScrolled()
                retryScrollCount = 0
            }
        }
    }

    // Навигация по найденным сообщениям внутри чата (стрелки Вверх/Вниз)
    LaunchedEffect(state.currentSearchIndex, state.inChatSearchResults) {
        val targetMessage = state.inChatSearchResults.getOrNull(state.currentSearchIndex)
        if (targetMessage != null) {
            val index = groupedMessages.indexOfFirst { item ->
                when (item) {
                    is MessageListItem.Single -> item.message.id == targetMessage.id
                    is MessageListItem.Album -> item.messages.any { it.id == targetMessage.id }
                }
            }
            if (index != -1) {
                scrollBehavior.listState.animateScrollToItem(index)
            } else {
                onIntent(LoadMoreMessages(targetMessage.id))
            }
        }
    }

    // -------------------------------------------------------------------------
    // В. Основная верстка экрана (Scaffold & Overlays)
    // -------------------------------------------------------------------------
    CompositionLocalProvider(LocalUriHandler provides ghostUriHandler) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            containerColor = GhostBackground,
            topBar = {
                if (state.isSearchOpen) {
                    InChatSearchBar(
                        query = state.inChatSearchQuery,
                        totalCount = state.inChatSearchResults.size,
                        currentIndex = state.currentSearchIndex,
                        isSearching = state.isSearchingInChat,
                        isAiSearching = state.isAiSearching,
                        onQueryChanged = { onIntent(OnInChatSearchQueryChanged(it)) },
                        onPrevious = { onIntent(OnSearchPrevious) },
                        onNext = { onIntent(OnSearchNext) },
                        onRunAiSearch = { onIntent(OnRunAiSearch) },
                        onClose = { onIntent(OnToggleInChatSearch(false)) }
                    )
                } else {
                    GhostTopBar(
                        avatarPath = state.avatarPath,
                        chatTitle = state.chatTitle,
                        isGhostMode = state.isGhostMode,
                        isCryptoMode = state.isCryptoMode,
                        isGroup = state.isGroup,
                        isChannel = state.isChannel,
                        memberCount = state.memberCount,
                        typingText = state.typingText,
                        isOnline = state.isOnline,
                        statusText = state.statusText,
                        onIntent = onIntent,
                        onBackClick = onBackClick,
                        onProfileClick = onProfileClick,
                        onSearchClick = { onIntent(OnToggleInChatSearch(true)) }
                    )
                }
            },
            bottomBar = {
                // В каналах прячем инпут и показываем плашку звука Mute/Unmute
                if (state.isChannel) {
                    ChannelBottomBar(
                        isMuted = state.isMuted,
                        onToggleMute = { onIntent(OnToggleMute) }
                    )
                } else {
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
                }
            },
            floatingActionButton = {
                FabGetDown(
                    showScrollToBottom = scrollBehavior.showScrollToBottom,
                    onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                // -------------------------------------------------------------
                // Г. Список сообщений (Reverse LazyColumn)
                // -------------------------------------------------------------
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = if (state.pinnedMessage != null && !state.isSearchOpen) 58.dp else 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        count = groupedMessages.size,
                        key = { index ->
                            when (val item = groupedMessages[index]) {
                                is MessageListItem.Single -> item.message.id
                                is MessageListItem.Album -> item.messages.first().id
                            }
                        }
                    ) { index ->
                        val item = groupedMessages[index]
                        val currentSenderKey = getSenderKey(item)
                        val topNeighborKey = groupedMessages.getOrNull(index + 1)?.let { getSenderKey(it) }
                        val bottomNeighborKey = groupedMessages.getOrNull(index - 1)?.let { getSenderKey(it) }

                        // Хвостовые скругления для цепочек сообщений одного автора
                        val isFirstInGroup = currentSenderKey != topNeighborKey
                        val isLastInGroup = currentSenderKey != bottomNeighborKey

                        when (item) {
                            // 1. Одиночное сообщение (Текст, Фото, Видео, Стикер, Голосовое)
                            is MessageListItem.Single -> {
                                val repliedMsg = if (item.message.replyToMessageId != 0L) {
                                    state.messages.find { it.id == item.message.replyToMessageId }
                                } else null

                                SwipeToReplyWrapper(
                                    onSwipe = { onIntent(OnSwipeToReply(item.message)) }
                                ) {
                                    GhostMessageBubble(
                                        message = item.message,
                                        searchQuery = state.inChatSearchQuery,
                                        isGroup = state.isGroup,
                                        isFirstInGroup = isFirstInGroup,
                                        isLastInGroup = isLastInGroup,
                                        isBursting = state.activeBurstMessageId == item.message.id,
                                        burstEffectJson = state.burstEffectJson,
                                        burstEmoji = state.burstEmoji,
                                        onBurstFinished = { onIntent(OnClearBurst) },
                                        onMediaClick = { activeViewerMessageId = item.message.id },
                                        onVideoClick = { activeViewerMessageId = item.message.id },
                                        onLongClick = { offset ->
                                            selectedMessageForMenu = item.message
                                            selectedMessageOffset = offset
                                        },
                                        replyMessage = repliedMsg,
                                        onReactionClick = { msgId, emoji, customEmojiId, isChosen ->
                                            onIntent(OnToggleReaction(msgId, emoji, isChosen, customEmojiId))
                                            if (!isChosen) {
                                                onIntent(OnPlayReactionEffect(msgId, emoji))
                                            }
                                        },
                                        onLoadCustomEmoji = onLoadCustomEmoji
                                    )
                                }
                            }

                            // 2. Медиа-альбом (Сетка 2+ фотографий или видео)
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
                                        onMediaClick = { activeViewerMessageId = it },
                                        isGroup = state.isGroup,
                                        isLastInGroup = isLastInGroup,
                                        isFirstInGroup = isFirstInGroup,
                                        replyMessage = repliedMsg,
                                        onLongClick = { selectedMessageForMenu = baseMsg },
                                        onReactionClick = { msgId, emoji, customEmojiId, isChosen ->
                                            onIntent(OnToggleReaction(msgId, emoji, isChosen, customEmojiId))
                                            if (!isChosen) {
                                                onIntent(OnPlayReactionEffect(msgId, emoji))
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Липкая дата-разделитель между днями ("Сегодня", "Вчера", "12 мая")
                        if (shouldShowDateHeader(index, groupedMessages)) {
                            val dateText = TimeFormatter.formatDateHeader(item.dateUnix())
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
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(GhostCard.copy(alpha = 0.6f))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Закрепленное сообщение сверху чата
                val pinnedMsg = state.pinnedMessage
                if (pinnedMsg != null && !state.isSearchOpen) {
                    PinnedMessageBar(
                        pinnedMessage = pinnedMsg,
                        onClick = {
                            val targetIndex = groupedMessages.indexOfFirst { item ->
                                when (item) {
                                    is MessageListItem.Single -> item.message.id == pinnedMsg.id
                                    is MessageListItem.Album -> item.messages.any { it.id == pinnedMsg.id }
                                }
                            }
                            if (targetIndex != -1) {
                                coroutineScope.launch { listState.animateScrollToItem(targetIndex) }
                            } else {
                                onIntent(LoadMoreMessages(pinnedMsg.id))
                            }
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                // Фоновый оверлей эффекта реакции
                FlyingReactionOverlay(
                    emoji = state.flyingEmoji,
                    onFinished = { onIntent(OnClearFlyingReaction) }
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Д. Модальные окна, Диалоги и Просмотрщик медиа
    // -------------------------------------------------------------------------
    // Карточка предпросмотра инвайт-ссылки (t.me/+)
    if (state.invitePreview != null) {
        ChatInviteDialog(
            preview = state.invitePreview!!,
            isLoading = state.isJoiningChat,
            onJoinClick = { onIntent(OnConfirmJoinChat(state.invitePreview!!.inviteLink)) },
            onDismiss = { onIntent(OnDismissInviteDialog) }
        )
    }

    // Парящее контекстное меню Telegram с параболическим «Hop»-перелётом смайлика
    if (selectedMessageForMenu != null) {
        TelegramContextMenu(
            msg = selectedMessageForMenu!!,
            messagePosition = selectedMessageOffset,
            availableReactions = state.availableReactions,
            clipboardManager = clipboardManager,
            onIntent = onIntent,
            onDismiss = { selectedMessageForMenu = null }
        )
    }

    // Диалог проверки 4 эмодзи сквозного шифрования (Anti-MITM)
    if (state.isSafetyDialogOpen) {
        SafetyVerificationDialog(
            chatTitle = state.chatTitle,
            fingerprint = state.safetyFingerprint,
            onDismiss = { onIntent(ChatDetailsIntent.OnDismissSafetyDialog) }
        )
    }

    // Полноэкранный просмотрщик фото и видео
    if (activeViewerMessageId != null) {
        val chatMediaList = remember(state.messages) {
            state.messages.filter {
                (it.mediaType == entity.MessageMediaType.PHOTO && it.photoPath != null) ||
                        (it.mediaType == entity.MessageMediaType.VIDEO && (it.fileName != null || it.photoPath != null))
            }
        }

        GhostMediaViewer(
            mediaMessages = chatMediaList,
            initialMessageId = activeViewerMessageId!!,
            onDismiss = { activeViewerMessageId = null }
        )
    }
}

// =============================================================================
// 3. PRIVATE EXTENSIONS & HELPERS
// =============================================================================
private fun MessageListItem.dateUnix(): Int = when (this) {
    is MessageListItem.Single -> message.date
    is MessageListItem.Album -> messages.first().date
}
