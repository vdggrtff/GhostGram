package com.ghostgram.data.repository.utils

object SearchStemmer {
    // Список распространенных русских окончаний (от длинных к коротким)
    private val russianEndings = listOf(
        "ому", "ему", "ыми", "ими", "ого", "его", "ной", "ная", "ное", "ные", "ных",
        "ами", "ями", "ьей", "ьям", "ьях", "ьев",
        "ам", "ям", "ах", "ях", "ов", "ев", "ей", "ой", "ей", "ом", "ем",
        "а", "е", "и", "й", "о", "у", "ы", "ь", "я"
    )

    /**
     * Срезает окончание, если слово длиннее 4 букв (собака -> собак, машине -> машин)
     */
    fun trimEnding(word: String): String {
        val clean = word.trim().lowercase()
        // Короткие слова (кот, дом, ку, чат) не трогаем!
        if (clean.length < 5) return clean

        for (ending in russianEndings) {
            if (clean.endsWith(ending)) {
                val stem = clean.removeSuffix(ending)
                if (stem.length >= 4) return stem
            }
        }
        return clean
    }
}