# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# General Kotlin & Attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class kotlin.Metadata { *; }

# Remote & Local Data Models
-keep class com.joano.seoga.data.remote.** { *; }
-keep class com.joano.seoga.data.local.** { *; }

# Moshi
-keepclassmembers class * {
    @com.squareup.moshi.JsonClass <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.JsonClass <fields>;
}
-keep class *JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
    public <init>(com.squareup.moshi.Moshi, java.lang.reflect.Type[]);
}
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Retrofit
-keepattributes Exceptions
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# DataStore
-dontwarn androidx.datastore.**

# Firebase & OkHttp / Coroutines
-dontwarn com.google.firebase.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
