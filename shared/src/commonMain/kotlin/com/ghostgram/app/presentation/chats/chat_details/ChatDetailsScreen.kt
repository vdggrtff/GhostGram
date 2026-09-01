package com.ghostgram.app.presentation.chats.chat_details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.ghostgram.app.ui.theme.BubbleIncomingBg
import com.ghostgram.app.ui.theme.BubblePurpleEnd
import com.ghostgram.app.ui.theme.BubblePurpleStart
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecondary
import com.ghostgram.app.ui.theme.GhostTextSecondary
import entity.Message
import org.koin.compose.viewmodel.koinViewModel

// 💥 Тот самый Route
@Composable
fun ChatDetailsRoute(
    viewModel: ChatDetailsViewModel = koinViewModel(),
    onBackClick: () -> Unit
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
    listState: LazyListState = rememberLazyListState()
) {

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty() && listState.firstVisibleItemIndex <= 2) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Чат #${state.messages.firstOrNull()?.chatId ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GhostBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Поле ввода сообщения
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
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            reverseLayout = true, // 💥 Инвертированный список (низ = index 0)
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), // Только системные отступы баров
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), // Идеальные отступы внутри списка
            verticalArrangement = Arrangement.spacedBy(8.dp) // Компактное расстояние между сообщениями
        ) {
            // asReversed переворачивает список, чтобы внизу (index 0) было самое последнее сообщение
            items(state.messages.asReversed(), key = { it.id }) { message ->
                GhostMessageBubble(message = message)
            }
        }
    }
}

@Composable
fun GhostMessageBubble(
    message: Message,
    modifier: Modifier = Modifier // 💥 Принимаем модификатор для анимаций
) {
    val isOutgoing = message.isOutgoing

    Box(
        modifier = modifier.fillMaxWidth(), // 💥 Применяем его к внешнему Box
        contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        // ... остальной код пузыря без изменений ...
        val bubbleShape = if (isOutgoing) {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
        }

        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(bubbleShape)
                .background(
                    if (isOutgoing)
                        Brush.linearGradient(listOf(BubblePurpleStart, BubblePurpleEnd))
                    else
                        Brush.linearGradient(listOf(BubbleIncomingBg, BubbleIncomingBg))
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                if (message.photoPath != null) {
                    AsyncImage(
                        model = message.photoPath,
                        contentDescription = "Фото",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // Текст / Подпись к фото
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }

                // Метка Anti-Revoke
                if (message.isDeletedLocally) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🗑️ Удалено отправителем",
                        color = Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}