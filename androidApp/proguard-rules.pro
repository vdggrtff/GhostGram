# ==============================================================================
# GHOSTGRAM RELEASE PROGUARD / R8 RULES
# ==============================================================================

# 💥 1. TDLib (C++ JNI ядро) — КРИТИЧНО! Без этого моментальный краш UnsatisfiedLinkError
-keep class org.drinkless.tdlib.** { *; }
-keepclassmembers class org.drinkless.tdlib.** { *; }
-dontwarn org.drinkless.tdlib.**

# 💥 2. Airbnb Lottie (Наш аппаратный движок стикеров)
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# 💥 3. Kotlinx Serialization (Чтобы JSON не превращался в кашу)
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
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

# 💥 4. Доменные сущности и База данных Room
-keep class entity.** { *; }
-keep class com.ghostgram.domain.entity.** { *; }
-keep class com.ghostgram.core.database.entity.** { *; }
-keep class com.ghostgram.core.database.dao.** { *; }

# 💥 5. Koin Dependency Injection
-keep class org.koin.** { *; }
-keepclassmembers class org.koin.** { *; }
-dontwarn org.koin.**

# 💥 6. Сеть Ktor и Okio
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn okio.**

# 💥 7. Медиа (Coil 3 и Compottie)
-keep class coil3.** { *; }
-dontwarn coil3.**
-keep class io.github.alexzhirkevich.compottie.** { *; }
-dontwarn io.github.alexzhirkevich.compottie.**

# 💥 8. Корутины и диспетчеры
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# 💥 9. Compose Runtime
-keepclassmembers class androidx.compose.runtime.RecomposeScopeImpl { *; }