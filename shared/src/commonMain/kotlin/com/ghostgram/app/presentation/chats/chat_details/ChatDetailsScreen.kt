package com.ghostgram.app.presentation.chats.chat_details

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.chatTitle) },
                navigationIcon = {
                    IconButton(onClick = {
                        onIntent(ChatDetailsIntent.OnBackClicked)
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
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
            } else if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Выводим нашу "историю" (пока это просто текст)
                Text(
                    text = state.chatHistory,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}