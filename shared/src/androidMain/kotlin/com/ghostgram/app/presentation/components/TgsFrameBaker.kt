package com.ghostgram.app.presentation.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.ceil

data class BakedTgsAnimation(
    val frames: List<ImageBitmap>,
    val durationMs: Long
)

object TgsFrameBaker {

    private const val TAG = "TgsFrameBaker"

    private const val MAX_CACHE_BYTES = 24 * 1024 * 1024

    private const val MAX_FRAMES = 90

    private val frameCache =
        object : LruCache<String, BakedTgsAnimation>(
            MAX_CACHE_BYTES
        ) {
            override fun sizeOf(
                key: String,
                value: BakedTgsAnimation
            ): Int {
                val frame = value.frames.firstOrNull()
                    ?: return 0

                val bytes = value.frames.size.toLong() *
                        frame.width.toLong() *
                        frame.height.toLong() * 4L

                return bytes.coerceAtMost(
                    Int.MAX_VALUE.toLong()
                ).toInt()
            }
        }

    private val bakeMutex = Mutex()

    fun getCachedFrames(
        cacheKey: String
    ): BakedTgsAnimation? {
        return frameCache.get(cacheKey)
    }

    suspend fun bake(
        tgsJson: String,
        cacheKey: String,
        widthPx: Int = 260,
        heightPx: Int = 260,
        targetFps: Int = 30
    ): BakedTgsAnimation? {

        frameCache.get(cacheKey)?.let {
            return it
        }

        return bakeMutex.withLock {
            currentCoroutineContext().ensureActive()

            // Пока ожидали блокировку, другой стикер
            // мог завершить запекание.
            frameCache.get(cacheKey)?.let {
                return@withLock it
            }

            try {
                val animation = withContext(Dispatchers.Default) {
                    render(
                        tgsJson = tgsJson,
                        cacheKey = cacheKey,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        targetFps = targetFps
                    )
                }

                if (animation != null) {
                    frameCache.put(cacheKey, animation)
                }

                animation
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Failed to bake TGS: $cacheKey",
                    e
                )

                null
            }
        }
    }

    private suspend fun render(
        tgsJson: String,
        cacheKey: String,
        widthPx: Int,
        heightPx: Int,
        targetFps: Int
    ): BakedTgsAnimation? {

        require(widthPx > 0 && heightPx > 0) {
            "Bitmap dimensions must be positive"
        }

        val composition =
            LottieCompositionFactory
                .fromJsonStringSync(tgsJson, cacheKey)
                .value
                ?: return null

        // Не обрезаем длительность до 1–4 секунд.
        // Используем реальную продолжительность композиции.
        val durationMs = composition.duration
            .toLong()
            .coerceAtLeast(1L)

        val fps = targetFps.coerceIn(1, 60)

        // Количество кадров зависит от настоящей длительности.
        // Верхний предел ограничивает расход памяти.
        val totalFrames = ceil(
            durationMs.toDouble() * fps / 1000.0
        ).toInt().coerceIn(1, MAX_FRAMES)

        val startFrame = composition.startFrame
        val endFrame = composition.endFrame

        val drawable = LottieDrawable().apply {
            setComposition(composition)
            bounds = Rect(0, 0, widthPx, heightPx)
        }

        val frames = ArrayList<ImageBitmap>(totalFrames)

        try {
            for (i in 0 until totalFrames) {
                currentCoroutineContext().ensureActive()

                // Равномерно выбираем кадры по всей анимации.
                // Не включаем конечную точку повторно,
                // чтобы уменьшить заметный скачок при зацикливании.
                val progress = i.toFloat() / totalFrames

                val frame = (
                        startFrame +
                                (endFrame - startFrame) * progress
                        ).toInt().coerceIn(
                        startFrame.toInt(),
                        endFrame.toInt().coerceAtLeast(
                            startFrame.toInt()
                        )
                    )

                val bitmap = Bitmap.createBitmap(
                    widthPx,
                    heightPx,
                    Bitmap.Config.ARGB_8888
                )

                drawable.frame = frame
                drawable.draw(Canvas(bitmap))

                frames.add(bitmap.asImageBitmap())
            }

            return BakedTgsAnimation(
                frames = frames,
                durationMs = durationMs
            )
        } catch (e: CancellationException) {
            // Если запекание отменено, освобождаем созданные кадры.
            frames.forEach { image ->
                image.asAndroidBitmap().recycle()
            }

            throw e
        }
    }
}