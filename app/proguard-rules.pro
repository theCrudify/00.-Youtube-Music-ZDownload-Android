# youtubedl-android (yt-dlp, FFmpeg, shared utils) loads classes and parses JSON reflectively
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**

# Jackson maps yt-dlp's JSON onto VideoInfo via reflection
-keep class com.fasterxml.jackson.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn com.fasterxml.jackson.**

# Apache commons-compress unpacks the bundled Python/FFmpeg; it instantiates classes reflectively
-keep class org.apache.commons.** { *; }
-dontwarn org.apache.commons.**
