package entity

data class MyProfile(
    val firstName: String = "Ghost",
    val lastName: String = "",
    val username: String = "",
    val phoneNumber: String = "",
    val avatarPath: String? = null
) {
    val fullName: String get() = "$firstName $lastName".trim()
    val handle: String get() = if (username.isNotBlank()) "@$username" else ""
}