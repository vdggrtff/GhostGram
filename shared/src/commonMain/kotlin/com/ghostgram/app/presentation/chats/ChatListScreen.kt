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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.ghostgram.app.ui.theme.GhostBadgeBg
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecureGreen
import com.ghostgram.app.ui.theme.GhostTextSecondary
import entity.Chat
import entity.Message
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatListRoute(
    viewModel: ChatListViewModel = koinViewModel(),
    onNavigateToChat: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    ChatListScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateToChat = onNavigateToChat
    )
}

@Composable
fun ChatListScreen(
    state: ChatListState,
    onIntent: (ChatListIntent) -> Unit,
    onNavigateToChat: (Long) -> Unit,
) {
    // Выбранная вкладка (пока только UI-переключатель)
    var selectedTab by remember { mutableStateOf(0) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GhostBackground)
    ) {
        // 💥 1. КАСТОМНЫЙ TOP BAR ИЗ МАКЕТА
        /*Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GhostSearchBar(
                query = state.searchQuery,
                onQueryChange = { newText ->
                    onIntent(ChatListIntent.OnSearchQueryChanged(newText))
                }
            )
            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(GhostCard)
                    .clickable { *//* TODO: Быстрые настройки невидимости *//* },
                contentAlignment = Alignment.Center
            ) {
                // Сделаем иконку щита неоново-зеленой, чтобы подчеркнуть, что Ghost Mode работает!
                Icon(
                    Icons.Outlined.Shield,
                    contentDescription = "Secure",
                    tint = GhostSecureGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = { *//* TODO *//* }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Еще",
                    tint = Color.White
                )
            }
        }*/

        if (isSearchActive) {
            // РЕЖИМ ПОИСКА (Строка + Кнопка Назад)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp, start = 4.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isSearchActive = false // Закрываем поиск
                        onIntent(ChatListIntent.OnSearchQueryChanged("")) // Очищаем запрос
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                }

                GhostSearchBar(
                    query = state.searchQuery,
                    onQueryChange = { onIntent(ChatListIntent.OnSearchQueryChanged(it)) },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // ОБЫЧНЫЙ РЕЖИМ (Логотип + Иконки)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GhostGram", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)

                Spacer(modifier = Modifier.width(8.dp))

                // Бейдж SECURE
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(GhostBadgeBg).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GhostSecureGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SECURE", color = GhostPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.weight(1f))

                // 💥 ИКОНКА ПОИСКА (Открывает строку!)
                IconButton(onClick = { isSearchActive = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Поиск", tint = Color.White)
                }

                IconButton(onClick = { /* TODO: Настройки Privacy */ }) {
                    Icon(Icons.Outlined.Shield, contentDescription = "Secure", tint = GhostSecureGreen)
                }
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
                // 💥 РАЗВИЛКА: ЕСЛИ ВВЕДЕН ТЕКСТ ПОИСКА
                /*if (state.searchQuery.isNotBlank()) {

                    // Локальный поиск по названиям чатов
                    val localResults = state.chats.filter { chat ->
                        chat.title.contains(state.searchQuery, ignoreCase = true)
                    }

                    if (localResults.isNotEmpty()) {
                        item { Text("Мои чаты", color = GhostPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
                        items(localResults, key = { "local_${it.id}" }) { chat ->
                            GhostChatListItem(chat = chat, onClick = { onNavigateToChat(chat.id) })
                        }
                    }

                    // Глобальный поиск
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Глобальный поиск", color = GhostPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("🛡️ Спам скрыт", color = GhostSecureGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (state.messageSearchResults.isNotEmpty()) {
                        item {
                            Text("Сообщения", color = GhostPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                        }
                        items(state.messageSearchResults, key = { "msg_${it.lastMessage?.id}" }) { chat ->
                            GhostChatListItem(chat = chat, onClick = { onNavigateToChat(chat.id) })
                        }
                    }

                    if (state.isSearching) {
                        item { Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GhostPrimary, modifier = Modifier.size(24.dp)) } }
                    } else if (localResults.isEmpty() && state.messageSearchResults.isEmpty() && state.globalSearchResults.isEmpty()) {
                        item { Text("Ничего не найдено", color = GhostTextSecondary, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                    }

                    items(state.globalSearchResults, key = { "search_${it.id}" }) { publicChat ->
                        GhostChatListItem(
                            chat = Chat(id = publicChat.id, title = publicChat.title, unreadCount = 0, lastMessage = Message(id = 0, chatId = publicChat.id, senderName = "", text = "@${publicChat.username}"), avatarPath = publicChat.avatarPath),
                            onClick = { onNavigateToChat(publicChat.id) }
                        )
                    }
                }*/
                if (state.searchQuery.isNotBlank()) {

                    // 1. ЛОКАЛЬНЫЕ ЧАТЫ (Мои чаты)
                    val localResults = state.chats.filter { chat ->
                        chat.title.contains(state.searchQuery, ignoreCase = true)
                    }

                    if (localResults.isNotEmpty()) {
                        item { Text("Мои чаты", color = GhostPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
                        items(localResults, key = { "local_${it.id}" }) { chat ->
                            GhostChatListItem(chat = chat, onClick = { onNavigateToChat(chat.id) })
                        }
                    }

                    // 2. ГЛОБАЛЬНЫЙ ПОИСК (Каналы)
                    if (state.globalSearchResults.isNotEmpty() || state.isSearching) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Глобальный поиск", color = GhostPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("🛡️ Спам скрыт", color = GhostSecureGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (state.isSearching) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GhostPrimary, modifier = Modifier.size(24.dp)) } }
                        }

                        items(state.globalSearchResults, key = { "search_${it.id}" }) { publicChat ->
                            GhostChatListItem(
                                chat = Chat(id = publicChat.id, title = publicChat.title, unreadCount = 0, lastMessage = Message(id = 0, chatId = publicChat.id, senderName = "", text = "@${publicChat.username}"), avatarPath = publicChat.avatarPath),
                                onClick = { onNavigateToChat(publicChat.id) }
                            )
                        }
                    }

                    // 3. СООБЩЕНИЯ (Из твоих переписок)
                    if (state.messageSearchResults.isNotEmpty()) {
                        item {
                            Text("Сообщения", color = GhostPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                        }

                        // 💥 ФИКС КРАША: Ключ = ChatID + MessageID (абсолютно уникальный!)
                        items(state.messageSearchResults, key = { "msg_${it.id}_${it.lastMessage?.id}" }) { chat ->
                            GhostChatListItem(chat = chat, onClick = { onNavigateToChat(chat.id) })
                        }
                    }

                    // 4. "НИЧЕГО НЕ НАЙДЕНО"
                    if (localResults.isEmpty() && state.messageSearchResults.isEmpty() && state.globalSearchResults.isEmpty() && !state.isSearching) {
                        item { Text("Ничего не найдено", color = GhostTextSecondary, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                    }
                }
                // 💥 ИНАЧЕ - ОБЫЧНЫЙ СПИСОК ТВОИХ ЧАТОВ
                else {
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
}