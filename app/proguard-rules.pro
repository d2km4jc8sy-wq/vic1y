# ============ Retrofit ============
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ============ Gson ============
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============ 数据模型 ============
-keep class com.example.s1client.data.model.** { *; }

# ============ OkHttp ============
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ============ Jsoup ============
-keeppackagenames org.jsoup.nodes

# ============ Kotlin ============
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# ===== release 剥离调试日志（2026-10-16，优化报告 Phase1-②）=====
# Log.v/d 调用直接从 release 字节码移除：全项目 92 处日志里 49 处 Log.d 含完整 URL，
# 不剥离会随包分发、logcat 可拼出用户浏览轨迹。Log.w/e 保留（崩溃诊断要用）。
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# 崩溃堆栈可还原：保留行号，配合 outputs/mapping/release/mapping.txt 反混淆
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
