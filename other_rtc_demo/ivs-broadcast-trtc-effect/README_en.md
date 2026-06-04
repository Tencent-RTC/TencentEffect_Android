# IVS × TRTC × Tencent Effect Integration Demo

This demo shows a complete solution for integrating the **Tencent Effect (XMagic / TEBeautyKit)** beauty capabilities into the **AWS IVS (Amazon Interactive Video Service)** broadcast SDK.

Because the AWS IVS Android Broadcast SDK does **not provide a camera module of its own** when using a "Custom Image Source", this demo brings in Tencent **TRTC (LiteAVSDK_Professional)** to handle camera capture, hands each GL texture frame to Tencent Effect for beautification, and finally feeds the beautified texture into the IVS broadcast pipeline via IVS's `ImageInputSource` for encoding and rendering.

> In one sentence: **TRTC produces frames → Tencent Effect beautifies → IVS broadcasts**.

---

## 1. Main Features of the Demo

1. **Build a Broadcast Session with the IVS broadcast SDK**
   - Use `BroadcastSession` + `BroadcastConfiguration` to build a custom broadcast session;
   - Register a custom image source (`createImageInputSource`) and a custom audio source (`createAudioInputSource`).
2. **Use TRTC to take over camera capture**
   - Use `TRTCCloud.startLocalPreview` to start the camera;
   - Use `setLocalVideoProcessListener` to obtain each OpenGL texture frame and allow secondary processing.
3. **Use Tencent Effect (TEBeautyKit) for real-time beautification**
   - Create `TEBeautyKit` within the GL context provided by TRTC, and process the texture for beauty / face shaping / filters / makeup / stickers / gestures / segmentation (green screen, blur, custom background), etc.;
   - Use `TEPanelView` to present the official Tencent Effect panel UI for quickly experiencing all effects.
4. **Send the beautified frames back to IVS**
   - Directly draw the texture output by TRTC's `onProcessVideoFrame` onto the `Surface` provided by IVS's `ImageInputSource`, so that "the beautified image = the broadcast image".
5. **Custom segmentation background**
   - Support choosing a custom image or video from the system gallery as the segmentation background;
   - Pick via SAF (`ACTION_OPEN_DOCUMENT`), then cache to the App's private directory with a filename derived from the source-stream's MD5, and only then hand it to the SDK — to avoid the issue that SAF Uris cannot be read directly by the SDK.

---

## 2. How to Run

### 1. Environment Requirements

| Item                       | Requirement                          |
| -------------------------- | ------------------------------------ |
| IDE                        | Android Studio Hedgehog or newer     |
| JDK                        | JDK 11                               |
| Gradle                     | The wrapper is included; no extra installation needed |
| `compileSdk` / `targetSdk` | 35                                   |
| `minSdkVersion`            | 21                                   |
| Device                     | Strongly recommend debugging on a **real device** |

Main dependencies (see [`dependencies.gradle`](dependencies.gradle) and [`app/build.gradle`](app/build.gradle)):

- `com.amazonaws:ivs-broadcast:1.32.2` (IVS Android Broadcast SDK)
- `com.tencent.liteav:LiteAVSDK_Professional:12.6.0.17772` (TRTC)
- `com.tencent.mediacloud:TEBeautyKit:4.2.0.6` (Tencent Effect UI Kit)
- `com.tencent.mediacloud:TencentEffect_S1-04:4.2.0.15` (Tencent Effect Core SDK)

### 2. Configure the Tencent Effect License

Tencent Effect is a commercial SDK and requires a license. Configure it in [`LicenseConstant.java`](app/src/main/java/com/amazonaws/ivs/basicbroadcast/LicenseConstant.java):

```java
public static final String mXMagicLicenceUrl = "xxxxxxx";
public static final String mXMagicKey = "xxxx";
```

> 📌 Fill in the license info you applied for, and change the `applicationId` in the `app` module's `build.gradle` to the package name you used when applying for the license.

---

## 3. Overall Architecture

### 3.1 Data Flow

```
 ┌──────────┐     onProcessVideoFrame     ┌────────────────┐    drawTo Surface    ┌─────────────┐
 │  TRTC    │ ──────► (GL Texture) ─────► │ TEBeautyKit    │ ──────────────────► │ IVS Custom  │
 │  Camera  │                             │ (Tencent FX)   │                      │ ImageInput  │
 └──────────┘                             └────────────────┘                      └─────────────┘
       ▲                                          ▲                                       │
       │                                          │ TEPanelView config                    │ Broadcast
   startLocalPreview                              │ (beauty / filter / segmentation ...)  │ Session
       │                                          │                                       ▼
   TXCloudVideoView(surface)                      │                          IVS Broadcast / Preview
                                                  │
                                       CustomPropertyManager
                                       (Custom segmentation background: image / video)
```

Key points:

