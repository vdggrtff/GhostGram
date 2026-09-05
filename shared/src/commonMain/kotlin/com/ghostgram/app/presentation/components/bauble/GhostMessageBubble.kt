package com.ghostgram.app.presentation.components.bauble

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecondary
import entity.Message
import entity.MessageMediaType

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
            RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 4.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            )
        }

        Box(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 280.dp) // Ограничиваем только максимум, чтобы он сжимался под текст
                .clip(bubbleShape)
                .background(
                    if (isOutgoing)
                        Brush.linearGradient(listOf(GhostPrimary, GhostSecondary))
                    else
                        Brush.linearGradient(listOf(GhostCard, GhostCard))
                )
                .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 6.dp) // Снизу отступ чуть меньше для красивых галочек
        ) {
            Column(
                modifier = Modifier.wrapContentWidth(), // 💥 Пузырь плотно облегает контент
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                // 📄 Виджет документа
                if (message.mediaType == MessageMediaType.DOCUMENT) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape)
                                .background(Color(0xFF2F88D4)),
                            contentAlignment = Alignment.Center
                        ) { Text("📄", fontSize = 20.sp) }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = message.fileName ?: "Документ",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1
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
                        modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
                            .clip(RoundedCornerShape(12.dp))
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

                Row(
                    modifier = Modifier.align(Alignment.End), // 💥 ПРИЖИМАЕМ ВПРАВО (БЕЗ fillMaxWidth!)
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Статичное время (Потом заменим на реальное timestamp из БД)
                    Text(
                        text = "15:42",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )

                    // Сами галочки (только для твоих сообщений)
                    if (isOutgoing) {
                        Text(
                            text = if (message.isRead) "✓✓" else "✓",
                            color = if (message.isRead) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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