# Tencent Effect × 七牛推流（PLDroidMediaStreaming）Android 集成示例

> 本工程是面向**已经使用七牛 RTC / 直播推流 SDK（PLDroidMediaStreaming）的客户**接入**腾讯特效（XMagic / TEBeautyKit）** 的参考 Demo。
> 它演示了如何把腾讯特效的美颜、滤镜、贴纸、美妆、AI 抠像等能力**无缝插入到七牛推流的视频处理管线**中，对原有推流业务侵入最小。

---

## 1. 工程结构

```
Tencent_Effect_Qiniu/
├── app/                              ← 七牛推流 Demo 主工程
│   ├── libs/pldroid-media-streaming-3.1.6.jar   ← 七牛推流 SDK
│   ├── src/main/jniLibs/             ← 七牛 .so（编码、推流、SRT/QUIC 等）
│   └── src/main/java/com/qiniu/pili/droid/streaming/demo/
│       ├── StreamingApplication.java          ← 七牛 StreamingEnv.init
│       ├── MainActivity.java                  ← 腾讯特效 License 鉴权入口
│       ├── activity/AVStreamingActivity.java  ← 推流 + 美颜联调主页面
│       └── tencenteffect/
│           └── SurfaceTextureCallbackImp.java ← ★ 两个 SDK 的“桥梁”
│
└── xmagic/                           ← 腾讯特效封装模块（library）
    ├── build.gradle                  ← 依赖 TencentEffect_S1-04 / TEBeautyKit
    ├── src/main/assets/
    │   ├── beauty_panel/             ← 面板配置 JSON（美颜/美妆/贴纸/分割…）
    │   ├── MotionRes/                ← 2D/3D 贴纸、美妆、抠像素材
    │   └── lut/                      ← 滤镜 LUT 图
    └── src/main/java/com/tencent/effect/demo/xmagic/
        ├── LicenseConstant.java      ← XMagic License URL / Key
        ├── BeautyManager.java
        ├── CustomPropertyManager.java← 自定义抠像背景图选择
        └── render/
            ├── TextureConverter.java ← ★ OES↔2D / 旋转 / 镜像
            ├── TextureTransform.java
            └── ...                   ← GL 渲染辅助
```

主目录文件清单仅列出关键部分。`xmagic` 是一个独立 `com.android.library`，对腾讯特效相关的 SDK 依赖、素材、纹理转换工具做了封装，`app` 通过 `implementation project(':xmagic')` 引入。

---

## 2. 如何运行demo

替换 [LicenseConstant.java](xmagic/src/main/java/com/tencent/effect/demo/xmagic/LicenseConstant.java) 中的 URL/Key 为您自己的腾讯云授权。 修改 [app/build.gradle](app/build.gradle) 的 `applicationId` 为申请美颜license时填写的包名。

---

## 3. 七牛 + 腾讯特效是如何结合的

整体思路只有一句：**七牛把每帧的纹理通过 `SurfaceTextureCallback` 抛出来，Demo 在回调里调用腾讯特效 `TEBeautyKit.process(...)` 渲染后再返回给七牛去编码推流。** 具体如下。

### 3.1 数据流

```
摄像头
  │
  ▼
七牛 MediaStreamingManager (CameraStreamingSetting)
  │  采集出 OES 纹理
  ▼
SurfaceTextureCallback.onDrawFrame(textureId, w, h, …)
  │
  │  ① OES → 2D       （TextureConverter.oes2Rgba）
  │  ② 旋转 / 镜像      （TextureConverter.convert，前后摄不同角度）
  │  ③ 美颜处理         （TEBeautyKit.process）  ← 腾讯特效真正干活的地方
  │  ④ 反向旋转回原方向 （directionRevert.convert）
  ▼
返回处理后的 textureId 给七牛
  │
  ▼
七牛 编码 → 推流（RTMP / SRT / QUIC）
```

### 3.2 关键代码：[SurfaceTextureCallbackImp.java](app/src/main/java/com/qiniu/pili/droid/streaming/demo/tencenteffect/SurfaceTextureCallbackImp.java)

这是两个 SDK 的“**焊点**”——它实现了七牛的 `SurfaceTextureCallback`，并在内部驱动腾讯 `TEBeautyKit`。

