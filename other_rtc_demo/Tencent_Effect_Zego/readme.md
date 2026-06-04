# 腾讯特效 SDK 集成 ZEGO RTC Demo（Android）

本工程演示了如何将 **腾讯特效（美颜）SDK（Tencent Effect / Xmagic）** 集成到 **ZEGO RTC（即构 RTC）SDK** 的视频流处理链路中，并接入腾讯特效官方提供的 **TEBeautyKit 美颜面板**，实现一套可直接运行的"实时音视频 + 美颜/滤镜/美妆/动效贴纸/绿幕分割"集成范例。

## 一、功能简介

本 Demo 实现的能力：

- 使用 ZEGO RTC SDK 进行摄像头采集与本地预览（`TextureView`）。
- 通过 ZEGO RTC 提供的 **自定义视频前处理（Custom Video Processing）** 能力，将摄像头采集到的纹理（`GL_TEXTURE_2D`）回调给业务层。
- 业务层将该纹理交给 **腾讯特效 SDK（TEBeautyKit）** 处理，得到处理后的 OpenGL 纹理。
- 将处理后的纹理通过 `sendCustomVideoProcessedTextureData` 回送给 ZEGO RTC，用于本地预览 / 推流。
- 接入 **TEBeautyKit 官方美颜面板（TEPanelView）**，开箱即用地展示美颜、美型、美妆、滤镜、动效贴纸、人像分割等功能。
- 演示了**前后摄像头切换**与**屏幕方向变化**时美颜 SDK 的图像方向同步处理。

---

## 二、如何运行demo

在APP module下找到LicenseConstant.java 在这类中填写您的美颜license 信息和zego license 信息，并且在app module 下的build.gradle 中将applicationId 的值修改为您在申请美颜license 时填写的包名。

## 三、工程结构

```
Tencent_Effect_Zego/
├── build.gradle                       // 顶层 Gradle 配置
├── settings.gradle                    // 模块声明（include :app、:Xmagic）和 ZEGO Maven 仓库
├── app/                               // 主 App 模块（Demo 业务代码）
│   ├── build.gradle                   // 依赖 :Xmagic 模块和 libs/ZegoExpressEngine.aar
│   ├── libs/
│   │   └── ZegoExpressEngine.aar      // ZEGO RTC SDK
│   └── src/main/
│       ├── AndroidManifest.xml        // 权限声明（CAMERA / RECORD_AUDIO / INTERNET 等）
│       ├── java/com/tencent/effect/demo/zego/
│       │   ├── MainActivity.java                // 入口：初始化腾讯特效 License，跳转特效页
│       │   ├── EffectActivity.java              // 核心：创建 ZEGO 引擎 + 接入腾讯特效 + 美颜面板
│       │   ├── CustomVideoProcessHandler.java   // 关键：ZEGO 自定义视频处理回调，调用美颜
│       │   └── LicenseConstant.java             // ZEGO appID/appSign + 腾讯特效 License Key/Url
│       └── res/layout/
│           └── effect_layout.xml      // 预览 TextureView + 美颜面板容器 + 摄像头切换 Switch
└── Xmagic/                            // 腾讯特效模块（资源 + SDK 依赖）
    ├── build.gradle                   // 集成 TEBeautyKit 与 TencentEffect_S1 SDK
    └── src/main/assets/
        ├── beauty_panel/              // 美颜面板的 JSON 配置 + 图标资源
        │   ├── beauty.json            // 基础美颜
        │   ├── beauty_template.json   // 美颜模板
        │   ├── beauty_image.json      // 画质（锐化、清晰度等）
        │   ├── beauty_shape.json      // 美型
        │   ├── beauty_makeup.json     // 美妆
        │   ├── lut.json               // 滤镜
        │   ├── light_makeup.json      // 轻美妆
        │   ├── makeup.json            // 风格妆
        │   ├── motion_2d.json         // 2D 动效
        │   ├── motion_3d.json         // 3D 动效
        │   ├── motion_gesture.json    // 手势动效
        │   └── segmentation.json      // 人像分割
        ├── MotionRes/                 // 动效贴纸 / 美妆 / 分割等素材
        └── lut/                       // 滤镜 LUT 图
```

