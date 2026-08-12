# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.studenthub.app.data.model.** { *; }
-dontwarn com.google.firebase.**
