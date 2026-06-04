# Tencent Effect SDK Integrated with ZEGO RTC Demo (Android)

This project demonstrates how to integrate the **Tencent Effect (Beauty) SDK (Tencent Effect / Xmagic)** into the video stream processing pipeline of the **ZEGO RTC SDK**, and how to plug in the official **TEBeautyKit beauty panel** provided by Tencent Effect, delivering a ready-to-run integration example of "real-time audio/video + beauty / filters / makeup / motion stickers / green-screen segmentation".

## 1. Feature Overview

Capabilities implemented in this Demo:

- Use the ZEGO RTC SDK for camera capture and local preview (`TextureView`).
- Leverage ZEGO RTC's **Custom Video Processing** capability to deliver the camera-captured texture (`GL_TEXTURE_2D`) back to the business layer.
- The business layer hands the texture to the **Tencent Effect SDK (TEBeautyKit)** for processing, obtaining a processed OpenGL texture.
- Send the processed texture back to ZEGO RTC via `sendCustomVideoProcessedTextureData` for local preview / publishing.
- Integrate the **official TEBeautyKit beauty panel (TEPanelView)** to expose beauty, body shaping, makeup, filters, motion stickers, and portrait segmentation features out of the box.
- Demonstrate how to keep the beauty SDK's image orientation in sync when **switching between front/back cameras** and when the **screen orientation changes**.

---

## 2. How to Run the Demo

In the APP module, locate `LicenseConstant.java` and fill in your beauty license information and ZEGO license information in this class. Then, in the `build.gradle` of the app module, change the value of `applicationId` to the package name you used when applying for the beauty license.

## 3. Project Structure

```
Tencent_Effect_Zego/
├── build.gradle                       // Top-level Gradle config
├── settings.gradle                    // Module declarations (include :app, :Xmagic) and ZEGO Maven repo
├── app/                               // Main app module (Demo business code)
│   ├── build.gradle                   // Depends on :Xmagic module and libs/ZegoExpressEngine.aar
│   ├── libs/
│   │   └── ZegoExpressEngine.aar      // ZEGO RTC SDK
│   └── src/main/
│       ├── AndroidManifest.xml        // Permission declarations (CAMERA / RECORD_AUDIO / INTERNET, etc.)
│       ├── java/com/tencent/effect/demo/zego/
│       │   ├── MainActivity.java                // Entry: initialize Tencent Effect license, jump to effect page
│       │   ├── EffectActivity.java              // Core: create ZEGO engine + integrate Tencent Effect + beauty panel
│       │   ├── CustomVideoProcessHandler.java   // Key: ZEGO custom video processing callback, invokes beauty
│       │   └── LicenseConstant.java             // ZEGO appID/appSign + Tencent Effect License Key/Url
│       └── res/layout/
│           └── effect_layout.xml      // Preview TextureView + beauty panel container + camera switch
└── Xmagic/                            // Tencent Effect module (resources + SDK dependencies)
    ├── build.gradle                   // Integrates TEBeautyKit and TencentEffect_S1 SDK
    └── src/main/assets/
        ├── beauty_panel/              // Beauty panel JSON config + icon resources
        │   ├── beauty.json            // Basic beauty
        │   ├── beauty_template.json   // Beauty template
        │   ├── beauty_image.json      // Image quality (sharpening, clarity, etc.)
        │   ├── beauty_shape.json      // Body/face shaping
        │   ├── beauty_makeup.json     // Makeup
        │   ├── lut.json               // Filters
        │   ├── light_makeup.json      // Light makeup
        │   ├── makeup.json            // Style makeup
        │   ├── motion_2d.json         // 2D motion
        │   ├── motion_3d.json         // 3D motion
        │   ├── motion_gesture.json    // Gesture motion
        │   └── segmentation.json      // Portrait segmentation
        ├── MotionRes/                 // Motion stickers / makeup / segmentation assets
        └── lut/                       // Filter LUT images
```

---

## 4. Overall Integration Flow

The runtime call chain of this Demo:

