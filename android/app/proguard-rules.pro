# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.voicelike.app.AppStats { *; }
-keep class com.voicelike.app.MediaItem { *; }
-keep class com.voicelike.app.LocalizedStrings { *; }
-keep class com.voicelike.app.AppLanguage { *; }
-keep class com.voicelike.app.photowall.model.PhotoWallDocument { *; }
-keep class com.voicelike.app.photowall.model.PhotoWallItem { *; }

# Gson reads and writes the app's data classes by reflection: field names are the JSON keys (also
# for server payloads and hand-parsed files) and generic signatures carry Map/List element types.
# Without this, R8 full mode renames those fields, drops their generic signatures (Map<Long, _>
# comes back with String keys -> ClassCastException) and even removes fields it thinks are never
# read, so every Gson model breaks in release builds only. Classes stay shrinkable/obfuscated.
-keepclassmembers class com.voicelike.app.** {
    !static !transient <fields>;
}

# Gson & Reflection
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Kotlin
-keep class kotlin.Metadata { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.CoroutineExceptionHandler {
    <init>(...);
}

# MNN JNI bridge
-keep class com.taobao.android.mnn.** { *; }
-keepclasseswithmembernames class com.taobao.android.mnn.** {
    native <methods>;
}

# Compose (Generally handled by R8, but good to be safe)
# -keep class androidx.compose.** { *; }

# ExoPlayer (Reduce stripping of required components if issues arise, but start strict)
# -keep class androidx.media3.** { *; }

# Prevent R8 from stripping the generic type information from fields/methods
# which Gson relies on for TypeToken
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Hotfix for Android runtime VerifyError on obfuscated StampCalendarPageKt.
# Keep this Compose-heavy class and generated inner classes unoptimized/unobfuscated
# to avoid dex verifier register type mismatch on incremental updates.
-keep class com.voicelike.app.stamp.StampCalendarPageKt { *; }
-keep class com.voicelike.app.stamp.StampCalendarPageKt$* { *; }

# Hotfix for Android runtime VerifyError on the Compose-heavy watermark editor.
# R8 can produce verifier-hostile bytecode for this very large composable on some ART versions.
-keep class com.voicelike.app.WatermarkFrameViewKt { *; }
-keep class com.voicelike.app.WatermarkFrameViewKt$* { *; }
