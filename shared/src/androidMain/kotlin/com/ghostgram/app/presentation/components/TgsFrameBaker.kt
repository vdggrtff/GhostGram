package com.ghostgram.app.presentation.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TgsFrameBaker {
    // 💥 Кэш готовых запеченных киноплёнок в RAM (держим до 8 активных стикеров на экране)
    private val frameCache = LruCache<String, List<ImageBitmap>>(8)

    fun getCachedFrames(cacheKey: String): List<ImageBitmap>? = frameCache.get(cacheKey)

    /**
     * Запекает векторную анимацию Lottie в плоские растровые кадры на фоновом потоке.
     * Вектор считается ровно 1 раз, дальше всё крутится "по инерции" с 0% CPU!
     */
    suspend fun bake(
        tgsJson: String,
        cacheKey: String,
        widthPx: Int = 260,
        heightPx: Int = 260,
        targetFps: Int = 30
    ): List<ImageBitmap>? = withContext(Dispatchers.Default) {
        frameCache.get(cacheKey)?.let { return@withContext it }

        try {
            // Парсим Lottie на фоновом ядре
            val composition = LottieCompositionFactory.fromJsonStringSync(tgsJson, cacheKey).value
                ?: return@withContext null

            val durationSec = (composition.duration / 1000f).coerceIn(1f, 4f)
            val totalFrames = (durationSec * targetFps).toInt().coerceIn(20, 60)
            val frameStep = composition.durationFrames / totalFrames

            val drawable = LottieDrawable().apply {
                setComposition(composition)
                bounds = Rect(0, 0, widthPx, heightPx)
            }

            val bakedFrames = ArrayList<ImageBitmap>(totalFrames)

            // Запекаем каждый кадр в растровую картинку
            for (i in 0 until totalFrames) {
                val targetFrame = (i * frameStep).toInt().coerceIn(0, composition.durationFrames.toInt() - 1)
                val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.frame = targetFrame
                drawable.draw(canvas)
                bakedFrames.add(bitmap.asImageBitmap())
            }

            frameCache.put(cacheKey, bakedFrames)
            bakedFrames
        } catch (e: Exception) {
            null
        }
    }
}