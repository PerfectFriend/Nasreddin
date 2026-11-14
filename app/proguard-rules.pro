# R8 / ProGuard Baseline Rules for Hodja (Nasreddin)
# Activated when `isMinifyEnabled = true` is enabled for Play Console release bundles.

# Keep immutable comic domain models (used across JSON serialization & Compose UI keys)
-keep class com.example.model.** { *; }

# Keep stack trace line numbers in release crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Strip verbose debug logs in minified release builds (keep Log.w / Log.e)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Play Billing & Ads SDKs pull consumer ProGuard rules automatically via AARs
# once dependencies are wired into app/build.gradle.kts.
