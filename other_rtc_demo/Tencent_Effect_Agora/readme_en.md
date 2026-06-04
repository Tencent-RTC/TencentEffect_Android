# Tencent Effect × Agora Integration Demo Module Documentation

> This document is for Android developers who want to "integrate Tencent Effect (Xmagic) into Agora RTC by referencing this Demo".
> It outlines the overall structure, core modules, and key invocation chains of the Demo, helping you quickly understand it and adapt it to your own project.

---

## Quick Start

In the project's `app` module, find the `LicenseConstant.java` class. Fill in your own beauty license information and Agora App ID, then in the `app` module's `build.gradle` file, change the value of `applicationId` to the package name corresponding to your beauty license.

## 1. Overall Project Structure

The project consists of 3 Gradle modules (see [settings.gradle](settings.gradle) and [build.gradle](build.gradle)):

| Module          | Role                  | Description                                                                                                                                       |
|:--------------- |:--------------------- |:------------------------------------------------------------------------------------------------------------------------------------------------- |
| `app`           | Business / Demo layer | Demo entry (Activity), Agora RTC integration, and the "fully manual" integration solution implemented via a custom `IVideoFrameObserver`.         |
| `agora_adapter` | Adapter (SDK) layer   | Tencent Effect's official Agora adapter SDK: `TEBeautyAgoraAdapter`, which encapsulates Texture handling and orientation rotation. Developers only need to "call one `bind` to integrate". |
| `Xmagic`        | UI / Utility layer    | The settings page `TEBeautyAdapterSettingsView` of the `TEPanelView` beauty panel, the custom segmentation property manager `CustomPropertyManager`, and several utility classes. |

Core dependencies (see [build.gradle](build.gradle)):

```groovy
effect    = "com.tencent.mediacloud:TencentEffect_S1-04:4.2.0.15" // Tencent Effect Core SDK
beautykit = "com.tencent.mediacloud:TEBeautyKit:4.2.0.6"          // Tencent Effect UI Kit
agorartc  = "io.agora.rtc:full-sdk:4.3.1"                          // Agora RTC SDK
```

---

## 2. Two Integration Approaches Provided by the Demo

The Demo demonstrates **two** ways to integrate Tencent Effect into Agora. Developers can choose either one based on their own project situation:

| Approach                                              | Entry Activity                                                                                          | Key Dependencies                                                | Suitable Scenario                                                                                          |
|:----------------------------------------------------- |:------------------------------------------------------------------------------------------------------- |:--------------------------------------------------------------- |:---------------------------------------------------------------------------------------------------------- |
| ① Directly use Agora's `IVideoFrameObserver` (manual) | [MainActivity.java](app/src/main/java/com/tencent/effect/agora/demo/activity/MainActivity.java)         | Depends only on the `observer` package in the `app` module      | Scenarios that require full control over the VideoFrame processing flow, or need to be adapted into an existing custom video processing pipeline. |
| ② Use Tencent's official Agora Adapter                | [AdapterActivity.java](app/src/main/java/com/tencent/effect/agora/demo/activity/AdapterActivity.java)   | Depends on `TEBeautyAgoraAdapter` in the `agora_adapter` module | Recommended approach: a single line of `bind` is enough to integrate, with no need to handle buffer conversion, mirroring, rotation, etc. yourself. |

## 3. Startup and Initialization Modules

### 3.1 [DemoApplication.java](app/src/main/java/com/tencent/effect/agora/demo/DemoApplication.java)

Initializes Tencent Effect when the application starts. The key tasks are:

1. Use `TEBeautyKit.setResPath(...)` to specify the directory (`xmagic_dir`) where effect resources will be extracted.
2. Use `TEPanelViewResModel` + `TEUIConfig` to set the asset bundle used by the UI panel (the `S1_04` package: beauty, filters, makeup, motion effects, and segmentation).
3. Use `TEBeautyKit.setTELicense(...)` to perform pre-authentication (License).

### 3.2 [SplashActivity.java](app/src/main/java/com/tencent/effect/agora/demo/activity/SplashActivity.java)

Responsibilities:

- Request `RECORD_AUDIO` / `CAMERA` / `READ_EXTERNAL_STORAGE` permissions.
- Call `TEBeautyKit.copyRes(...)` to extract the effect resources from assets to the sandbox directory (on first launch).
- Call `TEBeautyKit.setTELicense(...)` again to perform formal authentication. Only after successful authentication can users enter the two sample pages.

### 3.3 [LicenseConstant.java](app/src/main/java/com/tencent/effect/agora/demo/beauty/LicenseConstant.java)

Stores three constants that **integrators must replace with their own values**:

```java
mXMagicLicenceUrl  // Tencent Effect License URL
mXMagicKey         // Tencent Effect License Key
appId              // Agora App ID
```

---

## 4. Approach ①: Manual Integration Based on IVideoFrameObserver

> Entry: [MainActivity.java](app/src/main/java/com/tencent/effect/agora/demo/activity/MainActivity.java)

### 4.1 Module Composition

Located under `app/src/main/java/com/tencent/effect/agora/demo/observer/`:

```
observer/
├── AgoraVideoFrameObserver.java          // Core class implementing IVideoFrameObserver (capture callback)
├── AgoraVideoFrameObserverListener.java  // Callback interface (onProcessVideoFrame, etc.)
├── AgoraVideoFrameObserverHelper.java    // Mirroring strategy (front/back camera × local/remote mirror combinations)
├── AgoraTextureProvider.java             // Used only in Approach ②, ignored here
└── converter/
    ├── IAgoraTextureConverter.java       // Unified abstraction: VideoFrame → 2D Texture ID
    ├── AgoraTextureBufferConverter.java  // Handles TextureBuffer type (OES/2D texture + Matrix correction)
    ├── AgoraNV21BufferConverter.java     // Handles I420 / low-resolution (YUV → 2D texture)
    └── AgoraVideoFrameConvertListener.java
```

Business-side beauty processing:

```
beauty/
└── TencentBeautyProcessor.java           // Holds TEBeautyKit, implements onProcessVideoFrame to apply beauty effects
```

---

## 5. Approach ②: Using TEBeautyAgoraAdapter (Recommended)

> Entry: [AdapterActivity.java](app/src/main/java/com/tencent/effect/agora/demo/activity/AdapterActivity.java)

### 5.1 Module Composition

`agora_adapter` module (package name `com.tencent.effect.adapter.agora`):

```
api/
├── ITEBeautyAdapter.java          // Main adapter interface (bind/unbind/notifyXxx)
├── IAgoraTextureProvider.java     // Implemented by the business layer: a Provider connecting to the Agora engine
├── AgoraCustomTextureListener.java// Adapter callback (GL creation / texture processing / destruction)
└── DeviceDirection.java           // Device orientation enum (used by Sensor)
impl/
├── TEBeautyAgoraAdapter.java      // Main adapter implementation: maintains XmagicApi internally
├── TextureProcessor.java          // Processes textures + computes TEImageOrientation
└── TESensorManager.java           // Listens to the accelerometer sensor and outputs the current device orientation
```

Business-side Provider implementation: [AgoraTextureProvider.java](app/src/main/java/com/tencent/effect/agora/demo/observer/AgoraTextureProvider.java) — wraps the `AgoraVideoFrameObserver` from Approach ① into an `IAgoraTextureProvider`.

## 
