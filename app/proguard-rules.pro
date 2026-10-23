# ScrapMaster AI — R8 rules for release builds.
# Room, Moshi (codegen), OkHttp/Retrofit and Coroutines ship consumer rules, but
# reflection-touched code below needs explicit keep rules to survive R8.

# --- Keep app data models parsed via Moshi codegen (field names are JSON wire names) ---
-keep class com.example.data.model.** { <init>(...); <fields>; }
-keep class com.example.data.model.** { <fields>; }

# --- Keep Moshi generated adapters lookup (ClassJsonAdapter via reflection fallback) ---
-keep class **JsonAdapter { <init>(...); }
-keepnames class kotlin.Metadata { *; }

# --- Coroutines: debug metadata + internals used reflectively ---
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# --- OkHttp / Okio platform probes ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Keep crash-relevant stack traces readable ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
