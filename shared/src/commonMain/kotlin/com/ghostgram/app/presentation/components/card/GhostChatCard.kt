package com.ghostgram.app.presentation.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary
import entity.Chat

@Composable
fun GhostChatCard(
    chat: Chat,
    onClick: () -> Unit,
    onAiClick: () -> Unit,
    isSummarizing: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp) // Отступы между карточками
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp), // Скругление как на дизайне
        colors = CardDefaults.cardColors(containerColor = GhostCard)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Заглушка для аватарки (пока цветной кружок)
            if (chat.avatarPath != null) {
                AsyncImage(
                    model = chat.avatarPath,
                    contentDescription = "Аватар",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
            } else {
                // Фоллбэк, если аватарки нет или еще не скачалась
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(GhostBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.title.take(1).uppercase(),
                        color = GhostPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Тексты (Имя и последнее сообщение)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.title,
                        color = GhostTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = chat.lastMessage?.text ?: "Нет сообщений",
                    color = GhostTextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Кнопка AI вместо счетчика сообщений (для нашего MVP)
            IconButton(
                onClick = onAiClick,
                enabled = !isSummarizing,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GhostPrimary.copy(alpha = 0.1f))
            ) {
                Text(if (isSummarizing) "🧠" else "✨", fontSize = 16.sp)
            }
        }
    }
}