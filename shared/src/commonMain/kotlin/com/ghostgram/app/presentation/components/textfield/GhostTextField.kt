package com.ghostgram.app.presentation.components.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecondary
import com.ghostgram.app.ui.theme.GhostTextSecondary

@Composable
fun GhostTextField(
    inputText: String,
    isCryptoMode: Boolean,
    onIntent: (ChatDetailsIntent) -> Unit
){
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onIntent(ChatDetailsIntent.OnToggleCryptoMode) }
        ) {
            Text(
                text = if (isCryptoMode) "🔒" else "🔓",
                fontSize = 20.sp
            )
        }
    TextField(
        value = inputText,
        onValueChange = { onIntent(ChatDetailsIntent.OnInputChanged(it)) },
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
        maxLines = 4
    )
        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = { onIntent(ChatDetailsIntent.OnSendMessage) },
            enabled = inputText.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (inputText.isNotBlank())
                        Brush.horizontalGradient(listOf(GhostPrimary, GhostSecondary))
                    else
                        Brush.horizontalGradient(listOf(GhostCard, GhostCard))
                )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Отправить",
                tint = if (inputText.isNotBlank()) Color.White else GhostTextSecondary
            )
        }
    }
}