```
MainActivity
   │ TEBeautyKit.setupSDK( License )            // 1. Authenticate Tencent Effect license
   ▼
EffectActivity.onCreate
   │ initBeautyView()                           // 2. Initialize TEPanelView (beauty panel)
   │ createEngine()                             // 3. Create ZegoExpressEngine
   │ enableEffect(true)                         // 4. Enable ZEGO custom video processing
   │   └─ enableCustomVideoProcessing(true, GL_TEXTURE_2D)
   │   └─ setCustomVideoProcessHandler(CustomVideoProcessHandler)
   ▼
CustomVideoProcessHandler.onStart                // 5. ZEGO notifies that capture has started
   │ new TEBeautyKit(context, EffectMode.PRO)    //    Create the Tencent Effect BeautyKit on the GL thread
   │ callback.onCreatedBeautyKit(beautyKit)      //    Switch back to the main thread to bind it to TEPanelView
   ▼
CustomVideoProcessHandler.onCapturedUnprocessedTextureData  // 6. Per-frame callback
   │ resultId = teBeautyKit.process(textureID, w, h)         //    Tencent Effect processes the texture
   │ expressEngine.sendCustomVideoProcessedTextureData(...)  //    Send it back to ZEGO for rendering / publishing
   ▼
CustomVideoProcessHandler.onStop                 // 7. Stop capture, release BeautyKit
   │ teBeautyKit.onPause(); teBeautyKit.onDestroy();
```

> Key point: **Tencent Effect's `TEBeautyKit` must be created, used, and destroyed on the GL thread provided by ZEGO**. This Demo naturally satisfies that constraint via the `onStart` / `onStop` / `onCapturedUnprocessedTextureData` callbacks of `IZegoCustomVideoProcessHandler`.

---

## 5. Key Code Walkthrough

### 1. License Authentication (`MainActivity.java`)

Tencent Effect license authentication must succeed before entering the effect page; otherwise creating `TEBeautyKit` will fail.

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

> In `LicenseConstant.java`, replace the values with your own `mXMagicLicenceUrl` / `mXMagicKey` obtained from the Tencent Cloud console, and the `appID` / `appSign` obtained from the ZEGO console.

### 2. Create the ZEGO Engine and Enable Custom Video Processing (`EffectActivity.java`)

```java
void createEngine() {
    ZegoEngineProfile profile = new ZegoEngineProfile();
    profile.appID    = LicenseConstant.appID;
    profile.appSign  = LicenseConstant.appSign;
    profile.scenario = ZegoScenario.BROADCAST;        // Live-broadcast scenario, adjust per business
    profile.application = getApplication();
    expressEngine = ZegoExpressEngine.createEngine(profile, null);
    expressEngine.useFrontCamera(true);
    enableEffect(true);                               // Enable custom pre-processing + integrate Tencent Effect
}

private void enableEffect(boolean enable) {
    ZegoCustomVideoProcessConfig config = new ZegoCustomVideoProcessConfig();
    config.bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;     // Pass via OpenGL texture for best performance
    expressEngine.enableCustomVideoProcessing(enable, config);
    if (enable) setProcessHandler();
}

private void setProcessHandler() {
    processHandler = new CustomVideoProcessHandler(this, expressEngine);
    processHandler.setCustomVideoProcessCallBack(beautyKit ->
            new Handler(Looper.getMainLooper()).post(() ->
                    mTEPanelView.setupWithTEBeautyKit(beautyKit)));   // Bind panel on the main thread
    expressEngine.setCustomVideoProcessHandler(processHandler);
}
```

Highlights:

- `ZegoVideoBufferType.GL_TEXTURE_2D`: Tencent Effect is best at processing OpenGL textures, and exchanging textures with ZEGO avoids repeated CPU/GPU copies.
- Once `processHandler` receives the `TEBeautyKit` instance, it **switches back to the main thread** before binding it to `TEPanelView`, since the panel is part of the UI.

### 3. ZEGO Custom Video Processing Callback (`CustomVideoProcessHandler.java`)

Inherit `IZegoCustomVideoProcessHandler` and create / process / destroy Tencent Effect on ZEGO's GL thread.

