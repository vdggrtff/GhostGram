package com.ghostgram.app.presentation.chats.chat_details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.presentation.components.bauble.GhostMessageBubble
import com.ghostgram.app.presentation.components.topbar.GhostTopBar
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecondary
import com.ghostgram.app.ui.theme.GhostTextPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary
import entity.Message
import entity.MessageMediaType
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
            /*TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GhostBackground,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 💥 Аватарка собеседника в шапке
                        if (state.avatarPath != null) {
                            AsyncImage(
                                model = state.avatarPath,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(38.dp).clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(GhostPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.chatTitle.take(1).uppercase(),
                                    color = GhostPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = state.chatTitle,
                                fontSize = 16.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text("Online • Ghost Core", fontSize = 12.sp, color = GhostPrimary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onIntent(ChatDetailsIntent.OnToggleGhostMode) }
                    ) {
                        if (state.isGhostMode) {
                            // Режим невидимки включен (Фиолетовый неоновый призрак)
                            Text("👻", fontSize = 22.sp)
                        } else {
                            // Обычный режим (Глаз, тебя видят!)
                            Text("👁️", fontSize = 20.sp)
                        }
                    }
                }
            )*/
        },
        bottomBar = {
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
                            onClick = { onIntent(ChatDetailsIntent.OnCatchUpClick) },
                            label = {
                                Text(
                                    text = if (state.isCatchUpLoading) "🧠 Analyzing..."
                                    else if (state.unreadCount > 0) "✨ Catch Up (${state.unreadCount})"
                                    else "✨ Summary",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (state.unreadCount > 0) GhostPrimary.copy(alpha = 0.3f) else GhostCard
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = GhostPrimary
                            ),
                            enabled = !state.isCatchUpLoading
                        )
                    }

                    // Чип Smart Reply (оставляем рядом!)
                    if (state.smartReplies.isEmpty()) {
                        item {
                            SuggestionChip(
                                onClick = { onIntent(ChatDetailsIntent.OnGenerateRepliesClick) },
                                label = {
                                    Text(
                                        if (state.isRepliesLoading) "🧠 Thinking..." else "⚡️ Smart Reply",
                                        color = Color.White
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = GhostCard),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = GhostPrimary.copy(alpha = 0.5f)
                                ),
                                enabled = !state.isRepliesLoading
                            )
                        }
                    } else {
                        items(state.smartReplies) { reply ->
                            SuggestionChip(
                                onClick = { onIntent(ChatDetailsIntent.OnSmartReplyClick(reply)) },
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
                    if (state.smartReplies.isEmpty()) {
                        item {
                            SuggestionChip(
                                onClick = { onIntent(ChatDetailsIntent.OnGenerateRepliesClick) },
                                label = {
                                    Text(
                                        if (state.isRepliesLoading) "🧠 Думает..." else "⚡️ Умный ответ",
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
                                enabled = !state.isRepliesLoading
                            )
                        }
                    } else {
                        // 💥 Выводим 3 готовых варианта от Gemini!
                        items(state.smartReplies) { reply ->
                            SuggestionChip(
                                onClick = { onIntent(ChatDetailsIntent.OnSmartReplyClick(reply)) },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = state.inputText,
                        onValueChange = { onIntent(ChatDetailsIntent.OnInputChanged(it)) },
                        placeholder = { Text("Сообщение...", color = GhostTextSecondary) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = GhostCard,
                            unfocusedContainerColor = GhostCard,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { onIntent(ChatDetailsIntent.OnSendMessage) },
                        enabled = state.inputText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.inputText.isNotBlank())
                                    Brush.horizontalGradient(listOf(GhostPrimary, GhostSecondary))
                                else
                                    Brush.horizontalGradient(listOf(GhostCard, GhostCard))
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить",
                            tint = if (state.inputText.isNotBlank()) Color.White else GhostTextSecondary
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToBottom,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch { listState.animateScrollToItem(0) }
                    },
                    containerColor = GhostCard,
                    contentColor = GhostPrimary,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Вниз",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
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
            AlertDialog(
                onDismissRequest = { onIntent(ChatDetailsIntent.OnDismissCatchUpDialog) },
                containerColor = GhostCard,
                title = {
                    Text(
                        "⚡️ What You Missed",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        state.catchUpSummary!!,
                        color = GhostTextPrimary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onIntent(ChatDetailsIntent.OnDismissCatchUpDialog) }) {
                        Text("Got it", color = GhostPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}