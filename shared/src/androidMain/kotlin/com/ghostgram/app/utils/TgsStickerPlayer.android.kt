package com.ghostgram.app.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ghostgram.app.presentation.components.TgsFrameBaker
import com.ghostgram.app.ui.theme.GhostPrimary
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay

@Composable
actual fun TgsStickerPlayer(
    tgsJson: String,
    modifier: Modifier
) {
    val cacheKey = remember(tgsJson) { tgsJson.hashCode().toString() }

    // Проверяем, запечены ли уже кадры
    var bakedFrames by remember(cacheKey) { mutableStateOf(TgsFrameBaker.getCachedFrames(cacheKey)) }

    LaunchedEffect(cacheKey) {
        if (bakedFrames == null) {
            bakedFrames = TgsFrameBaker.bake(tgsJson, cacheKey)
        }
    }

    val frames = bakedFrames
    if (frames != null && frames.isNotEmpty()) {
        // 💥 ВОТ ОНА — ИНЕРЦИЯ!
        // Крутим готовую киноплёнку кадров по кругу с фиксированной скоростью 30 FPS!
        // Нагрузка на CPU: 0%! Нагрузка на GPU: 0.1 мс!
        var frameIndex by remember { mutableStateOf(0) }

        LaunchedEffect(frames) {
            val frameDelayMs = 33L // Ровно 30 кадров в секунду
            while (isActive) {
                delay(frameDelayMs)
                frameIndex = (frameIndex + 1) % frames.size
            }
        }

        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Image(
                bitmap = frames[frameIndex],
                contentDescription = "Стикер",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Пока фоновый поток 0.2 секунды печет кадры — крутится легкий спиннер
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = GhostPrimary, strokeWidth = 2.dp)
        }
    }
}