# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ──────────────────────────────────────────────
# Kotlin Serialization
# ──────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class ru.saikodev.initial.data.api.dto.**$$serializer { *; }
-keepclassmembers class ru.saikodev.initial.data.api.dto.** {
    *** Companion;
}
-keepclasseswithmembers class ru.saikodev.initial.data.api.dto.** {
    *** Companion;
}
-keepclasseswithmembers class ru.saikodev.initial.domain.model.** {
    *** Companion;
}

# ──────────────────────────────────────────────
# Retrofit
# ──────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ──────────────────────────────────────────────
# OkHttp
# ──────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ──────────────────────────────────────────────
# Coroutines
# ──────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ──────────────────────────────────────────────
# Hilt
# ──────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ──────────────────────────────────────────────
# Google
# ──────────────────────────────────────────────
-keep class com.google.android.material.** { *; }
-keep class androidx.** { *; }

# ──────────────────────────────────────────────
# Model classes (keep for serialization)
# ──────────────────────────────────────────────
-keep class ru.saikodev.initial.domain.model.** { *; }
-keep class ru.saikodev.initial.data.api.dto.** { *; }

# ──────────────────────────────────────────────
# Remove logging in release builds
# ──────────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
