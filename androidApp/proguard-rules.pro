# GHOSTGRAM UNIVERSAL R8 / PROGUARD SHIELD (v0.2.0)
# ==============================================================================

# 💥 1. ВЕСЬ КОД НАШЕГО ПРИЛОЖЕНИЯ (Защищаем от обфускации)
# 💥 ГАСИМ ВОРНИНГ ЛИНТЕРА СТУДИИ И ЗАЩИЩАЕМ ЯДРО TDLIB:
-dontwarn org.drinkless.tdlib.**
-keep class org.drinkless.tdlib.** { *; }
-keepclassmembers class org.drinkless.tdlib.** { *; }

# Защита нашего KMP-клиента и нативных вызовов:
-keep class com.ghostgram.** { *; }
-keepclassmembers class com.ghostgram.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class entity.** { *; }
-keepclassmembers class entity.** { *; }

-keep class repository.** { *; }
-keepclassmembers class repository.** { *; }

-keep class usecase.** { *; }
-keepclassmembers class usecase.** { *; }

# 💥 2. ВСЕ НА ТИВНЫЕ C++ МЕТОДЫ (JNI для TDLib)
-keepclasseswithmembernames class * {
    native <methods>;
}


# 💥 3. KOTLIN COROUTINES & FLOW
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# 💥 4. KOTLINX SERIALIZATION (JSON)
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-dontnote kotlinx.serialization.**

# 💥 5. KOIN
-keep class org.koin.** { *; }
-keepclassmembers class org.koin.** { *; }
-dontwarn org.koin.**

# 💥 6. AIRBNB LOTTIE
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# 💥 7. KTOR, OKIO, COIL 3, COMPOTTIE
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn okio.**
-keep class coil3.** { *; }
-dontwarn coil3.**
-keep class io.github.alexzhirkevich.compottie.** { *; }
-dontwarn io.github.alexzhirkevich.compottie.**

# 💥 8. COMPOSE RUNTIME
-keepclassmembers class androidx.compose.runtime.RecomposeScopeImpl { *; }

-dontwarn com.sun.jna.**
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** { *; }

# 💥 Защищаем наш интерфейс TdNativeAndroid от переименования функций!
# (Если R8 переименует td_json_client_create в a(), C++ не сможет его вызвать)
-keep interface **.TdNativeAndroid { *; }
-keepclassmembers interface **.TdNativeAndroid { *; }

-keep class **.TelegramNativeClient { *; }
-keepclassmembers class **.TelegramNativeClient { *; }