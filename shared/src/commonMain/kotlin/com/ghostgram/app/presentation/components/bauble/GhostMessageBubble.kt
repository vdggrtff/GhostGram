package com.ghostgram.app.presentation.components.bauble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
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
import com.ghostgram.app.ui.theme.GhostAccentRed
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecondary
import com.ghostgram.app.ui.theme.GhostSecureGreen
import com.ghostgram.app.utils.TimeFormatter
import entity.Message
import entity.MessageMediaType

@Composable
fun GhostMessageBubble(
    message: Message,
    chatAvatarPath: String?,
    myAvatarPath: String?, // 💥 Вернули твою аватарку!
    chatTitle: String,
    onMediaClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOutgoing = message.isOutgoing
    val timeString = TimeFormatter.formatTime(message.date)

    if (message.fileExtraInfo == "SYSTEM") {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.text,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        return // 💥 Выходим из функции, чтобы не рисовать синий пузырь!
    }

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
                    modifier = Modifier.size(34.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFE58235)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chatTitle.take(1).uppercase().ifBlank { "💬" },
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
                .widthIn(
                    min = 80.dp,
                    max = 280.dp
                ) // Ограничиваем только максимум, чтобы он сжимался под текст
                .clip(bubbleShape)
                .background(
                    if (isOutgoing)
                        Brush.linearGradient(listOf(GhostPrimary, GhostSecondary))
                    else
                        Brush.linearGradient(listOf(GhostCard, GhostCard))
                )
                .padding(
                    start = 12.dp,
                    top = 8.dp,
                    end = 12.dp,
                    bottom = 6.dp
                ) // Снизу отступ чуть меньше для красивых галочек
        ) {
            Column(
                modifier = Modifier.wrapContentWidth(), // 💥 Пузырь плотно облегает контент
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                when (message.mediaType) {
                    MessageMediaType.PHOTO -> {
                        if (message.photoPath != null) {
                            val model =
                                if (message.photoPath!!.startsWith("/")) "file://${message.photoPath}" else message.photoPath
                            AsyncImage(
                                model = model,
                                contentDescription = "Фото",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onMediaClick(model ?: "") } // 💥 КЛИКАБЕЛЬНО!
                            )
                        }
                    }

                    MessageMediaType.VIDEO, MessageMediaType.VIDEO_NOTE -> {
                        if (message.photoPath != null) {
                            val model =
                                if (message.photoPath!!.startsWith("/")) "file://${message.photoPath}" else message.photoPath
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onMediaClick(model ?: "") }, // 💥 КЛИКАБЕЛЬНО!
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = model,
                                    contentDescription = "Превью",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f))
                                )
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("▶", color = Color.White, fontSize = 20.sp)
                                }
                                Text(
                                    text = message.fileExtraInfo ?: "0:00",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.5f),
                                            RoundedCornerShape(4.dp)
                                        ).padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    MessageMediaType.DOCUMENT -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.2f)).padding(8.dp),
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
                                    text = message.fileName ?: "Файл",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = message.fileExtraInfo ?: "Документ",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    MessageMediaType.STICKER -> {
                        if (message.photoPath != null) {
                            val model =
                                if (message.photoPath!!.startsWith("/")) "file://${message.photoPath}" else message.photoPath
                            AsyncImage(
                                model = model,
                                contentDescription = "Стикер",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(140.dp)
                            )
                        }
                    }
                    MessageMediaType.VOICE -> {
                        // ... твой виджет голосового ...
                    }

                    MessageMediaType.TEXT -> { /* Обычный текст, ничего не делаем */ }
                }

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

                if (message.text.isNotBlank()) {
                    Text(
                        text = if (message.fileExtraInfo == "ENCRYPTED") "🔒 ${message.text}" else message.text,
                        color = if (message.fileExtraInfo == "ENCRYPTED") GhostSecureGreen else Color.White,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = if (message.mediaType == MessageMediaType.STICKER) 10.dp else 0.dp)
                    )
                }

                // 🗑 Метка Anti-Revoke
                if (message.isDeletedLocally) {
                    Text(text = "🗑️ Удалено отправителем", color = GhostAccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (timeString.isNotBlank() || isOutgoing) {
                    Row(
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeString,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )

                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = "Статус",
                                tint = if (message.isRead) Color(0xFF4FC3F7) else Color.White.copy(
                                    alpha = 0.6f
                                ),
                                modifier = Modifier.size(16.dp)
                            )
                        }
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