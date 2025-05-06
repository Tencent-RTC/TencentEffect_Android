简体中文  |  [English](https://github.com/Tencent-RTC/TencentEffect_Android/blob/main/TEBeauty_Dynamic_Feature/README.md)

# TEBeauty_Dynamic_Feature

本工程是 TencentEffectSDK 的 demo 工程，基于 TEBeauty_API_Example 工程进行改造，展示了如何集成 GooglePlay 的 Dynamic Feature 特性，实现按需下载美颜特效模块以减小初始应用体积。

# 关于 GooglePlay Dynamic Feature
Dynamic Feature Modules 是一种让 Android 应用能够按需加载特定功能的解决方案。它通过模块化设计和动态交付技术，使应用的核心体积更小，同时在用户需要时下载附加功能模块。
详情见Google官网介绍：https://developer.android.com/guide/playcore/feature-delivery

# 项目结构
```
TEBeauty_Dynamic_Feature
├── demo                  # Main application module  
└── TencentEffectDynamicFeature # Dynamic Feature module

```

# 快速开始
- 修改 LicenseConstant.java：将 mXMagicLicenceUrl 和 mXMagicKey 设置为你在腾讯云控制台申请到的 URL 和 Key。
- 修改 demo/build.gradle，将 applicationId 修改为你的包名，并确保该包名与上一步的 license url 和 Key 是匹配的。
- 在 AndroidStudio 中，执行 Build > Generate Signed Bundle/APK。key store path使用demo/demo.jks，key alias为demo，password为abc1234。
- 下载 bundle tool 的 jar 包：https://developer.android.com/tools/bundletool
- 在工程根目录执行如下命令构建apks包，注意修改bundletool的路径
```
java -jar /path/to/bundletool-all-1.18.1.jar build-apks --local-testing --bundle demo/debug/demo-debug.aab --output demo/debug/demo-debug.apks --ks demo/demo.jks --ks-pass pass:abc1234 --ks-key-alias demo --key-pass pass:abc1234
```
- 将apks包安装到手机,注意修改bundletool的路径
```
java -jar /path/to/bundletool-all-1.18.1.jar  install-apks --apks demo/debug/demo-debug.apk
```
- 运行app，在app首页会自动安装 dynamic feature module

# 如何在你的工程中添加TencentEffect的Dynamic Feature
## 1.新建 Dynamic Feature Module
在 AndroidStudio 中选择 New --> Module --> Dynamic Feature，创建一个 module，假设 module 名称为 TencentEffectDynamicFeature。
## 2.配置TencentEffect SDK
在 TencentEffectDynamicFeature 中创建 libs 目录，并将 TencentEffect SDK 的 aar 包（内含so库、asset）放在此目录，然后在它的 build.gradle 中配置依赖：
```
implementation project(":demo")
implementation fileTree(dir: "libs", include: ['*.aar'])
```
## 3.配置主module
在 demo/build.gradle 添加 dynamicFeatures 属性：
```
dynamicFeatures = [':TencentEffectDynamicFeature']
```
将 TencentEffect SDK 的 aar 包放到 demo/libs 目录，并配置 dependencies 属性：
```
    implementation 'com.google.android.play:feature-delivery:2.1.0'
    compileOnly fileTree(dir: "libs", include: ['*.aar'])
    implementation 'com.tencent.tav:libpag:4.4.24-noffavc'
```
注意：这里对 TencentEffect SDK 的依赖只能配置 compileOnly 而不能配置 implementation，以确保该 aar 仅参与编译，不会增加主包体积。 

## 4.代码中安装 dynamic feature module
首先，将 demo 工程的 FeatureManager.java copy到你的工程中，然后在 application 的 attachBaseContext中添加SplitCompat.install(this)：
```
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        SplitCompat.install(this);
    }
```
然后在你的某个activity触发安装，请参考demo工程的 CheckFeatureActivity.java：
```
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        SplitCompat.installActivity(this);
    }

private void loadDFModule() {
        featureManager = new FeatureManager(getApplicationContext(), MODULE_NAME);
        featureManager.loadMoudle(new FeatureManager.FeatureStatusListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: ");
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "onError: e=" + e.getMessage());
            }

            @Override
            public void onUserConfirm(SplitInstallSessionState sessionState) {
                Log.d(TAG, "onUserConfirm: sessionState=" + sessionState.toString());
                featureManager.startConfirmationDialogForResult(sessionState, CheckFeatureActivity.this, REQUEST_CODE);
            }
        });

    }
```
## 5.使用TencentEffect
- 在上一步安装成功后，就可以跳转到其他 Activity 使用 TencentEffect 了。注意：首次安装 Dynamic Feature 成功后，如果直接在安装时的 Activity 使用 TencentEffect，可能出现 class not found 的问题，这个可能是 dynamic feature 的 bug，因此建议跳转到其他 Activity 使用 TencentEffect。
- 在使用 TencentEffect 的 Activity，也要在 attachBaseContext 中调用 SplitCompat.installActivity(this)
- Dynamic Feature 的 so 库的加载与普通加载不一样，不能直接使用 System.load，因此在使用 TencentEffect SDK之间，请先用如下代码手动加载 SDK 的各个so：
```
String[] libs = {"YTCommonXMagic", "v8jni", "ace_zplan", "tecodec", "pag", "light-sdk"};
for(String lib : libs) {
    try {
        SplitInstallHelper.loadLibrary(this,lib);
    }catch (UnsatisfiedLinkError e) {
        Log.d(TAG, "loadLibrary: " + e.getMessage());
    }
}
```
注意：由于使用的SDK版本/套餐的差异，上面代码中 libs 里的各个库不一定全部存在，请以你实际的 aar 包里的so列表为准。但不能修改各个库的加载顺序。

## 6.打包运行
请参考上文 “快速开始” 中的步骤，进行本地打包测试。







