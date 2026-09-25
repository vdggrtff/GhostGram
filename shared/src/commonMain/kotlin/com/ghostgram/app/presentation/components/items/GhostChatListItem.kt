package com.ghostgram.app.presentation.components.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary
import com.ghostgram.app.utils.TimeFormatter
import entity.Chat

@Composable
fun GhostChatListItem(
    chat: Chat,
    onClick: () -> Unit
) {
    val timeString = TimeFormatter.formatTime(chat.lastMessage?.date ?: 0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp), // От края до края!
        verticalAlignment = Alignment.CenterVertically
    ) {
        // АВАТАРКА
        if (chat.avatarPath != null) {
            AsyncImage(
                model = chat.avatarPath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(54.dp).clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(GhostPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.title.take(1).uppercase().ifBlank { "💬" },
                    color = GhostPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // ИМЯ И ПОСЛЕДНЕЕ СООБЩЕНИЕ
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = chat.lastMessage?.text ?: "Нет сообщений",
                color = GhostTextSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // ВРЕМЯ И СЧЕТЧИК НЕПРОЧИТАННЫХ
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.height(44.dp), // Фиксируем высоту, чтобы элементы не прыгали
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Временно хардкодим время (в следующем шаге достанем реальное из TDLib)
            Text(
                text = timeString,
                color = GhostTextSecondary,
                fontSize = 12.sp
            )

            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 22.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(GhostPrimary)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(22.dp)) // Пустое место, чтобы текст не прыгал
            }
        }
    }
}