package com.ghostgram.app.presentation.components.bauble.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.ui.theme.TelegramColors
import entity.Message

@Composable
fun SenderAvatarBadge(message: Message) {
    val avatarToShow = message.senderAvatarPath
    val authorName = message.senderName.ifBlank { "Участник" }
    val authorColor = TelegramColors.getColorForUser(message.senderId)

    key(avatarToShow, authorName) {
        if (!avatarToShow.isNullOrBlank() && !avatarToShow.startsWith("INITIALS:")) {
            val model = if (avatarToShow.startsWith("/")) "file://$avatarToShow" else avatarToShow
            AsyncImage(
                model = model,
                contentDescription = "Аватарка автора",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(34.dp).clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(authorColor),
                contentAlignment = Alignment.Center
            ) {
                Text(text = authorName.take(1).uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SystemMessageBadge(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

fun calculateBubbleShape(isOutgoing: Boolean, isFirstInGroup: Boolean, isLastInGroup: Boolean): RoundedCornerShape {
    val corner = 10.dp
    val sharp = 2.dp
    return if (isOutgoing) {
        RoundedCornerShape(
            topStart = corner,
            topEnd = if (isFirstInGroup) corner else sharp,
            bottomStart = corner,
            bottomEnd = if (isLastInGroup) sharp else corner
        )
    } else {
        RoundedCornerShape(
            topStart = if (isFirstInGroup) corner else sharp,
            topEnd = corner,
            bottomStart = if (isLastInGroup) sharp else corner,
            bottomEnd = corner
        )
    }
}

fun calculateBubbleColor(message: Message, isOutgoing: Boolean): Color {
    return when {
        message.fileExtraInfo == "ENCRYPTED" -> Color(0xFF1A4A38)
        isOutgoing -> Color(0xFF5274E8)
        else -> Color(0xFF18222D)
    }
}