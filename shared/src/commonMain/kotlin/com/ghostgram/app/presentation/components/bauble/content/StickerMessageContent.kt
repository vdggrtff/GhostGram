package com.ghostgram.app.presentation.components.bauble.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.utils.TgsDecoder
import com.ghostgram.app.utils.TgsStickerPlayer
import com.ghostgram.app.utils.WebmStickerPlayer
import entity.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

@Composable
fun StickerMessageContent(message: Message) {
    val thumbPath = message.photoPath
    val videoOrTgsPath = message.fileName

    if (thumbPath.isNullOrBlank() && videoOrTgsPath.isNullOrBlank()) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = GhostPrimary)
        }
        return
    }

    // 💥 1. ВИДЕО-СТИКЕР .WEBM (Твой зацикленный нативный плеер на Android):
    if (videoOrTgsPath != null && videoOrTgsPath.endsWith(".webm", ignoreCase = true)) {
        WebmStickerPlayer(filePath = videoOrTgsPath, modifier = Modifier.size(140.dp))
        return
    }

    // 💥 2. АНИМИРОВАННЫЙ СТИКЕР .TGS (ПОЛНОЦЕННАЯ АНИМАЦИЯ БЕЗ ТОРМОЗОВ):
    val isTgs = (videoOrTgsPath != null && videoOrTgsPath.endsWith(".tgs", ignoreCase = true)) ||
            (thumbPath != null && thumbPath.endsWith(".tgs", ignoreCase = true))

    if (isTgs) {
        val tgsFilePath = (videoOrTgsPath?.takeIf { it.endsWith(".tgs", true) }
            ?: thumbPath?.takeIf { it.endsWith(".tgs", true) } ?: "")
        val cleanPath = if (tgsFilePath.startsWith("file://")) tgsFilePath.removePrefix("file://") else tgsFilePath

        // Моментально берем JSON из кэша, если он уже распаковывался
        var tgsJson by remember(cleanPath) { mutableStateOf(TgsDecoder.getJsonFromCache(cleanPath)) }

        LaunchedEffect(cleanPath) {
            if (tgsJson == null) {
                tgsJson = withContext(Dispatchers.IO) {
                    TgsDecoder.decodeTgsToJson(cleanPath)
                }
            }
        }

        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            // 💥 1. ПОДЛОЖКА: если векторный JSON еще не готов — мгновенно показываем статичную превьюшку .webp!
            // Пользователь видит стикер за 0.001 мс, список летит со скоростью 120 FPS!
            if (tgsJson == null && !thumbPath.isNullOrBlank()) {
                val previewUrl = if (thumbPath.startsWith("/")) "file://$thumbPath" else thumbPath
                AsyncImage(
                    model = previewUrl,
                    contentDescription = null,
                    modifier = Modifier.size(140.dp)
                )
            }

            // 💥 2. ЖИВАЯ АНИМАЦИЯ: бесшовно появляется поверх картинки, как только готова!
            if (tgsJson != null) {
                TgsStickerPlayer(
                    tgsJson = tgsJson!!,
                    modifier = Modifier.size(140.dp)
                )
            }
        }
        return
    }

    // 💥 3. СТАТИЧНЫЙ СТИКЕР .WEBP:
    if (!thumbPath.isNullOrBlank()) {
        val cleanPath = if (thumbPath.startsWith("file://")) thumbPath else "file://$thumbPath"
        AsyncImage(model = cleanPath, contentDescription = null, modifier = Modifier.size(140.dp))
    }
}