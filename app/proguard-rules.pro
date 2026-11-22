# R8 / ProGuard Baseline Rules for Hodja (Nasreddin)
# Activated when `isMinifyEnabled = true` is enabled for Play Console release bundles.

# Keep immutable comic domain & monetization models (used across JSON serialization & Compose UI keys)
-keep class com.example.model.** { *; }
-keep class com.example.security.EntitlementSnapshot { *; }
-keep class com.example.monetization.StoreProductPrices { *; }

# Keep stack trace line numbers in release crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Strip verbose debug logs in minified release builds (keep Log.w / Log.e)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Play Billing 9.1.0 & Google Mobile Ads 25.5.0 pull their own consumer-rules.pro automatically via AARs.
