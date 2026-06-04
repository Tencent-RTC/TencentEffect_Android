# Tencent Effect × Qiniu Streaming (PLDroidMediaStreaming) Android Integration Example

> This project is a reference Demo for **integrating Tencent Effect (XMagic / TEBeautyKit)** for customers who **already use the Qiniu RTC / live streaming SDK (PLDroidMediaStreaming)**.
> It demonstrates how to **seamlessly insert** Tencent Effect's beauty, filters, stickers, makeup, AI segmentation and other capabilities **into Qiniu's video processing pipeline**, with minimal intrusion into the existing streaming business.

---

## 1. Project Structure

```
Tencent_Effect_Qiniu/
├── app/                              ← Qiniu streaming Demo main project
│   ├── libs/pldroid-media-streaming-3.1.6.jar   ← Qiniu streaming SDK
│   ├── src/main/jniLibs/             ← Qiniu .so files (encoding, streaming, SRT/QUIC, etc.)
│   └── src/main/java/com/qiniu/pili/droid/streaming/demo/
│       ├── StreamingApplication.java          ← Qiniu StreamingEnv.init
│       ├── MainActivity.java                  ← Tencent Effect License authentication entry
│       ├── activity/AVStreamingActivity.java  ← Streaming + beauty integration main page
│       └── tencenteffect/
│           └── SurfaceTextureCallbackImp.java ← ★ The "bridge" between the two SDKs
│
└── xmagic/                           ← Tencent Effect wrapper module (library)
    ├── build.gradle                  ← Depends on TencentEffect_S1-04 / TEBeautyKit
    ├── src/main/assets/
    │   ├── beauty_panel/             ← Panel configuration JSON (beauty/makeup/stickers/segmentation…)
    │   ├── MotionRes/                ← 2D/3D stickers, makeup, segmentation assets
    │   └── lut/                      ← Filter LUT images
    └── src/main/java/com/tencent/effect/demo/xmagic/
        ├── LicenseConstant.java      ← XMagic License URL / Key
        ├── BeautyManager.java
        ├── CustomPropertyManager.java← Custom segmentation background image picker
        └── render/
            ├── TextureConverter.java ← ★ OES↔2D / rotation / mirroring
            ├── TextureTransform.java
            └── ...                   ← GL rendering helpers
```

The main directory listing only shows the key parts. `xmagic` is an independent `com.android.library` that wraps Tencent Effect related SDK dependencies, assets, and texture conversion utilities. `app` includes it via `implementation project(':xmagic')`.

---

## 2. How to Run the Demo

Replace the URL/Key in [LicenseConstant.java](xmagic/src/main/java/com/tencent/effect/demo/xmagic/LicenseConstant.java) with your own Tencent Cloud authorization. Change the `applicationId` in [app/build.gradle](app/build.gradle) to the package name you used when applying for the beauty license.

---

## 3. How Qiniu and Tencent Effect Are Combined

The overall idea can be summed up in one sentence: **Qiniu hands out each frame's texture via `SurfaceTextureCallback`. In the callback, the demo invokes Tencent Effect's `TEBeautyKit.process(...)` to render, and then returns the result to Qiniu for encoding and streaming.** Details below.

### 3.1 Data Flow

```
Camera
  │
  ▼
Qiniu MediaStreamingManager (CameraStreamingSetting)
  │  Produces an OES texture
  ▼
SurfaceTextureCallback.onDrawFrame(textureId, w, h, …)
  │
  │  ① OES → 2D       (TextureConverter.oes2Rgba)
  │  ② Rotate / mirror (TextureConverter.convert; different angles for front/back camera)
  │  ③ Beauty processing (TEBeautyKit.process)   ← where Tencent Effect actually does the work
  │  ④ Rotate back to the original orientation (directionRevert.convert)
  ▼
Return the processed textureId to Qiniu
  │
  ▼
Qiniu encoding → streaming (RTMP / SRT / QUIC)
```

### 3.2 Key Code: [SurfaceTextureCallbackImp.java](app/src/main/java/com/qiniu/pili/droid/streaming/demo/tencenteffect/SurfaceTextureCallbackImp.java)

This is the "**solder joint**" between the two SDKs —— it implements Qiniu's `SurfaceTextureCallback` and internally drives Tencent's `TEBeautyKit`.

```java
@Override
public int onDrawFrame(int textureId, int width, int height, float[] transformMatrix) {
    if (directionRevert == null || textureConverter == null || beautyKit == null) {
        return textureId;
    }
    // ① OES (from camera) → 2D (TEBeautyKit only accepts 2D)
    int rgbaId = textureConverter.oes2Rgba(textureId, width, height);
    // ② Make the face upright: front camera 270°, back camera 90°
    int id = textureConverter.convert(rgbaId, width, height,
            this.isFrontCamera ? 270 : 90, false, false);
    // ③ Invoke Tencent Effect for beauty / makeup / stickers / segmentation…
    int processed = beautyKit.process(id, height, width);
    // ④ Rotate back to the orientation Qiniu expects, otherwise the preview will be tilted
    return directionRevert.convert(processed, height, width,
            this.isFrontCamera ? 90 : 270, false, false);
}
```

The GL context's lifecycle is also managed by Qiniu, so the creation and destruction of `TEBeautyKit` must be hooked onto Qiniu's `onSurfaceCreated` / `onSurfaceDestroyed`:

