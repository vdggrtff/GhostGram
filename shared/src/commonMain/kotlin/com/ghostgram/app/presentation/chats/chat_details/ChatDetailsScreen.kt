package com.ghostgram.app.presentation.chats.chat_details

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecondary
import com.ghostgram.app.ui.theme.GhostTextSecondary
import entity.Message
import entity.MessageMediaType
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

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty() && listState.firstVisibleItemIndex <= 2) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
    }
}

@Composable
fun GhostMessageBubble(
    message: Message,
    chatAvatarPath: String?,
    myAvatarPath: String?, // 💥 Вернули твою аватарку!
    chatTitle: String,
    modifier: Modifier = Modifier,
) {
    val isOutgoing = message.isOutgoing

    // Внешний ряд, который держит Аватарки и Пузырь
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom // Аватарки прижаты к низу
    ) {
        // 💥 1. АВАТАРКА СОБЕСЕДНИКА СЛЕВА (Для входящих)
        if (!isOutgoing) {
            if (chatAvatarPath != null) {
                AsyncImage(
                    model = chatAvatarPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE58235)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chatTitle.take(2).uppercase().ifBlank { "AC" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 💥 2. САМ ПУЗЫРЬ СООБЩЕНИЯ
        val bubbleShape = if (isOutgoing) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp) // Сделали чуть уже, чтобы влезла правая аватарка
                .clip(bubbleShape)
                .background(
                    if (isOutgoing)
                        Brush.linearGradient(listOf(GhostPrimary, GhostSecondary))
                    else
                        Brush.linearGradient(listOf(GhostCard, GhostCard))
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // 📄 Виджет документа
                if (message.mediaType == MessageMediaType.DOCUMENT) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF2F88D4)),
                            contentAlignment = Alignment.Center
                        ) { Text("📄", fontSize = 20.sp) }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = message.fileName ?: "Документ",
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1
                            )
                            Text(
                                text = message.fileExtraInfo ?: "Файл",
                                color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp
                            )
                        }
                    }
                }

                // 🖼 Фотография
                if (message.photoPath != null) {
                    AsyncImage(
                        model = message.photoPath,
                        contentDescription = "Фото",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(12.dp))
                    )
                }

                // 💬 Текст сообщения (ВЫВОДИТСЯ ОДИН РАЗ!)
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 21.sp
                    )
                }

                // 🗑 Метка Anti-Revoke
                if (message.isDeletedLocally) {
                    Text(
                        text = "🗑️ Удалено отправителем",
                        color = Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 💥 3. ТВОЯ АВАТАРКА СПРАВА (Для исходящих)
        if (isOutgoing) {
            Spacer(modifier = Modifier.width(8.dp))
            if (myAvatarPath != null && !myAvatarPath.startsWith("INITIALS:")) {
                AsyncImage(
                    model = myAvatarPath,
                    contentDescription = "Моя аватарка",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                )
            } else {
                // Красивая заглушка с инициалами
                val myName = myAvatarPath?.removePrefix("INITIALS:") ?: "Я"
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GhostPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = myName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}