```java
@Override
public int onDrawFrame(int textureId, int width, int height, float[] transformMatrix) {
    if (directionRevert == null || textureConverter == null || beautyKit == null) {
        return textureId;
    }
    // ① OES（Camera 输出） → 2D（TEBeautyKit 只接受 2D）
    int rgbaId = textureConverter.oes2Rgba(textureId, width, height);
    // ② 摆正人脸方向：前置 270°，后置 90°
    int id = textureConverter.convert(rgbaId, width, height,
            this.isFrontCamera ? 270 : 90, false, false);
    // ③ 调用腾讯特效进行美颜 / 美妆 / 贴纸 / 分割…
    int processed = beautyKit.process(id, height, width);
    // ④ 转回七牛预期的方向，否则预览会歪
    return directionRevert.convert(processed, height, width,
            this.isFrontCamera ? 90 : 270, false, false);
}
```

GL 上下文的生命周期也由七牛管理，因此 `TEBeautyKit` 的创建与销毁必须挂在七牛的 `onSurfaceCreated` / `onSurfaceDestroyed` 上：

```java
@Override
public void onSurfaceCreated() {           // 七牛 GL 线程
    destroyBeautyApi();
    createBeautyApi();                     // new TEBeautyKit(context, EffectMode.PRO)
}

@Override
public void onSurfaceDestroyed() {
    destroyBeautyApi();                    // beautyKit.onPause() + onDestroy()
}
```

> ⚠️ 注意：七牛在页面切到后台时会回调 `onSurfaceDestroyed`，再次回到前台又会回调 `onSurfaceCreated`，意味着 `TEBeautyKit` 会被重建。Demo 在 [AVStreamingActivity.java](app/src/main/java/com/qiniu/pili/droid/streaming/demo/activity/AVStreamingActivity.java) 中通过 `mBeautyKit.exportInUseSDKParam()` 缓存上一次的美颜参数，新美颜对象创建时再 `setLastParamList(lastParams)` 恢复，避免用户参数丢失。

### 3.3 让七牛把“原始纹理”交给我们的关键设置

```java
mCameraStreamingSetting
    .setBuiltInFaceBeautyEnabled(false);   // 关闭七牛自带美颜，避免与腾讯特效冲突
//  .setBuiltInFaceBeautyEnabled(true) 时七牛会返回 Texture2D；为 false 时返回 OES
mMediaStreamingManager.setSurfaceTextureCallback(surfaceTextureCallbackImp);
```

只要注册了 `SurfaceTextureCallback`，七牛就把每一帧的纹理 ID 交给我们处理；我们 `return` 的纹理 ID 即为最终送编码器的画面。

---

## 4. 腾讯特效是如何初始化和运行起来的

整个生命周期由 4 步构成：**License 鉴权 → 面板配置 → GL 上下文里实例化 → 与面板绑定**。

### 4.1 License 鉴权（一次性，启动期）

文件：[MainActivity.java](app/src/main/java/com/qiniu/pili/droid/streaming/demo/MainActivity.java) → `checkLicense()`

```java
TEBeautyKit.setupSDK(
    getApplicationContext(),
    LicenseConstant.mXMagicLicenceUrl,   // license URL
    LicenseConstant.mXMagicKey,          // license Key
    (code, msg) -> {
        if (code == LicenseConstant.AUTH_STATE_SUCCEED) {
            // 鉴权成功，可使用美颜
        }
    });
```

License 常量定义在 [xmagic/.../LicenseConstant.java](xmagic/src/main/java/com/tencent/effect/demo/xmagic/LicenseConstant.java)，**正式接入时请替换为您自己的腾讯云 License**。

### 4.2 面板（UI）配置：[AVStreamingActivity#initBeautyPanelView](app/src/main/java/com/qiniu/pili/droid/streaming/demo/activity/AVStreamingActivity.java)

腾讯特效自带 `TEPanelView`，只要喂给它一组 `TEPanelDataModel`（指向 assets 中的 JSON 描述），UI 就能自动渲染对应的美颜分类：这里使用哪个json文件是需要根据您的美颜套餐决定的。demo 中是使用的S1-04套餐举例。

