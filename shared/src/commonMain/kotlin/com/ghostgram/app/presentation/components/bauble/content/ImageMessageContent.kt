package com.ghostgram.app.presentation.components.bauble.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import entity.Message

@Composable
fun ImageMessageContent(
    message: Message,
    onMediaClick: (String) -> Unit
) {
    val model = if (message.photoPath?.startsWith("/") == true) "file://${message.photoPath}" else message.photoPath

    Box(
        modifier = Modifier
            .widthIn(min = 160.dp, max = 280.dp) // Адаптивная ширина пузыря
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2330)) // Благородный темный фон
            .clickable {
                if (model != null) onMediaClick(model)
            },
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            val context = LocalPlatformContext.current

            // 💥 ДАУНСЕМПЛИНГ: декодируем текстуру максимум в 700x900px!
            // Это срезает 90% нагрузки на видеопамять и убивает оранжевый столб!
            val imageRequest = remember(model) {
                ImageRequest.Builder(context)
                    .data(model)
                    .size(Size(700, 900)) // 💥 Никаких 4K-текстур в видеопамяти!
                    .precision(Precision.INEXACT)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = "Фотография",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 360.dp)
            )
        }else {
            // 💥 ИНДИКАТОР ЗАГРУЗКИ (Если файл еще качается из Telegram)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (message.isSending) "Отправка..." else "Загрузка фото...",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}