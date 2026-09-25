package com.ghostgram.app.presentation.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.presentation.chats.chat_details.MediaItem
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSurfaceElevated
import com.ghostgram.app.ui.theme.GhostTextSecondary

@Composable
fun PendingMediaDialog(
    pendingMedia: List<MediaItem>,
    sendAsDocument: Boolean,
    pendingCaption: String,
    onIntent: (ChatDetailsIntent) -> Unit
){
    Dialog(
        onDismissRequest = { onIntent(ChatDetailsIntent.OnCancelMediaSend) }
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GhostSurfaceElevated),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Отправить фото", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // Превью первой картинки (Coil умеет читать ByteArray!)
                AsyncImage(
                    model = pendingMedia.first().bytes,
                    contentDescription = "Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GhostBackground)
                )

                if (pendingMedia.size > 1) {
                    Text(
                        "И еще ${pendingMedia.size - 1} файлов",
                        color = GhostTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Галочка "Как файл"
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onIntent(ChatDetailsIntent.OnToggleSendAsDocument(!sendAsDocument)) }) {
                    Checkbox(
                        checked = sendAsDocument,
                        onCheckedChange = { onIntent(ChatDetailsIntent.OnToggleSendAsDocument(it)) },
                        colors = CheckboxDefaults.colors(checkedColor = GhostPrimary)
                    )
                    Text("Отправить как файл (без сжатия)", color = Color.White, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Поле подписи
                TextField(
                    value = pendingCaption,
                    onValueChange = { onIntent(ChatDetailsIntent.OnPendingCaptionChanged(it)) },
                    placeholder = { Text("Добавить подпись...", color = GhostTextSecondary) },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GhostCard,
                        unfocusedContainerColor = GhostCard,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопки
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onIntent(ChatDetailsIntent.OnCancelMediaSend) }) {
                        Text("Отмена", color = GhostTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onIntent(ChatDetailsIntent.OnConfirmMediaSend) },
                        colors = ButtonDefaults.buttonColors(containerColor = GhostPrimary)
                    ) {
                        Text("Отправить", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}