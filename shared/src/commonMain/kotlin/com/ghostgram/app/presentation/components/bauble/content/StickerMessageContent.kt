package com.ghostgram.app.presentation.components.bauble.content

import androidx.compose.foundation.Image
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
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.utils.TgsDecoder
import entity.Message
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

@Composable
fun StickerMessageContent(
    message: Message,
){
// 1. Состояние для распакованного JSON
    var tgsJson by remember { mutableStateOf<String?>(null) }

    // 2. Распаковываем файл в фоне при появлении на экране
    LaunchedEffect(message.photoPath) {
        tgsJson = TgsDecoder.decodeTgsToJson(message.photoPath!!)
    }
    if (tgsJson != null) {
        // 1. Парсим JSON
        val composition by rememberLottieComposition(
            spec = LottieCompositionSpec.JsonString(tgsJson!!)
        )

        // 2. Создаем независимый стейт анимации (Крутим бесконечно)
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = Int.MAX_VALUE // Вместо красного Compottie.IterateForever
        )

        // 3. Передаем прогресс в отрисовщик
        val painter = rememberLottiePainter(
            composition = composition,
            progress = { progress }
        )

        Image(
            painter = painter,
            contentDescription = "Анимированный стикер",
            modifier = Modifier.size(140.dp)
        )
    } else {
        // Лоадер, пока распаковывается GZIP
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(
                    24.dp
                ), color = GhostPrimary
            )
        }
    }
}