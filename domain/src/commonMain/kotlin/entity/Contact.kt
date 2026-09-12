package entity

data class Contact(
    val userId: Long,
    val firstName: String,
    val lastName: String,
    val username: String? = null,
    val phoneNumber: String = "",
    val avatarPath: String? = null,
    val isOnline: Boolean = false,
    val statusText: String = ""
) {
    val fullName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Без имени" }

    // Первая буква имени для заголовка секции (А, Б, В...)
    val initialLetter: Char
        get() = fullName.firstOrNull()?.uppercaseChar() ?: '#'
}