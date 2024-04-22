
# 指定不去忽略非公共库的类成员
-dontskipnonpubliclibraryclassmembers

# 避免混淆泛型
-keepattributes Signature
#保持反射不被混淆
-keepattributes EnclosingMethod
# 忽略警告
-ignorewarnings
# 优化不优化输入的类文件
-dontoptimize
# 抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable
#类和成员都使用唯一的名字，如果没有这个选项，会有很多变量或方法或类名都叫‘a’，‘b’
-useuniqueclassmembernames


# 保留Serializable序列化的类不被混淆
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