---

## 四、整体集成流程

下面是本 Demo 的运行时调用链路：

```
MainActivity
   │ TEBeautyKit.setupSDK( License )            // 1. 鉴权腾讯特效 License
   ▼
EffectActivity.onCreate
   │ initBeautyView()                           // 2. 初始化 TEPanelView（美颜面板）
   │ createEngine()                             // 3. 创建 ZegoExpressEngine
   │ enableEffect(true)                         // 4. 开启 ZEGO 自定义视频处理
   │   └─ enableCustomVideoProcessing(true, GL_TEXTURE_2D)
   │   └─ setCustomVideoProcessHandler(CustomVideoProcessHandler)
   ▼
CustomVideoProcessHandler.onStart                // 5. ZEGO 通知开始采集
   │ new TEBeautyKit(context, EffectMode.PRO)    //    在 GL 线程上创建腾讯特效 BeautyKit
   │ callback.onCreatedBeautyKit(beautyKit)      //    回到主线程绑定到 TEPanelView
   ▼
CustomVideoProcessHandler.onCapturedUnprocessedTextureData  // 6. 每帧回调
   │ resultId = teBeautyKit.process(textureID, w, h)         //    腾讯特效处理纹理
   │ expressEngine.sendCustomVideoProcessedTextureData(...)  //    回传给 ZEGO 渲染/推流
   ▼
CustomVideoProcessHandler.onStop                 // 7. 停止采集，释放 BeautyKit
   │ teBeautyKit.onPause(); teBeautyKit.onDestroy();
```

> 关键点：**腾讯特效的 `TEBeautyKit` 必须在 ZEGO 提供的 GL 线程上创建、使用、销毁**。本 Demo 通过 `IZegoCustomVideoProcessHandler` 的 `onStart` / `onStop` / `onCapturedUnprocessedTextureData` 回调天然满足该约束。

---

## 五、关键代码解析

### 1. License 鉴权（`MainActivity.java`）

进入特效页面前必须先完成腾讯特效 License 鉴权，否则 `TEBeautyKit` 创建将失败。

```java
TEBeautyKit.setupSDK(this,
        LicenseConstant.mXMagicLicenceUrl,
        LicenseConstant.mXMagicKey,
        (errorCode, msg) -> {
            if (errorCode == 0) {
                startActivity(new Intent(this, EffectActivity.class));
            } else {
                Log.e(TAG, "auth failed " + msg + "  " + errorCode);
            }
        });
```

> `LicenseConstant.java` 中需要替换为你自己在腾讯云控制台申请的 `mXMagicLicenceUrl` / `mXMagicKey`，以及在 ZEGO 控制台申请的 `appID` / `appSign`。

### 2. 创建 ZEGO 引擎并启用自定义视频处理（`EffectActivity.java`）

```java
void createEngine() {
    ZegoEngineProfile profile = new ZegoEngineProfile();
    profile.appID    = LicenseConstant.appID;
    profile.appSign  = LicenseConstant.appSign;
    profile.scenario = ZegoScenario.BROADCAST;        // 直播场景，按业务调整
    profile.application = getApplication();
    expressEngine = ZegoExpressEngine.createEngine(profile, null);
    expressEngine.useFrontCamera(true);
    enableEffect(true);                               // 开启自定义前处理 + 接入腾讯特效
}

private void enableEffect(boolean enable) {
    ZegoCustomVideoProcessConfig config = new ZegoCustomVideoProcessConfig();
    config.bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;     // 用 OpenGL 纹理传递，性能最佳
    expressEngine.enableCustomVideoProcessing(enable, config);
    if (enable) setProcessHandler();
}

private void setProcessHandler() {
    processHandler = new CustomVideoProcessHandler(this, expressEngine);
    processHandler.setCustomVideoProcessCallBack(beautyKit ->
            new Handler(Looper.getMainLooper()).post(() ->
                    mTEPanelView.setupWithTEBeautyKit(beautyKit)));   // 主线程绑定面板
    expressEngine.setCustomVideoProcessHandler(processHandler);
}
```

