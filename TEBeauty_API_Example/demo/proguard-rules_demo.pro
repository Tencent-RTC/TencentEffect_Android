-dontskipnonpubliclibraryclassmembers

-keepattributes Signature
-keepattributes EnclosingMethod
-ignorewarnings
-dontoptimize
-keepattributes SourceFile,LineNumberTable
-useuniqueclassmembernames

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}



-keep class com.tencent.demo.** { *; }
-keep class com.tencent.effect.beautykit.** { *; }
-keep class androidx.** { *; }
-keep class com.squareup.** { *; }
-keep class com.warkiz.** { *; }
-keep class com.google.** { *;}
-keep class com.bumptech.** { *;}
