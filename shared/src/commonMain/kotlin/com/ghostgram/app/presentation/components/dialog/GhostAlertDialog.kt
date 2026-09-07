package com.ghostgram.app.presentation.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextPrimary

@Composable
fun GhostAlertDialog(
    catchUpSummary: String?,
    onIntent: (ChatDetailsIntent) -> Unit
){
    AlertDialog(
        onDismissRequest = { onIntent(ChatDetailsIntent.OnDismissCatchUpDialog) },
        containerColor = GhostCard,
        title = {
            Text(
                "⚡️ What You Missed",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                catchUpSummary!!,
                color = GhostTextPrimary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = { onIntent(ChatDetailsIntent.OnDismissCatchUpDialog) }) {
                Text("Got it", color = GhostPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}