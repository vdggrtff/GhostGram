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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.ghostgram.app.ui.theme.GhostTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GhostTopBar(
    avatarPath: String?,
    chatTitle: String,
    isGroup: Boolean = false, // 💥 Флаг группы
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
                    // 1. Аватарка чата / группы
                    key(avatarPath) {
                        if (!avatarPath.isNullOrBlank() && !avatarPath.startsWith("INITIALS:")) {
                            val model = if (avatarPath.startsWith("/")) "file://$avatarPath" else avatarPath
                            AsyncImage(
                                model = model,
                                contentDescription = "Аватарка",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(38.dp).clip(CircleShape)
                            )
                        } else {
                            // Заглушка с первой буквой
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

                    // 💥 2. Зеленая точка онлайна (ТОЛЬКО В ЛИЧКЕ, В ГРУППАХ ЕЁ НЕТ!)
                    if (!isGroup) {
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
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = chatTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 💥 3. ДИНАМИЧЕСКИЙ ПОДЗАГОЛОВОК
                    Text(
                        text = if (isGroup) "группа" else "в сети",
                        color = if (isGroup) GhostTextSecondary else GhostAccentGreen,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        },
        actions = {
            // 💥 4. ЗАМОК ШИФРОВАНИЯ ТОЛЬКО ДЛЯ ЛИЧНЫХ ЧАТОВ 1-НА-1
            if (!isGroup) {
                IconButton(onClick = { onIntent(ChatDetailsIntent.OnToggleCryptoMode) }, modifier = Modifier.size(34.dp)) {
                    Text(if (isCryptoMode) "🔒" else "🔓", fontSize = 15.sp)
                }
            }

            // Призрак и меню доступны везде
            IconButton(onClick = { onIntent(ChatDetailsIntent.OnToggleGhostMode) }, modifier = Modifier.size(34.dp)) {
                Text(if (isGhostMode) "👻" else "👁️", fontSize = 15.sp)
            }
            IconButton(onClick = { /* TODO */ }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
            }
        }
    )
}