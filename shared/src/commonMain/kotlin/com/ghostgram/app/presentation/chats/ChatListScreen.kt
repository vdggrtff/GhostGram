package com.ghostgram.app.presentation.chats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.presentation.chats.ChatListIntent.OnSummarizeChatClick
import com.ghostgram.app.presentation.components.card.GhostChatCard
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextPrimary
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
                            onAiClick = { onIntent(OnSummarizeChatClick(chat.id)) },
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