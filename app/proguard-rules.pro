# AutoBook ProGuard Rules

# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.autobook.app.data.model.** { *; }
-keep class com.autobook.app.data.remote.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
