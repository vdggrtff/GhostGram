package com.ghostgram.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

object TelegramColors {
    // 7 ТОЧНЫХ ЦВЕТОВ ИМЕН ИЗ ОФИЦИАЛЬНОГО TELEGRAM
    val userColors = listOf(
        Color(0xFFE56555), // Красный
        Color(0xFFF28537), // Оранжевый
        Color(0xFF8E85EE), // Фиолетовый
        Color(0xFF4FAE4E), // Зеленый
        Color(0xFF4F9CD9), // Бирюзовый
        Color(0xFF3390EC), // Синий
        Color(0xFFD3559F)  // Розовый
    )

    // Вычисляет уникальный цвет для каждого юзера
    fun getColorForUser(userId: Long): Color {
        if (userId == 0L) return userColors[0]
        val index = (userId.absoluteValue % userColors.size).toInt()
        return userColors[index]
    }
}