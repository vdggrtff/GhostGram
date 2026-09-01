package com.ghostgram.app.presentation.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary
import entity.Chat
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatListRoute(
    viewModel: ChatListViewModel = koinViewModel(),
    onNavigateToChat: (Long) -> Unit // Принимаем лямбду от графа
) {
    val state by viewModel.state.collectAsState()

    ChatListScreen(
        state = state,
        onIntent = viewModel::onIntent, // Пробрасываем отправку интентов
        onNavigateToChat = onNavigateToChat
    )
}

@Composable
fun ChatListScreen(
    state: ChatListState,
    onIntent: (ChatListIntent) -> Unit,
    onNavigateToChat: (Long) -> Unit
) {
    Scaffold(
        containerColor = GhostBackground, // Темный фон из палитры
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = GhostBackground,
                    titleContentColor = GhostTextPrimary
                ),
                title = {
                    Text("Ghost", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            )
        },
        floatingActionButton = {
            // Круглая фиолетовая кнопка из твоего дизайна
            FloatingActionButton(
                onClick = { /* TODO: Новый чат */ },
                containerColor = GhostPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Text("✏️", fontSize = 24.sp)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isLoading && state.chats.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = GhostPrimary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp) // Отступы сверху и снизу
                ) {
                    items(state.chats, key = { it.id }) { chat ->
                        GhostChatCard(
                            chat = chat,
                            onClick = { onNavigateToChat(chat.id) },
                            onAiClick = { onIntent(ChatListIntent.OnSummarizeChatClick(chat.id)) },
                            isSummarizing = state.isSummarizing
                        )
                    }
                }
            }

            // Всплывающий диалог с готовой выжимкой от Gemini
            if (state.aiSummaryText != null) {
                AlertDialog(
                    onDismissRequest = { onIntent(ChatListIntent.OnDismissSummaryDialog) },
                    title = { Text("✨ AI Выжимка") },
                    text = { Text(state.aiSummaryText) },
                    confirmButton = {
                        TextButton(
                            onClick = { onIntent(ChatListIntent.OnDismissSummaryDialog) }
                        ) {
                            Text("Понял")
                        }
                    }
                )
            }

            // Ошибка от Gemini (если вдруг ключ не тот или инета нет)
            if (state.errorMessage != null) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                ) {
                    Text(state.errorMessage)
                }
            }
        }
    }
}

@Composable
fun GhostChatCard(
    chat: Chat,
    onClick: () -> Unit,
    onAiClick: () -> Unit,
    isSummarizing: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp) // Отступы между карточками
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp), // Скругление как на дизайне
        colors = CardDefaults.cardColors(containerColor = GhostCard)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Заглушка для аватарки (пока цветной кружок)
            if (chat.avatarPath != null) {
                AsyncImage(
                    model = chat.avatarPath,
                    contentDescription = "Аватар",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
            } else {
                // Фоллбэк, если аватарки нет или еще не скачалась
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(GhostBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.title.take(1).uppercase(),
                        color = GhostPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Тексты (Имя и последнее сообщение)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.title,
                        color = GhostTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = chat.lastMessage?.text ?: "Нет сообщений",
                    color = GhostTextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Кнопка AI вместо счетчика сообщений (для нашего MVP)
            IconButton(
                onClick = onAiClick,
                enabled = !isSummarizing,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GhostPrimary.copy(alpha = 0.1f))
            ) {
                Text(if (isSummarizing) "🧠" else "✨", fontSize = 16.sp)
            }
        }
    }
}