```java
List<TEPanelDataModel> panelDataModels = TEUIConfig.getInstance().getPanelDataList();
panelDataModels.clear();
panelDataModels.add(new TEPanelDataModel("beauty_panel/beauty_template.json", UICategory.BEAUTY_TEMPLATE));
panelDataModels.add(new TEPanelDataModel("beauty_panel/beauty.json",          UICategory.BEAUTY));
panelDataModels.add(new TEPanelDataModel("beauty_panel/beauty_shape.json",    UICategory.BEAUTY));
panelDataModels.add(new TEPanelDataModel("beauty_panel/beauty_makeup.json",   UICategory.BEAUTY));
panelDataModels.add(new TEPanelDataModel("beauty_panel/lut.json",             UICategory.LUT));
panelDataModels.add(new TEPanelDataModel("beauty_panel/light_makeup.json",    UICategory.LIGHT_MAKEUP));
panelDataModels.add(new TEPanelDataModel("beauty_panel/makeup.json",          UICategory.MAKEUP));
panelDataModels.add(new TEPanelDataModel("beauty_panel/motion_2d.json",       UICategory.MOTION));
panelDataModels.add(new TEPanelDataModel("beauty_panel/motion_3d.json",       UICategory.MOTION));
panelDataModels.add(new TEPanelDataModel("beauty_panel/motion_gesture.json",  UICategory.MOTION));
panelDataModels.add(new TEPanelDataModel("beauty_panel/segmentation.json",    UICategory.SEGMENTATION));

mPanelView = new TEPanelView(this);
mPanelView.showView(callback);
((LinearLayout) findViewById(R.id.te_panel_view_layout)).addView(mPanelView);
```

### 4.3 GL 线程内创建 `TEBeautyKit`

`TEBeautyKit` 必须在拥有 OpenGL 上下文的线程里创建。本 Demo 复用了七牛的 GL 线程：

```java
// 在七牛的 onSurfaceCreated 回调里
beautyKit = new TEBeautyKit(context, XmagicConstant.EffectMode.PRO);

if (callBack != null) {
    callBack.onCreatedTEBeautyKit(beautyKit);  // 把对象抛回 Activity
}
```

### 4.4 把 BeautyKit 与 PanelView 绑定

`TEBeautyKit` 创建好后，回调到 `AVStreamingActivity`，立刻：

```java
mBeautyKit = beautyKit;
mPanelView.setupWithTEBeautyKit(beautyKit);   // ★ 让面板能驱动美颜
if (lastParams != null) {
    mBeautyKit.setLastParamList(lastParams);  // 恢复上次效果
}
customPropertyManager.setBeautyKit(beautyKit);// 让自定义分割等高级用法可用
```

至此：

- 用户在 `TEPanelView` 上拖动滑条 → 经 `TEBeautyKit` 写入参数；
- 七牛 GL 线程每帧调用 `beautyKit.process(...)` → 应用最新参数到当前帧；
- 七牛拿到处理后的纹理继续编码并推流。

---

## 5. 运行流程时序

```
App 启动
  │
  ├─ StreamingApplication.onCreate()
  │     └─ StreamingEnv.init(...)                 // 七牛 SDK 初始化
  │
  └─ MainActivity
        └─ checkLicense()
              └─ TEBeautyKit.setupSDK(...)         // 腾讯特效 License 鉴权
                                                   ↓
进入 AVStreamingActivity（推流页）
  │
  ├─ initBeautyPanelView()                         // 装配腾讯特效 UI 面板
  ├─ new SurfaceTextureCallbackImp(this, callback) // 创建“桥梁”
  ├─ initStreamingManager()
  │     ├─ new MediaStreamingManager(...)
  │     ├─ prepare(cameraSetting, micSetting, ... )
  │     └─ setSurfaceTextureCallback(bridge)       // ★ 把桥梁注册给七牛
  │
  └─ 七牛 GL 线程启动
        ├─ bridge.onSurfaceCreated()
        │     └─ new TEBeautyKit(...)              // 在 GL 线程创建腾讯特效
        │     └─ callback.onCreatedTEBeautyKit()
        │           └─ mPanelView.setupWithTEBeautyKit(beautyKit)
        │
        └─ 每帧 bridge.onDrawFrame(textureId,...)
              └─ TEBeautyKit.process(...)          // 美颜
              └─ return processedTextureId         // 回给七牛去编码
```

---
