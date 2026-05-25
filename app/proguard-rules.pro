-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

-keep class com.gasleakdetector.data.model.** { *; }
-keep class com.gasleakdetector.data.local.LocalDataStorage { *; }

-dontwarn androidx.**
-dontwarn org.slf4j.**