要点：

- `ZegoVideoBufferType.GL_TEXTURE_2D`：腾讯特效擅长处理 OpenGL 纹理，与 ZEGO 之间用纹理传递可避免 CPU/GPU 反复拷贝。
- `processHandler` 在拿到 `TEBeautyKit` 实例后，**切回主线程**再绑定到 `TEPanelView`，因为面板属于 UI。

### 3. ZEGO 自定义视频处理回调（`CustomVideoProcessHandler.java`）

继承 `IZegoCustomVideoProcessHandler`，在 ZEGO 的 GL 线程上完成腾讯特效的创建、处理、销毁。

```java
@Override
public void onStart(ZegoPublishChannel channel) {
    // GL 线程，在此创建 TEBeautyKit
    teBeautyKit = new TEBeautyKit(context, XmagicConstant.EffectMode.PRO);

    // 监听设备方向，同步给腾讯特效，保证人脸检测方向正确
    teBeautyKit.setEventListener((orientation, deviceDirection) -> {
        // 前/后摄像头方向不同，需要分别处理（参考代码完整 switch）
    });

    if (customVideoProcessCallBack != null) {
        customVideoProcessCallBack.onCreatedBeautyKit(teBeautyKit);   // 回调给 UI 层绑定面板
    }
}

@Override
public void onCapturedUnprocessedTextureData(int textureID, int width, int height,
                                             long ts, ZegoPublishChannel channel) {
    if (teBeautyKit == null) {
        // 美颜未就绪，直接透传原始纹理
        expressEngine.sendCustomVideoProcessedTextureData(textureID, width, height, ts, channel);
    } else {
        // 调用腾讯特效处理：输入纹理 → 输出新纹理
        int resultId = teBeautyKit.process(textureID, width, height);
        expressEngine.sendCustomVideoProcessedTextureData(resultId, width, height, ts, channel);
    }
}

@Override
public void onStop(ZegoPublishChannel channel) {
    if (teBeautyKit != null) {
        teBeautyKit.onPause();
        teBeautyKit.onDestroy();
    }
}
```

要点：

- `process(textureID, width, height)`：腾讯特效核心接口，输入 / 输出均为 OpenGL 纹理 ID。
- **方向同步**：`teBeautyKit.setEventListener` 中根据 `deviceDirection` 调用 `setImageOrientation`，**前后摄像头需分别配置不同旋转**，否则人脸检测会错位、动效贴纸贴歪。
- 切前后摄像头时，由 `EffectActivity` 调用 `processHandler.setFrontCamera(isChecked)` 同步给方向监听器：

```java
expressEngine.useFrontCamera(isChecked);
processHandler.setFrontCamera(isChecked);
```

### 4. 接入 TEBeautyKit 官方美颜面板（`EffectActivity.initBeautyView`）

腾讯特效提供了 `TEPanelView`，可零成本接入完整美颜 UI。开发者通过 `TEUIConfig` 注入希望展示的功能 JSON 配置：

