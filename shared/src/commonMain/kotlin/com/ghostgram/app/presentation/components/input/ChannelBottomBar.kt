package com.ghostgram.app.presentation.components.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary

@Composable
fun ChannelBottomBar(
    isMuted: Boolean,
    onToggleMute: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhostCard)
            .navigationBarsPadding()
            .clickable(onClick = onToggleMute)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                contentDescription = null,
                tint = if (isMuted) Color.White.copy(alpha = 0.7f) else GhostPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isMuted) "ВКЛЮЧИТЬ ЗВУК" else "ОТКЛЮЧИТЬ ЗВУК",
                color = if (isMuted) Color.White.copy(alpha = 0.7f) else GhostPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}