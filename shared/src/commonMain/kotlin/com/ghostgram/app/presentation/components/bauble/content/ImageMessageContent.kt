package com.ghostgram.app.presentation.components.bauble.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import entity.Message

@Composable
fun ImageMessageContent(
    message: Message,
    onMediaClick: (String) -> Unit
){
    val model = if (message.photoPath!!.startsWith("/")) "file://${message.photoPath}" else message.photoPath
    AsyncImage(
        model = model,
        contentDescription = "Фото",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onMediaClick(model ?: "") } // КЛИКАБЕЛЬНО!
    )
}