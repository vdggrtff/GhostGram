package com.ghostgram.app.presentation.components.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.ui.theme.GhostAccentGreen
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostPrimary

@Composable
fun GhostTopBar(
    avatarPath: String?,
    chatTitle: String,
    isGhostMode: Boolean,
    isCryptoMode: Boolean,
    onBackClick: () -> Unit,
    onIntent: (ChatDetailsIntent) -> Unit
){
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = GhostBackground),
        navigationIcon = {
            IconButton(onClick = { onBackClick() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    // Сама аватарка
                   key(avatarPath) {
                        if (!avatarPath.isNullOrBlank() && !avatarPath.startsWith("INITIALS:")) {
                            val model = if (avatarPath.startsWith("/")) "file://$avatarPath" else avatarPath
                            AsyncImage(
                                model = model, // 💥 Теперь с file://
                                contentDescription = "Аватарка чата",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(38.dp).clip(CircleShape)
                            )
                        } else {
                            // Заглушка с первой буквой имени
                            Box(
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(GhostPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chatTitle.take(1).uppercase().ifBlank { "💬" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                    /*if (avatarPath != null) {
                        AsyncImage(
                            model = avatarPath,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(GhostPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(chatTitle.take(1).uppercase(), color = GhostPrimary, fontWeight = FontWeight.Bold)
                        }
                    }*/
                    // Зеленая точка прямо на углу аватарки
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(GhostBackground)
                            .padding(1.5.dp)
                            .clip(CircleShape)
                            .background(GhostAccentGreen)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(text = chatTitle, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "в сети", color = GhostAccentGreen, fontSize = 12.sp, maxLines = 1)
                }
            }
        },
        actions = {
            IconButton(onClick = { onIntent(ChatDetailsIntent.OnToggleCryptoMode) }, modifier = Modifier.size(34.dp)) {
                Text(if (isCryptoMode) "🔒" else "🔓", fontSize = 15.sp)
            }
            IconButton(onClick = { onIntent(ChatDetailsIntent.OnToggleGhostMode) }, modifier = Modifier.size(34.dp)) {
                Text(if (isGhostMode) "👻" else "👁️", fontSize = 15.sp)
            }
            IconButton(onClick = { /* TODO */ }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
            }
        }
    )
}
    /*TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = GhostBackground,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    // Сама аватарка
                    if (avatarPath != null) {
                        AsyncImage(
                            model = avatarPath,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(GhostPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(chatTitle.take(1).uppercase(), color = GhostPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 💥 ЗЕЛЕНАЯ ТОЧКА ОНЛАЙНА (С обводкой цвета фона, чтобы не сливалась!)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(GhostBackground) // Обводка
                            .padding(2.dp) // Толщина обводки
                            .clip(CircleShape)
                            .background(Color(0xFF00E676)) // Неоновый зеленый
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = chatTitle,
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
            // Кнопка Шифрования
            IconButton(onClick = { onIntent(ChatDetailsIntent.OnToggleCryptoMode) }) {
                Text(
                    text = if (isCryptoMode) "🔒" else "🔓",
                    fontSize = 18.sp,
                    // Слегка затемняем открытый замок, чтобы не отвлекал
                    color = if (isCryptoMode) Color.White else Color.White.copy(alpha = 0.5f)
                )
            }

            // Кнопка Ghost Mode (Призрак/Глаз)
            IconButton(onClick = { onIntent(ChatDetailsIntent.OnToggleGhostMode) }) {
                Text(if (isGhostMode) "👻" else "👁️", fontSize = 20.sp)
            }

            // Стандартные три точки (Опции)
            IconButton(onClick = { *//* TODO *//* }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Опции", tint = Color.White)
            }
        }
    )*/
//}