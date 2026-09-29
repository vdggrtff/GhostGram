package com.ghostgram.app.presentation.components.bauble.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.utils.TimeFormatter
import entity.Message

@Composable
fun MessageTimeAndStatus(
    message: Message,
    textColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val timeString = TimeFormatter.formatTime(message.date)

    if (timeString.isNotBlank() || message.isOutgoing) {
        val mutedColor = textColor.copy(alpha = 0.6f)

        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (message.isEdited) {
                Text(
                    text = "изменено",
                    color = mutedColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Text(
                text = timeString,
                color = mutedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )

            if (message.isOutgoing) {
                Spacer(modifier = Modifier.width(3.dp))
                if (message.isSending) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = mutedColor,
                        modifier = Modifier.size(11.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = null,
                        tint = if (message.isRead) Color(0xFF4FC3F7) else iconColor.copy(alpha = 0.65f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}