```java
List<TEPanelDataModel> panelDataModels = TEUIConfig.getInstance().getPanelDataList();
panelDataModels.clear();

// 按需添加各项能力
panelDataModels.add(new TEPanelDataModel("beauty_panel/beauty_template.json", UICategory.BEAUTY_TEMPLATE));
panelDataModels.add(new TEPanelDataModel("beauty_panel/beauty.json",          UICategory.BEAUTY));
panelDataModels.add(new TEPanelDataModel("beauty_panel/beauty_image.json",    UICategory.BEAUTY));
panelDataModels.add(new TEPanelDataModel("beauty_panel/beauty_shape.json",    UICategory.BEAUTY));
panelDataModels.add(new TEPanelDataModel("beauty_panel/beauty_makeup.json",   UICategory.BEAUTY));
panelDataModels.add(new TEPanelDataModel("beauty_panel/lut.json",             UICategory.LUT));
panelDataModels.add(new TEPanelDataModel("beauty_panel/light_makeup.json",    UICategory.LIGHT_MAKEUP));
panelDataModels.add(new TEPanelDataModel("beauty_panel/makeup.json",          UICategory.MAKEUP));
panelDataModels.add(new TEPanelDataModel("beauty_panel/motion_2d.json",       UICategory.MOTION));
panelDataModels.add(new TEPanelDataModel("beauty_panel/motion_3d.json",       UICategory.MOTION));
panelDataModels.add(new TEPanelDataModel("beauty_panel/motion_gesture.json",  UICategory.MOTION));
panelDataModels.add(new TEPanelDataModel("beauty_panel/segmentation.json",    UICategory.SEGMENTATION));

mTEPanelView = new TEPanelView(this);
mTEPanelView.showView(this);
mPanelLayout.addView(mTEPanelView, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
```

待 `CustomVideoProcessHandler.onStart` 创建好 `TEBeautyKit` 后再绑定即可：

```java
mTEPanelView.setupWithTEBeautyKit(beautyKit);
```

> 所有 JSON 与图标资源均位于 `Xmagic/src/main/assets/beauty_panel/`，可直接复用，也可自定义增删项。

### 5. 生命周期

`EffectActivity` 中只关心预览的启停：

```java
@Override protected void onResume()  { expressEngine.startPreview(zegoCanvas); }
@Override protected void onPause()   { expressEngine.stopPreview(); }
@Override protected void onDestroy() { enableEffect(false); }   // 关闭自定义前处理 → 触发 onStop 释放 BeautyKit
```

`TEBeautyKit` 的 `onPause` / `onDestroy` 调用，由 `CustomVideoProcessHandler.onStop` 在 GL 线程统一收口，无需业务层手动管理。





### Step 2. 配置 License

替换 [LicenseConstant.java](app/src/main/java/com/tencent/effect/demo/zego/LicenseConstant.java) 中：

- `appID` / `appSign`：ZEGO 控制台
- `mXMagicLicenceUrl` / `mXMagicKey`：腾讯云视立方控制台

### Step 3. 在你的 RTC 页面里启用自定义前处理

```java
ZegoCustomVideoProcessConfig cfg = new ZegoCustomVideoProcessConfig();
cfg.bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
expressEngine.enableCustomVideoProcessing(true, cfg);
expressEngine.setCustomVideoProcessHandler(yourHandler);
```

### Step 4. 复用 `CustomVideoProcessHandler`

直接复制 [CustomVideoProcessHandler.java](app/src/main/java/com/tencent/effect/demo/zego/CustomVideoProcessHandler.java)，里面已经处理好：

- 在 `onStart` 创建 `TEBeautyKit`（GL 线程）
- 在 `onCapturedUnprocessedTextureData` 调 `process` 处理纹理并回送
- 在 `onStop` 释放 `TEBeautyKit`
- 监听 `setEventListener` 同步设备方向

### Step 5. 接入美颜 UI

如果你想沿用腾讯特效官方面板，照搬 `EffectActivity.initBeautyView()` 即可；
如果你想自研 UI，直接拿 `TEBeautyKit` 实例调用其 SDK 接口（如 `setEffect(...)`、`setFilter(...)`）。

### Step 6. 切换前后摄像头

```java
expressEngine.useFrontCamera(isFront);
processHandler.setFrontCamera(isFront);   // 让美颜也知道当前摄像头朝向
```

---
