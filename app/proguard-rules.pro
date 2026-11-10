# R8 Shrinking and Obfuscation Rules for WebCade Gaming Browser

# Preserve methods annotated with @JavascriptInterface so native bridges work under R8
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# OkHttp and networking rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

