# ---- Kotlin / coroutines / serialization ----
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontwarn kotlinx.**

# kotlinx.serialization – keep generated serializers + @Serializable model classes
-keepclassmembers class app.lumadocs.kmp.** {
    *** Companion;
}
-keepclasseswithmembers class app.lumadocs.kmp.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.lumadocs.kmp.**$$serializer { *; }
-keep @kotlinx.serialization.Serializable class app.lumadocs.kmp.** { *; }
-keep class app.lumadocs.kmp.data.** { *; }
-keep class app.lumadocs.kmp.services.DriveFile { *; }

# ---- Google API Client + Drive (reflects on @Key model fields) ----
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
    @com.google.api.client.util.Value <fields>;
}
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.** { *; }
-keep class com.google.auth.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.auth.**

# ---- Firebase ----
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ---- Google Sign-In / Credential Manager / Google ID ----
-keep class com.google.android.gms.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.gms.**

# ---- Misc transitive deps pulled in by the Google API client ----
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.**
-dontwarn com.google.j2objc.**
-dontwarn org.apache.**
-dontwarn org.joda.**
-dontwarn org.checkerframework.**
-dontwarn afu.org.checkerframework.**
-dontwarn javax.**
-dontwarn java.beans.**
-dontwarn org.w3c.dom.**
-dontwarn org.slf4j.**

# ---- Ktor / OkHttp ----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn io.ktor.**

# Keep enough for reflection-based line numbers in crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