1. IVS provides a `Surface` (from `imageSource.inputSurface`).
2. TRTC wraps this `Surface` into a `TXCloudVideoView` and uses it as the local preview output, so that "TRTC's render output = IVS's input".
3. In TRTC's `setLocalVideoProcessListener` callback, each frame's texture is processed by `TEBeautyKit.process(...)` and then written back to `outputFrame`, thereby beautifying the frame.

### 3.2 Module Layout

```
ivs-broadcast-trtc-effect/
├── app/
│   ├── build.gradle                    # Module dependencies: IVS, TRTC, Tencent Effect
│   └── src/main/
│       ├── AndroidManifest.xml         # Permissions / entry
│       ├── assets/
│       │   ├── beauty_panel/           # Tencent Effect panel configuration JSON
│       │   ├── MotionRes/              # Stickers / makeup / segmentation assets
│       │   └── lut/                    # Filter LUTs
│       └── java/com/amazonaws/ivs/basicbroadcast/
│           ├── App.kt                  # Application + Dagger entry
│           ├── LicenseConstant.java    # Tencent Effect License (replace with your own)
│           ├── activities/
│           │   ├── MenuActivity.kt     # Home page: register Tencent Effect panel + License auth + navigate
│           │   ├── CustomSourceActivity.kt   # Core Activity: ties together IVS / TRTC / Effect
│           │   └── PermissionActivity.kt     # Runtime permission wrapper
│           ├── effect/
│           │   ├── BeautyProcessor.java      # ★ Core: TRTC frame callback + TEBeautyKit processing
│           │   ├── CustomPropertyManager.java # Custom segmentation background (SAF + private-dir cache)
│           │   └── utils/                    # Bitmap / Uri utilities
│           ├── viewModel/
│           │   └── CustomSourceViewModel.kt  # IVS BroadcastSession creation / release
│           ├── common/                       # AudioRecorder and other common utilities
│           ├── data/ injection/              # Dagger DI, local cache
│           └── adapters/                     # Lists / adapters
├── dependencies.gradle                 # Global dependencies
├── settings.gradle / build.gradle      # Gradle top-level configuration
└── README.md
```

---

## 4. Core Code Index

> To quickly understand the integration details, reading these 4 files in order is enough:

### 1) [`MenuActivity.kt`](app/src/main/java/com/amazonaws/ivs/basicbroadcast/activities/MenuActivity.kt)

- Registers the Tencent Effect panel configurations (a `TEPanelDataModel` list);
- Calls `TEBeautyKit.setupSDK(...)` to perform License authentication;
- Navigates to `CustomSourceActivity` after authentication succeeds.

### 2) [`CustomSourceActivity.kt`](app/src/main/java/com/amazonaws/ivs/basicbroadcast/activities/CustomSourceActivity.kt)

- `attachCustomCamera()`:
  - Create `imageSource` from IVS, obtain `inputSurface`;
  - Create a `TRTCCloud` instance, and register the custom beauty processor `BeautyProcessor` via `setLocalVideoProcessListener`.
- `startPreview()`: wraps `inputSurface` into a `TXCloudVideoView` as TRTC's local preview output —— this is the key step that makes "TRTC output = IVS input".
- `attachCustomMicrophone()`: uses `AudioRecorder` to write PCM data into IVS's `AudioDevice`.
- `onClickCustomSeg(...)`: when the user clicks "Custom segmentation background", delegates to `CustomPropertyManager` to open the SAF file picker.

### 3) [`BeautyProcessor.java`](app/src/main/java/com/amazonaws/ivs/basicbroadcast/effect/BeautyProcessor.java)

The implementation of TRTC's `TRTCVideoFrameListener`, bridging TRTC and Tencent Effect:

```java
@Override
public void onGLContextCreated() {
    // Create TEBeautyKit within TRTC's GL context
    this.teBeautyKit = new TEBeautyKit(context, XmagicConstant.EffectMode.PRO);
}

@Override
public int onProcessVideoFrame(TRTCVideoFrame in, TRTCVideoFrame out) {
    // Pass the input texture to the effect for processing; write the output texture back to out
    out.texture.textureId = teBeautyKit.process(
        in.texture.textureId, in.width, in.height);
    return 0;
}
```

### 4) [`CustomPropertyManager.java`](app/src/main/java/com/amazonaws/ivs/basicbroadcast/effect/CustomPropertyManager.java)

- Uses SAF (`ACTION_OPEN_DOCUMENT`) to pick an image/video, **no runtime storage permission required**;
- After picking, uses the **"source-stream MD5"** as the filename to copy the content into the App's private directory `cacheDir/custom_seg/seg_<md5>.<ext>`, then hands the local path to the effect SDK;
- Writes use the `.tmp + renameTo` atomic pattern, ensuring "either complete or not exist" so that half-written files are not mistakenly reused on the next launch.
