package com.ghostgram.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 🖤 Основные фоновые цвета (глубокий OLED-графит)
val GhostBackground = Color(0xFF0D0F17)        // Главный фон приложения
val GhostSurface = Color(0xFF141822)           // Фон карточек и пузырей входящих
val GhostSurfaceElevated = Color(0xFF1C2230)   // Фон диалогов и всплывающих меню
val GhostBorder = Color(0xFF252C3D)            // Тонкие рамки для эффекта стекла
val GhostCard = Color(0xFF1A1F2B)       // Чуть светлее для карточек чатов

// 💜 Фирменные Неоновые Акценты
val GhostPrimary = Color(0xFF7C4DFF)           // Яркий фиолетовый неон
val GhostSecureGreen = Color(0xFF00E676)
val GhostBadgeBg = Color(0xFF1E1E2A)
val GhostSecondary = Color(0xFF536DFE)         // Сине-индиго для градиентов
val GhostAccentGreen = Color(0xFF00E676)       // Неоновый зеленый (Online / E2EE Замок)
val GhostAccentRed = Color(0xFFFF5252)         // Неоновый красный (Anti-Revoke / Выход)
val GhostAccentOrange = Color(0xFFFF9100)      // Оранжевый (Служебные алерты)

// 📝 Текст и Типографика
val GhostTextPrimary = Color(0xFFFFFFFF)       // Основной белый текст
val GhostTextSecondary = Color(0xFF8E9BAE)     // Вторичный серо-синий текст
val GhostTextMuted = Color(0xFF536074)         // Приглушенный текст (время, даты)

// 🌈 Фирменные Градиенты
val GhostPrimaryGradient = Brush.horizontalGradient(
    listOf(GhostPrimary, GhostSecondary)
)

val GhostCardGradient = Brush.linearGradient(
    listOf(GhostSurface, GhostSurfaceElevated)
)