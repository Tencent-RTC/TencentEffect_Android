# 在火山引擎 RTC 中接入腾讯特效（Xmagic）美颜 SDK —— Demo 代码说明


## 快速跑通demo
在demo中的app module 下找到LicenseConstant.java 类，填写美颜鉴权信息和byte 鉴权信息, 再在app module下的build.gradle中将applicationId 修改为申请美颜license 时的包名。

## 1. 整体架构

整体采用三层结构：**业务层 ←→ 适配层 ←→ SDK 层（火山 RTC + 腾讯特效）**。

```
+-------------------+        bind/unbind/方向通知       +-----------------------+
|  XmagicActivity   |  ------------------------------>  |  TEBeautyVolcAdapter  |
|   (业务页面)      |                                   |     (适配器)          |
+-------------------+                                   +-----------+-----------+
        |                                                           |
        | TEPanelView / TEBeautyKit                                  |
        v                                                           v
+-------------------+    registerLocalVideoProcessor    +-----------------------+
|     XmagicApi     |  <------------------------------  |       RTCVideo        |
|   (腾讯特效)      |     XmagicApi.process(texId)      |     (火山 RTC)        |
+-------------------+                                   +-----------------------+
                                                                    |
                                  TextureProcessor / TESensorManager 协助方向适配
```

核心思路：

1. 火山 RTC 提供视频前处理回调 `IVideoProcessor`，返回采集到的 `VideoFrame`（含 OpenGL Texture）。
2. 适配层把这个 Texture 旋转到"人脸正向"，交给腾讯特效 `XmagicApi.process()` 处理。
3. 处理完成的 Texture 再旋转回原方向，封装为新的 `VideoFrame` 回填给火山 RTC，参与编码 / 渲染。

---

## 2. 业务层：`XmagicActivity`

它的工作可以简化为 5 步（在 `onCreate` 中）：

```java
// 1. 创建 RTC 引擎
rtcVideo = RTCVideo.createRTCVideo(this, LicenseConstant.APP_ID, videoEventHandler, null, null);
rtcVideo.setVideoOrientation(settingsView.getSettingViewMode().videoOrientation);

// 2. 根据设备等级选择特效模式（低端机走 NORMAL，中高端走 PRO）
XmagicConstant.DeviceLevel level = TEBeautyKit.getDeviceLevel(getApplicationContext());
if (level.getValue() < XmagicConstant.DeviceLevel.DEVICE_LEVEL_MIDDLE.getValue()) {
    adapter = new TEBeautyVolcAdapter(XmagicConstant.EffectMode.NORMAL, TEBeautyKit.getResPath());
} else {
    adapter = new TEBeautyVolcAdapter(XmagicConstant.EffectMode.PRO, TEBeautyKit.getResPath());
}

// 3. 把当前的视频/屏幕方向同步给 adapter
adapter.notifyVideoOrientationChanged(...);
adapter.notifyScreenOrientationChanged(...);

// 4. 设置本地预览 + 启动采集
setLocalRenderView();
rtcVideo.startVideoCapture();

// 5. 绑定美颜（关键一步）
enableBeauty();
```

`enableBeauty()` 内部调用 `adapter.bind(...)`，并在回调里完成：
- 拿到 `XmagicApi`，包装成 `TEBeautyKit`；
- 把 `TEBeautyKit` 交给 `TEPanelView`，从而让美颜面板可以驱动具体效果；
- 退出时通过 `mBeautyKit.exportInUseSDKParam()` 保存当前美颜配置，下次进入由 `setLastParam()` 还原。

页面同时实现了 `TEBeautyAdapterSettingsView.CallBack`，将 UI 上的"前后置切换 / 屏幕方向 / 视频方向 / 是否启用美颜 / 面板显隐"统一转发给 adapter。

---

## 3. 适配层：`adapter/`

目录结构：

```
adapter/
├── api/
│   ├── ITEBeautyAdapter.java   适配器接口（bind/unbind/方向通知/状态通知）
│   └── DeviceDirection.java    设备物理方向枚举（PORTRAIT_UP / LANDSCAPE_RIGHT...）
├── impl/
│   ├── TEBeautyVolcAdapter.java  ★核心：与火山 RTC 对接
│   ├── TextureProcessor.java     纹理旋转 + 方向计算 + 调用 XmagicApi.process
│   └── TESensorManager.java      监听重力传感器，得出当前手机物理方向
└── render/                       OpenGL 纹理转换工具（旋转、镜像、OES→2D）
    ├── TextureConverter.java
    ├── TextureTransform.java / TextureFormat.java
    ├── Texture2DRenderer.java / TextureOesRenderer.java
    ├── Drawable2d.java / GlUtil.java
    └── Renderer.java / RendererManager.java
```

### 3.1 `ITEBeautyAdapter` —— 接口契约

定义了业务层"该跟适配器说什么"：

| 方法 | 说明 |
|---|---|
| `bind(context, t, callback)` | 把美颜挂到 RTC 上 |
| `unbind()` | 解绑 |
| `notifyVideoOrientationChanged` | RTC 视频方向变更（ADAPTIVE/PORTRAIT/LANDSCAPE） |
| `notifyCameraChanged(isFront)` | 前后置切换 |
| `notifyScreenOrientationChanged` | Activity 屏幕方向变更 |
| `notifyEffectStateChanged(ENABLED/DISABLED)` | 启停美颜（不释放） |
| `CallBack.onCreatedTEBeautyApi/onDestroyTEBeautyApi` | XmagicApi 创建/销毁时机回调 |

泛型 `T` 在火山实现里被特化为 `RTCVideo`。

### 3.2 `TEBeautyVolcAdapter` —— 核心适配器

