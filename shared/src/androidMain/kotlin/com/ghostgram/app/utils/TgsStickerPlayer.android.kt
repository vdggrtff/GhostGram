package com.ghostgram.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.RenderMode

@Composable
actual fun TgsStickerPlayer(
    tgsJson: String,
    modifier: Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            /*LottieAnimationView(context).apply {
                // 💥 ГЛАВНЫЙ СЕКРЕТ СКОРОСТИ: Аппаратное ускорение на GPU!
                setRenderMode(RenderMode.HARDWARE)
                repeatCount = LottieDrawable.INFINITE
            }*/
            LottieAnimationView(context).apply {
                // AUTOMATIC сам выбирает самый легкий путь между GPU и CPU
                setRenderMode(RenderMode.AUTOMATIC)
                repeatCount = LottieDrawable.INFINITE
                // Отключаем лишнее кэширование системы, которое дублирует кадры
                enableMergePathsForKitKatAndAbove(false)
            }
        },
        update = { view ->
            // Кэшируем разобранный JSON по хэшу строки
            val cacheKey = tgsJson.hashCode().toString()
            LottieCompositionFactory.fromJsonString(tgsJson, cacheKey)
                .addListener { composition ->
                    view.setComposition(composition)
                    view.playAnimation()
                }
        }
    )
}