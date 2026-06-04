# Integrating Tencent Effect (Xmagic) Beauty SDK into Volcano Engine RTC — Demo Code Documentation


## Quick Start
In the demo's `app` module, find the `LicenseConstant.java` class and fill in the beauty authentication info and Volcano (Byte) authentication info. Then, in the `app` module's `build.gradle`, change `applicationId` to the package name you used when applying for the beauty license.

## 1. Overall Architecture

The overall design adopts a three-layer structure: **Business Layer ←→ Adapter Layer ←→ SDK Layer (Volcano RTC + Tencent Effect)**.

```
+-------------------+        bind/unbind/orientation notify   +-----------------------+
|  XmagicActivity   |  -------------------------------------> |  TEBeautyVolcAdapter  |
|  (Business page)  |                                         |       (Adapter)       |
+-------------------+                                         +-----------+-----------+
        |                                                                 |
        | TEPanelView / TEBeautyKit                                       |
        v                                                                 v
+-------------------+    registerLocalVideoProcessor          +-----------------------+
|     XmagicApi     |  <------------------------------------  |       RTCVideo        |
|  (Tencent Effect) |     XmagicApi.process(texId)            |     (Volcano RTC)     |
+-------------------+                                         +-----------------------+
                                                                          |
                              TextureProcessor / TESensorManager assist with orientation
```

Core idea:

1. Volcano RTC provides a video pre-processing callback `IVideoProcessor` that returns the captured `VideoFrame` (containing an OpenGL Texture).
2. The adapter layer rotates this Texture so that the face is "upright", and hands it to Tencent Effect's `XmagicApi.process()` for processing.
3. The processed Texture is rotated back to its original orientation, wrapped into a new `VideoFrame`, and returned to Volcano RTC for encoding / rendering.

---

## 2. Business Layer: `XmagicActivity`

Its work can be simplified into 5 steps (in `onCreate`):

```java
// 1. Create the RTC engine
rtcVideo = RTCVideo.createRTCVideo(this, LicenseConstant.APP_ID, videoEventHandler, null, null);
rtcVideo.setVideoOrientation(settingsView.getSettingViewMode().videoOrientation);

// 2. Choose the effect mode based on device tier
//    (low-end devices use NORMAL, mid-to-high-end devices use PRO)
XmagicConstant.DeviceLevel level = TEBeautyKit.getDeviceLevel(getApplicationContext());
if (level.getValue() < XmagicConstant.DeviceLevel.DEVICE_LEVEL_MIDDLE.getValue()) {
    adapter = new TEBeautyVolcAdapter(XmagicConstant.EffectMode.NORMAL, TEBeautyKit.getResPath());
} else {
    adapter = new TEBeautyVolcAdapter(XmagicConstant.EffectMode.PRO, TEBeautyKit.getResPath());
}

// 3. Sync the current video / screen orientation to the adapter
adapter.notifyVideoOrientationChanged(...);
adapter.notifyScreenOrientationChanged(...);

// 4. Set up local preview + start capture
setLocalRenderView();
rtcVideo.startVideoCapture();

// 5. Bind the beauty effect (the key step)
enableBeauty();
```

`enableBeauty()` internally calls `adapter.bind(...)`, and inside the callback:
- It obtains the `XmagicApi` and wraps it into `TEBeautyKit`.
- It hands `TEBeautyKit` to `TEPanelView`, so the beauty panel can drive the actual effects.
- On exit, `mBeautyKit.exportInUseSDKParam()` saves the current beauty configuration, which `setLastParam()` will restore the next time the user enters.

The page also implements `TEBeautyAdapterSettingsView.CallBack`, which uniformly forwards UI actions — "front/back camera switch / screen orientation / video orientation / enable beauty / panel show/hide" — to the adapter.

---

## 3. Adapter Layer: `adapter/`

Directory structure:

```
adapter/
├── api/
│   ├── ITEBeautyAdapter.java   Adapter interface (bind/unbind/orientation notify/state notify)
│   └── DeviceDirection.java    Physical device orientation enum (PORTRAIT_UP / LANDSCAPE_RIGHT...)
├── impl/
│   ├── TEBeautyVolcAdapter.java  ★ Core: integrates with Volcano RTC
│   ├── TextureProcessor.java     Texture rotation + orientation calculation + invokes XmagicApi.process
│   └── TESensorManager.java      Listens to the gravity sensor to determine the phone's physical orientation
└── render/                       OpenGL texture conversion utilities (rotate, mirror, OES→2D)
    ├── TextureConverter.java
    ├── TextureTransform.java / TextureFormat.java
    ├── Texture2DRenderer.java / TextureOesRenderer.java
    ├── Drawable2d.java / GlUtil.java
    └── Renderer.java / RendererManager.java
```

### 3.1 `ITEBeautyAdapter` — The Interface Contract

Defines what the business layer "needs to tell the adapter":

| Method | Description |
|---|---|
| `bind(context, t, callback)` | Attach the beauty effect to RTC |
| `unbind()` | Unbind |
| `notifyVideoOrientationChanged` | RTC video orientation changed (ADAPTIVE/PORTRAIT/LANDSCAPE) |
| `notifyCameraChanged(isFront)` | Front/back camera switch |
| `notifyScreenOrientationChanged` | Activity screen orientation changed |
| `notifyEffectStateChanged(ENABLED/DISABLED)` | Enable/disable beauty (without releasing) |
| `CallBack.onCreatedTEBeautyApi/onDestroyTEBeautyApi` | Callbacks for XmagicApi creation/destruction timing |

The generic `T` is specialized as `RTCVideo` in the Volcano implementation.

### 3.2 `TEBeautyVolcAdapter` — The Core Adapter

