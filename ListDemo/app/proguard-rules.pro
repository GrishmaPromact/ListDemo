# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\grishma\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Disable Android logging
-dontwarn okio.**
-dontwarn android.support.**
-dontnote android.support.**
-dontwarn org.apache.http.**
-dontwarn org.apache.lang.**
-dontwarn android.net.http.AndroidHttpClient
-dontwarn com.google.android.gms.**
-dontwarn com.android.volley.toolbox.**
-dontwarn com.squareup.**
-dontnote com.squareup.**
-dontwarn com.dropbox.**
-dontnote com.dropbox.**
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
-keep class * extends android.app.Activity

-assumenosideeffects class android.util.Log {
public static *** d(...);
public static *** w(...);
public static *** v(...);
public static *** i(...);
public static *** e(...);
}

