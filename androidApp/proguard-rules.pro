# ==============================================================================
# GHOSTGRAM RELEASE PROGUARD / R8 RULES
# ==============================================================================

# 💥 1. TDLib (C++ JNI ядро) — КРИТИЧНО! Без этого моментальный краш UnsatisfiedLinkError
-keep class org.drinkless.tdlib.** { *; }
-keepclassmembers class org.drinkless.tdlib.** { *; }
-dontwarn org.drinkless.tdlib.**

# 💥 2. Kotlinx Serialization (Защита JSON-парсера и моделей)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer();
}
-keep,allowobfuscation,allowshrinking class * {
    <fields>;
}

# 💥 3. Сущности данных и DAO (Room KMP, Domain, Database)
-keep class entity.** { *; }
-keep class com.ghostgram.domain.entity.** { *; }
-keep class com.ghostgram.core.database.entity.** { *; }
-keep class com.ghostgram.core.database.dao.** { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# 💥 4. Koin DI (Внедрение зависимостей без рефлексивных сбоев)
-keep class org.koin.** { *; }
-keepclassmembers class org.koin.** { *; }
-dontwarn org.koin.**

# 💥 5. Ktor Client & Okio (Сетевой стек и файловые потоки)
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn okio.**
-keep class okio.** { *; }

# 💥 6. Coil 3 & Compottie (Медиа и Lottie-стикеры)
-keep class coil3.** { *; }
-dontwarn coil3.**
-keep class io.github.alexzhirkevich.compottie.** { *; }
-dontwarn io.github.alexzhirkevich.compottie.**

# 💥 7. Compose Runtime
-keepclassmembers class androidx.compose.runtime.RecomposeScopeImpl { *; }