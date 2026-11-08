package com.ghostgram.app.presentation.components.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun FlyingReactionOverlay(
    emoji: String?,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (emoji == null) return

    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(1f) }
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(-12f) }

    LaunchedEffect(emoji) {
        // 1. Пружинящий вылет и надувание
        launch {
            scale.animateTo(
                targetValue = 1.35f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            scale.animateTo(1.7f, tween(250))
        }

        // 2. Игривое покачивание
        launch {
            rotation.animateTo(14f, tween(350))
            rotation.animateTo(0f, tween(250))
        }

        // 3. Полет вверх
        launch {
            offsetY.animateTo(-160f, tween(650, easing = FastOutSlowInEasing))
        }

        // 4. Плавное растворение в конце
        launch {
            alpha.animateTo(1f, tween(400))
            alpha.animateTo(0f, tween(250))
            onFinished()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 84.sp,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationY = offsetY.value
                    rotationZ = rotation.value
                    this.alpha = alpha.value
                }
        )
    }
}