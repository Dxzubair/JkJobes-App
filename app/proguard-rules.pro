# Room - keep entity classes since they're accessed via reflection by the generated DAOs
-keep class com.jkjobs.app.data.JobPosting { *; }

# OkHttp / Okio - suppress known harmless R8 warnings about optional dependencies
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Jsoup - no special rules required, but keep it from being warned about missing optional classes
-dontwarn org.jsoup.**
