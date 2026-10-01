package com.ghostgram.app.presentation.components.bauble.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.utils.openFileInSystem
import entity.Message
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

@Composable
fun DocumentMessageContent(
    message: Message
){
    val fileName = message.fileName ?: "Файл"
    val extension = fileName.substringAfterLast('.', "").uppercase().take(4)
    val sizeStr = message.fileExtraInfo ?: ""

    val isDownloaded = remember(message.photoPath) {
        !message.photoPath.isNullOrBlank() && runCatching {
            FileSystem.SYSTEM.exists(message.photoPath!!.toPath())
        }.getOrDefault(false)
    }

    val badgeColor = when (extension) {
        "APK" -> Color(0xFF43A047)
        "PDF" -> Color(0xFFE53935)
        "ZIP", "RAR", "7Z" -> Color(0xFFFB8C00)
        else -> GhostPrimary
    }

    Row(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .clickable {
                // 💥 КЛИК: ОТКРЫВАЕМ ФАЙЛ В СИСТЕМЕ!
                if (isDownloaded && !message.photoPath.isNullOrBlank()) {
                    openFileInSystem(message.photoPath!!)
                }
            }
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