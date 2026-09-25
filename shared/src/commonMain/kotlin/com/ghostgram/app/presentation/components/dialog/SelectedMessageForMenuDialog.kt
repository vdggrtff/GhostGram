package com.ghostgram.app.presentation.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.ui.theme.GhostAccentRed
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostTextSecondary
import entity.Message
import entity.MessageMediaType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun SelectedMessageForMenuDialog(
    msg: Message,
    clipboardManager: ClipboardManager,
    onIntent: (ChatDetailsIntent) -> Unit,
    onDismiss: () -> Unit
){
    val coroutineScope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GhostCard,
        title = { Text("Действия", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                // Копировать (Только если есть текст)
                if (msg.text.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                coroutineScope.launch {
                                    clipboardManager.setText(AnnotatedString(msg.text))
                                    //clipboardManager.getClipEntry()
                                }
                                onDismiss()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📋", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Скопировать текст", color = Color.White, fontSize = 16.sp)
                    }
                }

                if (msg.isOutgoing && msg.mediaType == MessageMediaType.TEXT) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onIntent(ChatDetailsIntent.OnEditMessageClick(msg))
                                onDismiss()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Редактировать", color = Color.White, fontSize = 16.sp)
                    }
                }

                // Удалить у всех (Revoke)
                if (!msg.isDeletedLocally) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onIntent(ChatDetailsIntent.OnDeleteMessage(msg.id, revoke = true))
                                onDismiss()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🗑", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Удалить у всех", color = GhostAccentRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Отмена", color = GhostTextSecondary)
            }
        }
    )
}
