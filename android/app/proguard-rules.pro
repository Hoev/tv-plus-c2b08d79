# ProGuard / R8 Rules for APiX App

# ===== AGGRESSIVE OBFUSCATION =====
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose
-allowaccessmodification
-repackageclasses ''
-flattenpackagehierarchy ''

# Remove all Log calls in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# ===== KEEP RULES =====

# Keep ExoPlayer classes
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }

# Keep Gson classes
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Keep our model classes
-keep class com.apix.app.StreamConfig { *; }
-keep class com.apix.app.StreamConfig$* { *; }

# Keep security monitor (obfuscate internals but keep entry points)
-keep class com.apix.app.SecurityMonitor {
    public static ** getInstance(android.content.Context);
    public void startMonitor();
    public void stopMonitor();
}

# Keep activities
-keep class com.apix.app.MainActivity { *; }
-keep class com.apix.app.PlayerActivity { *; }
-keep class com.apix.app.WebViewActivity { *; }

# ===== STRING ENCRYPTION HELPERS =====
# Obfuscate string constants used in security checks
-adaptresourcefilecontents
-adaptresourcefilenames

# ===== ANTI-DECOMPILATION =====
# Remove source file names and line numbers
-renamesourcefileattribute ''
-keepattributes !SourceFile,!LineNumberTable