**关键点 1：用火山 RTC 的本地视频前处理接口注入美颜**

```java
VideoPreprocessorConfig config = new VideoPreprocessorConfig();
config.requiredPixelFormat = VideoPixelFormat.TEXTURE_2D;
rtcVideo.registerLocalVideoProcessor(customTextureListener, config);
```

`customTextureListener` 是一个 `IVideoProcessor`，主要回调 3 个：
- `onGLEnvInitiated()` —— 火山的 GL 环境就绪，**在这里初始化 XmagicApi**。
- `processVideoFrame(VideoFrame)` —— 每一帧调用，把 textureId 交给 `TextureProcessor` 处理后封装新的 `VideoFrame` 返回。
- `onGLEnvRelease()` —— GL 环境释放，销毁 XmagicApi。

**关键点 2：处理 + 重新封装 VideoFrame**

处理后用 `GLTextureVideoFrameBuilder` 重新构造 `VideoFrame`，保留原始的 EGLContext / 时间戳 / 旋转，仅替换 textureID：

```java
GLTextureVideoFrameBuilder builder = new GLTextureVideoFrameBuilder(VideoPixelFormat.TEXTURE_2D);
builder.setEGLContext(EGL14.eglGetCurrentContext())
       .setTextureID(textureID)
       .setWidth(...).setHeight(...)
       .setRotation(frame.getRotation())
       .setTimeStampUs(frame.getTimeStampUs());
return builder.build();
```

**关键点 3：兜底初始化**

代码注释明确指出，火山 SDK 多次调用 `registerLocalVideoProcessor` 时，后续的 listener 可能不再回调 `onGLEnvInitiated`，因此 `processVideoFrame` 里也兜底调用了一次 `initBeautyApi()`，并通过 `isInitXmagicApi` 标记防止重复创建。

**关键点 4：XmagicApi 的初始化**

```java
xmagicApi = new XmagicApi(mContext, effectMode, xMagicResPath,
        (s, i) -> LightLogUtil.e(TAG, "onXmagicPropertyError code=" + i + " msg=" + s));
mTextureProcessor.setBeautyApi(xmagicApi);
```

资源路径来自 `TEBeautyKit.getResPath()`，效果模式由设备等级决定。

**关键点 5：销毁时机**

`destroyBeautyApi()` 中按顺序调用 `mTextureProcessor.onDestroy()` → 触发业务层 `onDestroyTEBeautyApi()` → `xmagicApi.onPause() & onDestroy()`。

### 3.3 `TextureProcessor` —— 方向适配 + 调用 XmagicApi

腾讯特效要求"输入纹理里的人脸是正向"，但火山推过来的 Texture 经常是横向的，因此处理流程为：

```
火山 Texture
   │  rotateAndProcessTexture(textureId, w, h, isFront, screenOrientation)
   ▼
directionRotate.convert(...)   先旋转到正向
   ▼
XmagicApi.process(...)         做美颜
   ▼
directionRevert.convert(...)   再旋转回原方向，返回给火山
```

具体旋转角度由 `getTextureRotationAngle(...)` 根据 **VideoOrientation × ScreenOrientation × 前/后置** 三维查表得到（共 ADAPTIVE / PORTRAIT / LANDSCAPE 三套规则）。

此外 `setImageOrientation(...)` 会把传感器传来的 `DeviceDirection` 映射到 `TEImageOrientation`，调用 `xmagicApi.setImageOrientation()` 让 SDK 内部的人脸识别方向与设备朝向保持一致；当手机水平朝上 / 朝下时直接忽略，避免误判。

`notifyEffectStateChanged()` 通过 `xmagicApi.onResume()/onPause()` 实现"软启停"（不释放 SDK，仅暂停推理）。

### 3.4 `TESensorManager` —— 物理方向

注册 `TYPE_ACCELEROMETER` 加速度传感器，用 (x,y,z) 判断设备处于：`PORTRAIT_UP / DOWN`、`LANDSCAPE_LEFT / RIGHT`、`HORIZONTAL_UP / DOWN` 六个方向，并通过 `EventListener` 回调给 adapter，最终影响特效引擎的人脸方向判定。

### 3.5 `render` 包

通用 OpenGL 工具集合，主要由 `TextureConverter` 对外暴露两个能力：
- `oes2Rgba()`：OES 纹理转 2D；
- `convert(srcID, w, h, rotation, flipV, flipH)`：旋转 + 镜像。

火山接入路径中输入已经是 `TEXTURE_2D`，所以仅用到 `convert()` 做旋转。

---

## 4. Xmagic 模块

这是一个 Android Library 模块，主要承担 **资源 + UI 面板 + 通用 Demo 工具** 的角色。

### 4.1 依赖（`Xmagic/build.gradle`）

```gradle
api 'com.tencent.mediacloud:TEBeautyKit:4.2.0.6'          // 美颜 UI 套件 TEBeautyKit / TEPanelView
api 'com.tencent.mediacloud:TencentEffect_S1-04:4.2.0.15' // 腾讯特效核心 XmagicApi
api "com.volcengine:VolcEngineRTC:$VOLCENGINE"            // 火山 RTC
```

### 4.2 资源 `src/main/assets/`

| 目录 | 作用 |
|---|---|
| `beauty_panel/*.json` | 面板配置（beauty/lut/motions/segmentation/light_makeup） |
| `beauty_panel/panel_icon/` | 面板图标 |
| `MotionRes/` | 动效素材（2D/3D/手势/分割/美妆…） |
| `lut/` | 滤镜 LUT 图 |

`TEPanelView.showView(this)` 会按这些 json 渲染默认的腾讯标准美颜面板。

 

---

