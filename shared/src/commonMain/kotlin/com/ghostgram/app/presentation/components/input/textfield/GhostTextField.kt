package com.ghostgram.app.presentation.components.input.textfield

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.AutoMirrored.Filled
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnInputChanged
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSendMessage
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecondary
import com.ghostgram.app.ui.theme.GhostSurfaceElevated
import com.ghostgram.app.ui.theme.GhostTextSecondary
import entity.Message
import io.github.vinceglb.filekit.compose.PickerResultLauncher

@Composable
fun GhostTextField(
    inputText: String,
    onIntent: (ChatDetailsIntent) -> Unit,
    replyingToMessage: Message?,
    fileLauncher: PickerResultLauncher
) {
    if (replyingToMessage != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GhostSurfaceElevated)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Синяя полоска слева
            Box(modifier = Modifier.width(3.dp).height(32.dp).background(GhostPrimary, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(replyingToMessage.senderName, color = GhostPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = replyingToMessage.text.ifBlank { "Медиафайл" },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { onIntent(ChatDetailsIntent.OnCancelReply) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Отмена", tint = GhostTextSecondary)
            }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { fileLauncher.launch() }, // 💥 Открываем системный выбор файлов
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Icon(Icons.Default.AttachFile, contentDescription = "Прикрепить", tint = GhostTextSecondary)
        }
        TextField(
            value = inputText,
            onValueChange = { onIntent(OnInputChanged(it)) },
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
            maxLines = 5,
            // Иконка эмодзи внутри поля ввода (справа)
            trailingIcon = {
                Icon(
                    // Замени на нужную иконку смайлика
                    imageVector = Outlined.Face,
                    contentDescription = "Эмодзи",
                    tint = GhostTextSecondary,
                    modifier = Modifier.padding(end = 8.dp).size(22.dp)
                )
            }
        )
        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = { onIntent(OnSendMessage) },
            enabled = inputText.isNotBlank(),
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(GhostPrimary) // На макете она залита сплошным фиолетовым
        ) {
            Icon(
                imageVector = Filled.Send,
                contentDescription = "Отправить",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}