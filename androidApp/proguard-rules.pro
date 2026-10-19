# GhostGram R8 / ProGuard Configuration (v0.2.0)
# ==============================================================================

# TDLib Core & Native bindings
-dontwarn org.drinkless.tdlib.**
-keep class org.drinkless.tdlib.** { *; }
-keepclassmembers class org.drinkless.tdlib.** { *; }

# Application modules & models
-keep class com.ghostgram.** { *; }
-keepclassmembers class com.ghostgram.** { *; }
-keep class entity.** { *; }
-keepclassmembers class entity.** { *; }
-keep class repository.** { *; }
-keepclassmembers class repository.** { *; }
-keep class usecase.** { *; }
-keepclassmembers class usecase.** { *; }

# Keep native C++ method signatures for JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Kotlin Coroutines & Flow
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# Serialization (JSON)
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-dontnote kotlinx.serialization.**

# Koin DI
-keep class org.koin.** { *; }
-keepclassmembers class org.koin.** { *; }
-dontwarn org.koin.**

# Libraries (Lottie, Ktor, Coil)
-dontwarn okio.**
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-dontwarn coil3.**
-keep class coil3.** { *; }
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }
-dontwarn io.github.alexzhirkevich.compottie.**
-keep class io.github.alexzhirkevich.compottie.**

# Compose Runtime
-keepclassmembers class androidx.compose.runtime.RecomposeScopeImpl { *; }

# JNA reflection support
-dontwarn com.sun.jna.**
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** { *; }

# Preserve native function names used by C++ symbol lookup
-keep interface **.TdNativeAndroid { *; }
-keepclassmembers interface **.TdNativeAndroid { *; }
-keep class **.TelegramNativeClient { *; }
-keepclassmembers class **.TelegramNativeClient { *; }