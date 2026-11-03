package com.ghostgram.app.presentation.components.player

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.RenderMode

@Composable
actual fun TgsEffectPlayer(
    tgsJson: String,
    onFinished: () -> Unit,
    modifier: Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LottieAnimationView(context).apply {
                setRenderMode(RenderMode.HARDWARE)
                repeatCount = 0 // 💥 ИГРАЕМ РОВНО 1 РАЗ!
                addAnimatorListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onFinished() // Анимация завершилась -> убираем с экрана
                    }
                })
            }
        },
        update = { view ->
            val cacheKey = "effect_${tgsJson.hashCode()}"
            LottieCompositionFactory.fromJsonString(tgsJson, cacheKey)
                .addListener { composition ->
                    view.setComposition(composition)
                    view.playAnimation()
                }
        }
    )
}