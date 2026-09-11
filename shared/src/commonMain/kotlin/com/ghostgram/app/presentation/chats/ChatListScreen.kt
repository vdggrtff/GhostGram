package com.ghostgram.app.presentation.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.presentation.components.items.GhostChatListItem
import com.ghostgram.app.presentation.components.input.GhostSearchBar
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecureGreen
import com.ghostgram.app.ui.theme.GhostTextSecondary
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatListRoute(
    viewModel: ChatListViewModel = koinViewModel(),
    onNavigateToChat: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    ChatListScreen(state = state, onIntent = viewModel::onIntent, onNavigateToChat = onNavigateToChat)
}

@Composable
fun ChatListScreen(
    state: ChatListState,
    onIntent: (ChatListIntent) -> Unit,
    onNavigateToChat: (Long) -> Unit
) {
    // Выбранная вкладка (пока только UI-переключатель)
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Все", "Личные", "Группы", "Каналы")
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GhostBackground)
    ) {
        // 💥 1. КАСТОМНЫЙ TOP BAR ИЗ МАКЕТА
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GhostSearchBar(
                query = searchQuery,
                onQueryChange = {searchQuery = it}
            )
            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(GhostCard)
                    .clickable { /* TODO: Быстрые настройки невидимости */ },
                contentAlignment = Alignment.Center
            ) {
                // Сделаем иконку щита неоново-зеленой, чтобы подчеркнуть, что Ghost Mode работает!
                Icon(Icons.Outlined.Shield, contentDescription = "Secure", tint = GhostSecureGreen, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = { /* TODO */ }) { Icon(Icons.Default.MoreVert, contentDescription = "Еще", tint = Color.White) }
        }

        // 💥 2. ТАБЫ (ВКЛАДКИ)
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = GhostBackground,
            contentColor = Color.White,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                // Тонкий фиолетовый индикатор под текстом
                SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = GhostPrimary,
                    height = 3.dp
                )
            },
            divider = { HorizontalDivider(color = GhostCard) } // Тонкая линия под табами
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) Color.White else GhostTextSecondary,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                )
            }
        }

        // 💥 3. СПИСОК ЧАТОВ ОТ КРАЯ ДО КРАЯ
        if (state.isLoading && state.chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GhostPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Отступ снизу, чтобы наш парящий BottomBar не перекрывал последнее сообщение!
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(state.chats, key = { it.id }) { chat ->
                    GhostChatListItem(
                        chat = chat,
                        onClick = { onNavigateToChat(chat.id) }
                    )
                }
            }
        }
    }
}