package com.ghostgram.app.presentation.components.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val angle: Double,
    val maxDistance: Float,
    val rotationTarget: Float,
    val sizeSp: TextUnit,
    val scaleMultiplier: Float
)

@Composable
fun MessageParticleBurst(
    emoji: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 💥 16 разнокалиберных микро-эмодзи с разлетом веером
    val particles = remember(emoji) {
        val sizes = listOf(16.sp, 20.sp, 24.sp, 26.sp)
        List(16) { index ->
            val angleDeg = index * (360.0 / 16) + Random.nextDouble(-12.0, 12.0)
            Particle(
                angle = angleDeg * (PI / 180.0),
                maxDistance = Random.nextFloat() * 55f + 40f, // радиус разлета 40-95dp
                rotationTarget = Random.nextFloat() * 140f - 70f,
                sizeSp = sizes[index % sizes.size],
                scaleMultiplier = Random.nextFloat() * 0.35f + 0.85f
            )
        }
    }

    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(emoji) {
        // 💥 ДЛИТЕЛЬНОСТЬ 1050мс + EaseOutCubic: резкий выстрел и медленное зависание в воздухе!
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1050, easing = EaseOutCubic)
        )
        onFinished()
    }

    val progress = progressAnim.value

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        particles.forEach { particle ->
            // Радиальный разлет + облачко плавно дрейфует вверх на 35dp
            val distance = progress * particle.maxDistance
            val upwardDrift = progress * 35f
            val x = (cos(particle.angle) * distance).dp
            val y = (sin(particle.angle) * distance - upwardDrift).dp

            // Быстрый рост до максимума на первых 20% пути, потом мягкое уменьшение
            val currentScale = if (progress < 0.2f) {
                (progress / 0.2f) * particle.scaleMultiplier
            } else {
                (1f - (progress - 0.2f) * 0.45f) * particle.scaleMultiplier
            }

            // 💥 ЧАСТИЦЫ ВИДНЫ ДОЛГО: начинают плавно исчезать только после 65% времени!
            val alpha = if (progress < 0.65f) {
                1f
            } else {
                (1f - (progress - 0.65f) / 0.35f).coerceIn(0f, 1f)
            }

            Text(
                text = emoji,
                fontSize = particle.sizeSp,
                modifier = Modifier
                    .offset { IntOffset(x.roundToPx(), y.roundToPx()) }
                    .scale(currentScale.coerceAtLeast(0f))
                    .rotate(progress * particle.rotationTarget)
                    .alpha(alpha)
            )
        }
    }
}