```java
@Override
public void onStart(ZegoPublishChannel channel) {
    // GL thread; create TEBeautyKit here
    teBeautyKit = new TEBeautyKit(context, XmagicConstant.EffectMode.PRO);

    // Listen to device orientation and forward it to Tencent Effect to keep face detection accurate
    teBeautyKit.setEventListener((orientation, deviceDirection) -> {
        // Front and back cameras differ in orientation, handle them separately (see the full switch in source)
    });

    if (customVideoProcessCallBack != null) {
        customVideoProcessCallBack.onCreatedBeautyKit(teBeautyKit);   // Notify the UI layer to bind the panel
    }
}

@Override
public void onCapturedUnprocessedTextureData(int textureID, int width, int height,
                                             long ts, ZegoPublishChannel channel) {
    if (teBeautyKit == null) {
        // Beauty not ready yet, pass the original texture through
        expressEngine.sendCustomVideoProcessedTextureData(textureID, width, height, ts, channel);
    } else {
        // Invoke Tencent Effect: input texture -> output new texture
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

Highlights:

- `process(textureID, width, height)`: the core Tencent Effect API; both input and output are OpenGL texture IDs.
- **Orientation sync**: inside `teBeautyKit.setEventListener`, call `setImageOrientation` based on `deviceDirection`. **The front and back cameras must be configured with different rotations**, otherwise face detection will be misaligned and motion stickers will be skewed.
- When switching between front and back cameras, `EffectActivity` calls `processHandler.setFrontCamera(isChecked)` to forward this to the orientation listener:

```java
expressEngine.useFrontCamera(isChecked);
processHandler.setFrontCamera(isChecked);
```

### 4. Integrate the Official TEBeautyKit Beauty Panel (`EffectActivity.initBeautyView`)

Tencent Effect provides `TEPanelView`, allowing you to plug in a complete beauty UI at zero cost. Developers inject the JSON configuration of the desired features via `TEUIConfig`:

```java
List<TEPanelDataModel> panelDataModels = TEUIConfig.getInstance().getPanelDataList();
panelDataModels.clear();

// Add capabilities as needed
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

Once `CustomVideoProcessHandler.onStart` has created `TEBeautyKit`, simply bind it:

```java
mTEPanelView.setupWithTEBeautyKit(beautyKit);
```

> All JSON files and icon resources are located under `Xmagic/src/main/assets/beauty_panel/`. They can be reused as-is, or freely customized by adding/removing items.

### 5. Lifecycle

`EffectActivity` only cares about starting/stopping the preview:

```java
@Override protected void onResume()  { expressEngine.startPreview(zegoCanvas); }
@Override protected void onPause()   { expressEngine.stopPreview(); }
@Override protected void onDestroy() { enableEffect(false); }   // Disable custom pre-processing -> triggers onStop to release BeautyKit
```

`TEBeautyKit.onPause` / `onDestroy` are uniformly invoked by `CustomVideoProcessHandler.onStop` on the GL thread, so the business layer does not need to manage them manually.




### Step 2. Configure the License

Replace the values in [LicenseConstant.java](app/src/main/java/com/tencent/effect/demo/zego/LicenseConstant.java):

- `appID` / `appSign`: from the ZEGO console
- `mXMagicLicenceUrl` / `mXMagicKey`: from the Tencent Cloud Visual Cube console

### Step 3. Enable Custom Pre-processing on Your RTC Page

```java
ZegoCustomVideoProcessConfig cfg = new ZegoCustomVideoProcessConfig();
cfg.bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
expressEngine.enableCustomVideoProcessing(true, cfg);
expressEngine.setCustomVideoProcessHandler(yourHandler);
```

### Step 4. Reuse `CustomVideoProcessHandler`

Simply copy [CustomVideoProcessHandler.java](app/src/main/java/com/tencent/effect/demo/zego/CustomVideoProcessHandler.java); it already takes care of:

- Creating `TEBeautyKit` in `onStart` (on the GL thread)
- Calling `process` in `onCapturedUnprocessedTextureData` to process the texture and send it back
- Releasing `TEBeautyKit` in `onStop`
- Listening via `setEventListener` to keep the device orientation in sync

### Step 5. Integrate the Beauty UI

If you want to use the official Tencent Effect panel, just copy `EffectActivity.initBeautyView()` as-is.
If you want to build your own UI, take the `TEBeautyKit` instance and call its SDK APIs directly (e.g. `setEffect(...)`, `setFilter(...)`).

### Step 6. Switch Between Front and Back Cameras

```java
expressEngine.useFrontCamera(isFront);
processHandler.setFrontCamera(isFront);   // Make sure beauty is also aware of the current camera orientation
```

---