**Key Point 1: Inject the beauty effect via Volcano RTC's local video pre-processing interface**

```java
VideoPreprocessorConfig config = new VideoPreprocessorConfig();
config.requiredPixelFormat = VideoPixelFormat.TEXTURE_2D;
rtcVideo.registerLocalVideoProcessor(customTextureListener, config);
```

`customTextureListener` is an `IVideoProcessor` with 3 main callbacks:
- `onGLEnvInitiated()` — Volcano's GL environment is ready; **initialize XmagicApi here**.
- `processVideoFrame(VideoFrame)` — Called for each frame; passes textureId to `TextureProcessor`, then wraps and returns a new `VideoFrame`.
- `onGLEnvRelease()` — GL environment is released; destroy XmagicApi.

**Key Point 2: Process and re-wrap the VideoFrame**

After processing, use `GLTextureVideoFrameBuilder` to build a new `VideoFrame`, preserving the original EGLContext / timestamp / rotation, and only replacing the textureID:

```java
GLTextureVideoFrameBuilder builder = new GLTextureVideoFrameBuilder(VideoPixelFormat.TEXTURE_2D);
builder.setEGLContext(EGL14.eglGetCurrentContext())
       .setTextureID(textureID)
       .setWidth(...).setHeight(...)
       .setRotation(frame.getRotation())
       .setTimeStampUs(frame.getTimeStampUs());
return builder.build();
```

**Key Point 3: Fallback initialization**

The code comments explicitly note that when the Volcano SDK calls `registerLocalVideoProcessor` multiple times, subsequent listeners may no longer trigger `onGLEnvInitiated`. Therefore `processVideoFrame` also calls `initBeautyApi()` once as a fallback, and uses the `isInitXmagicApi` flag to prevent duplicate creation.

**Key Point 4: XmagicApi initialization**

```java
xmagicApi = new XmagicApi(mContext, effectMode, xMagicResPath,
        (s, i) -> LightLogUtil.e(TAG, "onXmagicPropertyError code=" + i + " msg=" + s));
mTextureProcessor.setBeautyApi(xmagicApi);
```

The resource path comes from `TEBeautyKit.getResPath()`, and the effect mode is decided by the device tier.

**Key Point 5: Destruction timing**

`destroyBeautyApi()` calls in order: `mTextureProcessor.onDestroy()` → triggers the business layer's `onDestroyTEBeautyApi()` → `xmagicApi.onPause() & onDestroy()`.

### 3.3 `TextureProcessor` — Orientation Adaptation + Invoking XmagicApi

Tencent Effect requires the face in the input texture to be "upright", but the texture pushed in by Volcano is often in landscape, so the processing flow is:

```
Volcano Texture
   │  rotateAndProcessTexture(textureId, w, h, isFront, screenOrientation)
   ▼
directionRotate.convert(...)   First rotate to upright
   ▼
XmagicApi.process(...)         Apply beauty
   ▼
directionRevert.convert(...)   Rotate back to the original orientation, return to Volcano
```

The specific rotation angle is obtained by `getTextureRotationAngle(...)` via a 3-dimensional lookup based on **VideoOrientation × ScreenOrientation × Front/Back camera** (with three rule sets: ADAPTIVE / PORTRAIT / LANDSCAPE).

In addition, `setImageOrientation(...)` maps the `DeviceDirection` reported by the sensor to a `TEImageOrientation`, and calls `xmagicApi.setImageOrientation()` so the face-detection orientation inside the SDK stays consistent with the device orientation. When the phone is horizontally facing up / down, this is simply ignored to avoid misjudgment.

`notifyEffectStateChanged()` uses `xmagicApi.onResume()/onPause()` to implement a "soft start/stop" (the SDK is not released, only inference is paused).

### 3.4 `TESensorManager` — Physical Orientation

Registers the `TYPE_ACCELEROMETER` sensor, and uses (x, y, z) to determine which of six orientations the device is in: `PORTRAIT_UP / DOWN`, `LANDSCAPE_LEFT / RIGHT`, `HORIZONTAL_UP / DOWN`. It reports the result back to the adapter via `EventListener`, which ultimately affects the effect engine's face-orientation judgment.

### 3.5 The `render` Package

A general-purpose OpenGL utility collection. `TextureConverter` exposes two main capabilities:
- `oes2Rgba()`: Convert an OES texture to a 2D texture.
- `convert(srcID, w, h, rotation, flipV, flipH)`: Rotate + mirror.

In the Volcano integration path, the input is already `TEXTURE_2D`, so only `convert()` is used for rotation.

---

## 4. Xmagic Module

This is an Android Library module, primarily playing the role of **resources + UI panel + general demo utilities**.

### 4.1 Dependencies (`Xmagic/build.gradle`)

```gradle
api 'com.tencent.mediacloud:TEBeautyKit:4.2.0.6'          // Beauty UI kit: TEBeautyKit / TEPanelView
api 'com.tencent.mediacloud:TencentEffect_S1-04:4.2.0.15' // Tencent Effect core: XmagicApi
api "com.volcengine:VolcEngineRTC:$VOLCENGINE"            // Volcano RTC
```

### 4.2 Resources `src/main/assets/`

| Directory | Purpose |
|---|---|
| `beauty_panel/*.json` | Panel configurations (beauty/lut/motions/segmentation/light_makeup) |
| `beauty_panel/panel_icon/` | Panel icons |
| `MotionRes/` | Motion-effect assets (2D/3D/gesture/segmentation/makeup…) |
| `lut/` | Filter LUT images |

`TEPanelView.showView(this)` renders the default Tencent standard beauty panel based on these json files.

 

---