```java
@Override
public void onSurfaceCreated() {           // Qiniu GL thread
    destroyBeautyApi();
    createBeautyApi();                     // new TEBeautyKit(context, EffectMode.PRO)
}

@Override
public void onSurfaceDestroyed() {
    destroyBeautyApi();                    // beautyKit.onPause() + onDestroy()
}
```

> ⚠️ Note: Qiniu calls `onSurfaceDestroyed` when the page goes to the background, and `onSurfaceCreated` again when it returns to the foreground, which means `TEBeautyKit` will be recreated. In [AVStreamingActivity.java](app/src/main/java/com/qiniu/pili/droid/streaming/demo/activity/AVStreamingActivity.java), the demo caches the previous beauty parameters via `mBeautyKit.exportInUseSDKParam()`, and restores them with `setLastParamList(lastParams)` when the new beauty object is created, to avoid losing user settings.

### 3.3 Key Setting that Lets Qiniu Hand Us the "Raw Texture"

```java
mCameraStreamingSetting
    .setBuiltInFaceBeautyEnabled(false);   // Disable Qiniu's built-in beauty to avoid conflicts with Tencent Effect
//  When setBuiltInFaceBeautyEnabled(true) Qiniu returns Texture2D; when false it returns OES
mMediaStreamingManager.setSurfaceTextureCallback(surfaceTextureCallbackImp);
```

As long as a `SurfaceTextureCallback` is registered, Qiniu will hand each frame's texture ID to us; the texture ID we `return` is the final frame fed to the encoder.

---

## 4. How Tencent Effect Is Initialized and Runs

The whole lifecycle consists of 4 steps: **License authentication → Panel configuration → Instantiation inside the GL context → Binding with the panel**.

### 4.1 License Authentication (one-time, at startup)

File: [MainActivity.java](app/src/main/java/com/qiniu/pili/droid/streaming/demo/MainActivity.java) → `checkLicense()`

```java
TEBeautyKit.setupSDK(
    getApplicationContext(),
    LicenseConstant.mXMagicLicenceUrl,   // license URL
    LicenseConstant.mXMagicKey,          // license Key
    (code, msg) -> {
        if (code == LicenseConstant.AUTH_STATE_SUCCEED) {
            // Authentication succeeded; beauty can now be used
        }
    });
```

The License constants are defined in [xmagic/.../LicenseConstant.java](xmagic/src/main/java/com/tencent/effect/demo/xmagic/LicenseConstant.java). **Replace them with your own Tencent Cloud License when integrating for production**.

### 4.2 Panel (UI) Configuration: [AVStreamingActivity#initBeautyPanelView](app/src/main/java/com/qiniu/pili/droid/streaming/demo/activity/AVStreamingActivity.java)

Tencent Effect ships with a `TEPanelView`. Just feed it a list of `TEPanelDataModel` (pointing to JSON descriptors in assets), and the UI will automatically render the corresponding beauty categories. Which json files to use here depends on your beauty package; the demo uses the S1-04 package as an example.

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

### 4.3 Create `TEBeautyKit` on the GL Thread

`TEBeautyKit` must be created on a thread that owns an OpenGL context. This Demo reuses Qiniu's GL thread:

```java
// Inside Qiniu's onSurfaceCreated callback
beautyKit = new TEBeautyKit(context, XmagicConstant.EffectMode.PRO);

if (callBack != null) {
    callBack.onCreatedTEBeautyKit(beautyKit);  // Pass the object back to the Activity
}
```

### 4.4 Bind BeautyKit with PanelView

After `TEBeautyKit` is created, the callback returns to `AVStreamingActivity`, which immediately:

```java
mBeautyKit = beautyKit;
mPanelView.setupWithTEBeautyKit(beautyKit);   // ★ Let the panel drive the beauty effects
if (lastParams != null) {
    mBeautyKit.setLastParamList(lastParams);  // Restore previous effect settings
}
customPropertyManager.setBeautyKit(beautyKit);// Make advanced features such as custom segmentation usable
```

At this point:

- The user drags sliders on `TEPanelView` → parameters are written through `TEBeautyKit`;
- Each frame on Qiniu's GL thread calls `beautyKit.process(...)` → applies the latest parameters to the current frame;
- Qiniu takes the processed texture and continues to encode and stream it.

---

## 5. Runtime Sequence

```
App Launch
  │
  ├─ StreamingApplication.onCreate()
  │     └─ StreamingEnv.init(...)                 // Qiniu SDK initialization
  │
  └─ MainActivity
        └─ checkLicense()
              └─ TEBeautyKit.setupSDK(...)         // Tencent Effect License authentication
                                                   ↓
Enter AVStreamingActivity (streaming page)
  │
  ├─ initBeautyPanelView()                         // Set up the Tencent Effect UI panel
  ├─ new SurfaceTextureCallbackImp(this, callback) // Create the "bridge"
  ├─ initStreamingManager()
  │     ├─ new MediaStreamingManager(...)
  │     ├─ prepare(cameraSetting, micSetting, ... )
  │     └─ setSurfaceTextureCallback(bridge)       // ★ Register the bridge with Qiniu
  │
  └─ Qiniu GL thread starts
        ├─ bridge.onSurfaceCreated()
        │     └─ new TEBeautyKit(...)              // Create Tencent Effect on the GL thread
        │     └─ callback.onCreatedTEBeautyKit()
        │           └─ mPanelView.setupWithTEBeautyKit(beautyKit)
        │
        └─ Each frame: bridge.onDrawFrame(textureId,...)
              └─ TEBeautyKit.process(...)          // Beautify
              └─ return processedTextureId         // Hand back to Qiniu for encoding
```

---
