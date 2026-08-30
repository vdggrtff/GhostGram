package com.ghostgram.app.presentation.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
        topBar = {
            TopAppBar(
                title = { Text("GhostGRAM 👻") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.chats, key = { it.id }) { chat ->
                        ListItem(
                            // 💥 ВОТ ОНА — Навигация при клике на сам чат!
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToChat(chat.id) },
                            headlineContent = { Text(chat.title) },
                            supportingContent = {
                                Text(chat.lastMessage?.text ?: "Нет сообщений")
                            },
                            trailingContent = {
                                // Кнопка ИИ (на неё кликаем — делаем выжимку, а не переходим в чат)
                                Button(
                                    onClick = { onIntent(ChatListIntent.OnSummarizeChatClick(chat.id)) },
                                    enabled = !state.isSummarizing
                                ) {
                                    Text(if (state.isSummarizing) "🧠" else "✨ AI")
                                }
                            }
                        )
                        HorizontalDivider()
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