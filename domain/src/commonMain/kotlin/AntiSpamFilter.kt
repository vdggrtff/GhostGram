
object AntiSpamFilter {

    // 1. Фильтр алфавитов (арабский, китайский и т.д.)
    private val spamRegex = Regex("[\\u0600-\\u06FF\\u4E00-\\u9FA5]")

    fun isSpam(title: String, username: String): Boolean {
        return spamRegex.containsMatchIn(title) || spamRegex.containsMatchIn(username)
    }
}