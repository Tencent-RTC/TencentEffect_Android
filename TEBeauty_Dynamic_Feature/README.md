[简体中文](https://github.com/Tencent-RTC/TencentEffect_Android/blob/main/TEBeauty_Dynamic_Feature/README_zh_CN.md)  |  English

# TEBeauty_Dynamic_Feature

This project is a demo project of TencentEffectSDK. It is modified based on the TEBeauty_API_Example project. It shows how to integrate the Dynamic Feature feature of GooglePlay and implement on-demand download of beauty effect modules to reduce the initial application volume.

# About Google Play Dynamic Feature
Dynamic Feature Modules is a solution that enables Android applications to load specific features on demand. It uses modular design and dynamic delivery technology to reduce the core volume of the application and download additional feature modules when users need them.
For details, see the introduction on the Google official website: https://developer.android.com/guide/playcore/feature-delivery
# Project Struct
```
TEBeauty_Dynamic_Feature
├── demo                  # Main application module  
└── TencentEffectDynamicFeature # Dynamic Feature module

```

# Quick Start
- Modify LicenseConstant.java: Set mXMagicLicenceUrl and mXMagicKey to the URL and Key you applied for in the Tencent Cloud console.
- Modify demo/build.gradle, change applicationId to your package name, and ensure the package name matches the license url and Key in the previous step.
- In Android Studio, execute Build > Generate Signed Bundle/APK. For key store path, use demo/demo.jks. The key alias is demo, and the password is abc1234.
- Download the bundle tool jar package: https://developer.android.com/tools/bundletool
- Execute the following command in the project root directory to build the apks package. Note to modify the path of the bundle tool.
```
java -jar /path/to/bundletool-all-1.18.1.jar build-apks --local-testing --bundle demo/debug/demo-debug.aab --output demo/debug/demo-debug.apks --ks demo/demo.jks --ks-pass pass:abc1234 --ks-key-alias demo --key-pass pass:abc1234
```
- Install the apks package on the mobile phone. Note to modify the path of the bundle tool.
```
java -jar /path/to/bundletool-all-1.18.1.jar  install-apks --apks demo/debug/demo-debug.apk
```
- Run the app. The dynamic feature module will be automatically installed on the app homepage.

# How to Add TencentEffect'S Dynamic Feature in Your Project
## 1.Creating a Dynamic Feature Module
In Android Studio, select New --> Module --> Dynamic Feature to create a Module. Assume the Module name is TencentEffectDynamicFeature.
## 2.Configure TencentEffect SDK
Create a libs directory in TencentEffectDynamicFeature, and place the aar package of TencentEffect SDK (containing so library and asset) in this directory, then configure the dependency in its build.gradle:
```
implementation project(":demo")
implementation fileTree(dir: "libs", include: ['*.aar'])
```
## 3.Configure the Main Module
Add the `dynamicFeatures` attribute in `demo/build.gradle`:
```
dynamicFeatures = [':TencentEffectDynamicFeature']
```
Place the TencentEffect SDK's aar package in the demo/libs directory, and configure the dependencies attribute:
```
    implementation 'com.google.android.play:feature-delivery:2.1.0'
    compileOnly fileTree(dir: "libs", include: ['*.aar'])
    implementation 'com.tencent.tav:libpag:4.4.24-noffavc'
```
The dependency on the TencentEffect SDK here can only be configured with compileOnly instead of implementation, to ensure that this aar only participates in compilation and will not increase the main package size.

## 4.Installing the Dynamic Feature Module in the Code
First, copy the FeatureManager.java from the demo project to your project. Then, add SplitCompat.install(this) in the attachBaseContext of the application:
```
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        SplitCompat.install(this);
    }
```
Then trigger the installation in a certain activity of yours. Refer to the CheckFeatureActivity.java in the demo project:
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
## 5.Using TencentEffect
- Upon successful installation in the previous step, you can navigate to other Activities to use TencentEffect. Note: If you directly use the classes in Dynamic Feature in the Activity during installation after the first successful installation of Dynamic Feature, a "class not found" issue may occur. This might be a bug of dynamic feature. Therefore, it is recommended that you navigate to other Activities to use TencentEffect.
- When using an Activity with TencentEffect, you also need to call SplitCompat.installActivity(this) in the attachBaseContext method.
- The loading of the so library for Dynamic Feature is different from ordinary loading. It cannot be directly used with System.load. Therefore, before using the TencentEffect SDK, please manually load each so of the SDK with the following code:
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
Due to the variations of the used SDK version/package, not all libraries in the "libs" folder above necessarily exist. Please refer to the .so list in the .aar package you actually use. However, the loading order of each .so cannot be modified.

## 6.Package and Run
Follow the steps in "Quick Start" above to perform local packaging testing.







