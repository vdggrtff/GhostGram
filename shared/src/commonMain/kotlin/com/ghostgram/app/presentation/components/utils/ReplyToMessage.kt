package com.ghostgram.app.presentation.components.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary
import entity.Message

@Composable
fun ReplyToMessage(
    replyMessage: Message?,
    isOutgoing: Boolean
){
// Если оригинал есть в памяти - берем его данные, иначе пишем заглушку
    val replySenderName = replyMessage?.senderName ?: "Сообщение"
    val replyText = replyMessage?.text?.ifBlank { "Медиафайл" } ?: "Загрузка..."

    // Цвета зависят от того, исходящий ли это пузырь
    val accentColor = if (isOutgoing) Color.White else GhostPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(accentColor.copy(alpha = 0.1f)) // Легкий фон под цитатой
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Вертикальная полоска
        Box(modifier = Modifier.width(3.dp).height(36.dp).background(accentColor))
        Spacer(modifier = Modifier.width(8.dp))

        // Текст цитаты
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(text = replySenderName, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                text = replyText,
                color = if (isOutgoing) Color.White.copy(alpha = 0.8f) else GhostTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}