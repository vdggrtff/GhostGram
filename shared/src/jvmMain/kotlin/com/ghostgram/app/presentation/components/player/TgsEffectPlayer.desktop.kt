package com.ghostgram.app.presentation.components.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.*
import kotlinx.coroutines.delay

@Composable
actual fun TgsEffectPlayer(
    tgsJson: String,
    onFinished: () -> Unit,
    modifier: Modifier
) {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.JsonString(tgsJson))

    val animState = animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = composition != null
    )

    // 💥 1. ЖЕЛЕЗНЫЙ ТАЙМАУТ: если анимация зависнет или не стартанет — закрываем через 1.5 сек!
    LaunchedEffect(tgsJson) {
        delay(1300)
        onFinished()
    }

    // 💥 2. ПРАВИЛЬНЫЙ ФИНИШ: завершаем ТОЛЬКО когда анимация РЕАЛЬНО отыграла от 0 до 100%!
    LaunchedEffect(composition, animState.isAtEnd, animState.value) {
        if (composition != null && animState.isAtEnd && animState.value >= 0.95f) {
            println("🎆 [DESKTOP EFFECT] Анимация эффекта завершена!")
            onFinished()
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (composition != null) {
            val painter = rememberLottiePainter(
                composition = composition,
                progress = { animState.value }
            )
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(130.dp)
            )
        }
    }
}