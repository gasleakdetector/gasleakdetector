-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

-keep class com.gasleak.nativebridge.NativeBridge {
    public native <methods>;
}

-keep class com.gasleak.data.model.** { *; }

-dontwarn androidx.**