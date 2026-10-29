package com.ghostgram.app.presentation.components.topbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Search
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
    isGroup: Boolean = false,
    isChannel: Boolean = false,
    memberCount: Int = 0,
    typingText: String? = null,
    isOnline: Boolean = false,
    statusText: String = "",
    isGhostMode: Boolean,
    isCryptoMode: Boolean,
    onBackClick: () -> Unit,
    onIntent: (ChatDetailsIntent) -> Unit,
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
){
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = GhostBackground),
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
            }
        },
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onProfileClick)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 💥 1. АВАТАРКА
                Box(contentAlignment = Alignment.BottomEnd) {
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

                    // 💥 Зеленая точка онлайна: ТОЛЬКО в личке и ТОЛЬКО если собеседник реально в сети!
                    if (!isGroup && !isChannel && isOnline) {
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

                // 💥 2. ЗАГОЛОВОК И ДИНАМИЧЕСКИЙ ПОДЗАГОЛОВОК
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chatTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 💥 1. ВЫЧИСЛЯЕМ ТЕКСТ СРАЗУ:
                    val isTyping = !typingText.isNullOrBlank()
                    val displaySubtitle = when {
                        isTyping -> typingText!!
                        isChannel -> formatMembers(memberCount, isChannel = true)
                        isGroup -> formatMembers(memberCount, isChannel = false)
                        isOnline -> "в сети"
                        statusText.isNotBlank() -> statusText
                        else -> "был(а) недавно"
                    }

                    // 💥 2. АНИМАЦИЯ РЕАГИРУЕТ НА ЛЮБОЕ ИЗМЕНЕНИЕ (ОНЛАЙН ВЫХОДИТ МГНОВЕННО БЕЗ КЛИКОВ!):
                    AnimatedContent(
                        targetState = displaySubtitle,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "status_anim"
                    ) { targetText ->
                        val subtitleColor = when {
                            isTyping -> GhostAccentGreen
                            !isGroup && !isChannel && isOnline -> GhostAccentGreen
                            else -> GhostTextSecondary
                        }

                        Text(
                            text = targetText,
                            color = subtitleColor,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        actions = {
            // Кнопка шифрования — ТОЛЬКО для лички 1-на-1
            if (!isGroup && !isChannel) {
                IconButton(onClick = { onIntent(ChatDetailsIntent.OnToggleCryptoMode) }, modifier = Modifier.size(34.dp)) {
                    Text(if (isCryptoMode) "🔒" else "🔓", fontSize = 15.sp)
                }
            }

            // Поиск в чате
            IconButton(onClick = onSearchClick, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Search, contentDescription = "Поиск", tint = Color.White)
            }

            // Призрак
            IconButton(onClick = { onIntent(ChatDetailsIntent.OnToggleGhostMode) }, modifier = Modifier.size(34.dp)) {
                Text(if (isGhostMode) "👻" else "👁️", fontSize = 15.sp)
            }

            IconButton(onClick = { /* Меню чата */ }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
            }
        }
    )
}

// 💥 ПРАВИЛЬНОЕ РУССКОЕ СКЛОНЕНИЕ УЧАСТНИКОВ И ПОДПИСЧИКОВ
private fun formatMembers(count: Int, isChannel: Boolean): String {
    if (count <= 0) return if (isChannel) "канал" else "группа"

    if (count >= 1000) {
        val thousands = count / 1000f
        val formatted = if (count % 1000 < 100) "${count / 1000}K" else "${(thousands * 10).toInt() / 10f}K"
        return "$formatted ${if (isChannel) "подписчиков" else "участников"}"
    }

    val mod10 = count % 10
    val mod100 = count % 100

    return if (isChannel) {
        when {
            mod100 in 11..14 -> "$count подписчиков"
            mod10 == 1 -> "$count подписчик"
            mod10 in 2..4 -> "$count подписчика"
            else -> "$count подписчиков"
        }
    } else {
        when {
            mod100 in 11..14 -> "$count участников"
            mod10 == 1 -> "$count участник"
            mod10 in 2..4 -> "$count участника"
            else -> "$count участников"
        }
    }
}