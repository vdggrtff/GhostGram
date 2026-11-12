package entity

data class SafetyFingerprint(
    val emojis: List<String>,      // 4 эмодзи безопасности
    val formattedHex: String,      // 64-значный хэш "a1b2 c3d4..."
    val isKeyPinned: Boolean = true
)