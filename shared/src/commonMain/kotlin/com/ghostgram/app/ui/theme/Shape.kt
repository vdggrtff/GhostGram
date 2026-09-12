package com.ghostgram.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val GhostShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),      // Карточки чатов и настройки
    large = RoundedCornerShape(24.dp),       // Поле ввода и модалки
    extraLarge = RoundedCornerShape(32.dp)    // Крупные панели
)