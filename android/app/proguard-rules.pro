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

# Keep Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep our model classes (needed for Firebase deserialization)
-keep class com.apix.app.StreamConfig { *; }
-keep class com.apix.app.StreamConfig$* { *; }
-keep class com.apix.app.FirebaseModels { *; }
-keep class com.apix.app.FirebaseModels$* { *; }
-keep class com.apix.app.data.** { *; }

# Keep security monitor entry points
-keep class com.apix.app.SecurityMonitor {
    public static ** getInstance(android.content.Context);
    public void startMonitor();
    public void stopMonitor();
    public java.lang.String runInitialCheck();
    public void runInitialCheckAsync(**);
}

# Keep activities
-keep class com.apix.app.SplashActivity { *; }
-keep class com.apix.app.HomeActivity { *; }
-keep class com.apix.app.SubMenuActivity { *; }
-keep class com.apix.app.MainActivity { *; }
-keep class com.apix.app.PlayerActivity { *; }
-keep class com.apix.app.WebViewActivity { *; }
-keep class com.apix.app.ComposeActivity { *; }

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Coil
-keep class coil.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# ===== ANTI-DECOMPILATION =====
-renamesourcefileattribute ''
-keepattributes !SourceFile,!LineNumberTable
-adaptresourcefilecontents
-adaptresourcefilenames
