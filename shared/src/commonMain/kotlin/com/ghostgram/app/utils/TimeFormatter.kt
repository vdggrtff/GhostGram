package com.ghostgram.app.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

object TimeFormatter {
    private val months = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    // Форматируем время для пузыря (например, "15:42")
    fun formatTime(unixSeconds: Int): String {
        if (unixSeconds == 0) return ""
        val instant = Instant.fromEpochSeconds(unixSeconds.toLong())
        val localTime = instant.toLocalDateTime(TimeZone.currentSystemDefault()).time
        val h = localTime.hour.toString().padStart(2, '0')
        val m = localTime.minute.toString().padStart(2, '0')
        return "$h:$m"
    }

    // Форматируем плашку даты (например, "Сегодня", "Вчера", "9 сентября")
    fun formatDateHeader(unixSeconds: Int): String {
        if (unixSeconds == 0) return ""
        val instant = Instant.fromEpochSeconds(unixSeconds.toLong())
        val msgDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val daysDiff = today.toEpochDays() - msgDate.toEpochDays()

        return when (daysDiff) {
            0L -> "Сегодня"
            1L -> "Вчера"
            else -> "${msgDate.day} ${months[msgDate.month.number - 1]}"
        }
    }

    // Проверка: это один и тот же день?
    fun isSameDay(unix1: Int, unix2: Int): Boolean {
        val d1 = Instant.fromEpochSeconds(unix1.toLong()).toLocalDateTime(TimeZone.currentSystemDefault()).date
        val d2 = Instant.fromEpochSeconds(unix2.toLong()).toLocalDateTime(TimeZone.currentSystemDefault()).date
        return d1 == d2
    }
}