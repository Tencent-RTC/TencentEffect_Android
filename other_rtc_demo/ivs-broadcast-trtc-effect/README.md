# IVS × TRTC × 腾讯特效 集成 Demo

本 Demo 演示了在 **AWS IVS（Amazon Interactive Video Service）** 推流 SDK 中集成
**腾讯特效（XMagic / TEBeautyKit）** 美颜能力的完整方案。

由于 AWS IVS 的 Android Broadcast SDK 在使用「自定义图像源（Custom Image Source）」时
**自身不提供相机模块**，因此本 Demo 引入腾讯 **TRTC（LiteAVSDK_Professional）** 来负责
相机采集，再把每一帧 GL 纹理交给腾讯特效做美颜，最终把美颜后的纹理通过
IVS 的 `ImageInputSource` 送入推流管线进行编码与渲染。

> 一句话概括：**TRTC 出图 → 腾讯特效美颜 → IVS 推流**。

---

## 一、Demo 主要功能

1. **使用 IVS 推流 SDK 构建 Broadcast Session**
   - 通过 `BroadcastSession` + `BroadcastConfiguration` 自建推流会话；
   - 注册自定义图像源（`createImageInputSource`）和自定义音频源（`createAudioInputSource`）。
2. **使用 TRTC 接管相机采集**
   - 通过 `TRTCCloud.startLocalPreview` 启动相机；
   - 借助 `setLocalVideoProcessListener` 拿到每一帧 OpenGL 纹理并允许二次加工。
3. **使用腾讯特效（TEBeautyKit）做实时美颜**
   - 在 TRTC 给出的 GL 上下文中创建 `TEBeautyKit`，对纹理进行
     美颜 / 美型 / 滤镜 / 美妆 / 贴纸 / 手势 / 抠像（绿幕、虚化、自定义背景）等处理；
   - 通过 `TEPanelView` 呈现腾讯特效官方面板 UI，方便快速体验所有效果。
4. **将美颜后的画面送回 IVS**
   - 把 TRTC 在 `onProcessVideoFrame` 中输出的纹理直接绘制到 IVS 的
     `ImageInputSource` 提供的 `Surface` 上，实现"美颜后的画面 = 推流画面"。
5. **自定义抠像背景**
   - 支持从系统相册选择自定义图片或视频作为分割（Segmentation）背景；
   - 通过 SAF（`ACTION_OPEN_DOCUMENT`）选择，然后用源流 MD5 命名缓存到
     App 私有目录后再传给 SDK，避免 SAF Uri 不能直接被 SDK 读取的问题。

---

## 二、运行方式

### 1. 环境要求

| 项目                         | 要求                          |
| -------------------------- | --------------------------- |
| IDE                        | Android Studio Hedgehog 或更新 |
| JDK                        | JDK 11                      |
| Gradle                     | 项目自带 wrapper，无需额外安装         |
| `compileSdk` / `targetSdk` | 35                          |
| `minSdkVersion`            | 21                          |
| 真机                         | 强烈建议使用 **真机** 调试            |

主要依赖（详见 [`dependencies.gradle`](dependencies.gradle) 与 [`app/build.gradle`](app/build.gradle)）：

- `com.amazonaws:ivs-broadcast:1.32.2`（IVS Android Broadcast SDK）
- `com.tencent.liteav:LiteAVSDK_Professional:12.6.0.17772`（TRTC）
- `com.tencent.mediacloud:TEBeautyKit:4.2.0.6`（腾讯特效 UI 套件）
- `com.tencent.mediacloud:TencentEffect_S1-04:4.2.0.15`（腾讯特效核心 SDK）

### 2. 配置腾讯特效 License

腾讯特效是商业 SDK，必须先申请 License。在 [`LicenseConstant.java`](app/src/main/java/com/amazonaws/ivs/basicbroadcast/LicenseConstant.java)中进行配置：

```java
public static final String mXMagicLicenceUrl = "xxxxxxx";
public static final String mXMagicKey = "xxxx";
```

> 📌 填写你申请的license 信息，并将app module下的build.gradle文件中的 applicationId 修改为申请license时填写的包名。

---

## 三、整体架构

### 3.1 数据流

```
 ┌──────────┐     onProcessVideoFrame     ┌────────────────┐    drawTo Surface    ┌─────────────┐
 │  TRTC    │ ──────► (GL Texture) ─────► │ TEBeautyKit    │ ──────────────────► │ IVS Custom  │
 │  Camera  │                             │ (腾讯特效)      │                      │ ImageInput  │
 └──────────┘                             └────────────────┘                      └─────────────┘
       ▲                                          ▲                                       │
       │                                          │ TEPanelView 配置                       │ Broadcast
   startLocalPreview                              │ (美颜 / 滤镜 / 抠像 ...)                │ Session
       │                                          │                                       ▼
   TXCloudVideoView(surface)                      │                              IVS 推流 / 预览
                                                  │
                                       CustomPropertyManager
                                       (自定义抠像背景：图片 / 视频)
```

关键点：

1. IVS 提供 `Surface`（来自 `imageSource.inputSurface`）；
2. TRTC 把这个 `Surface` 包成 `TXCloudVideoView` 用作本地预览输出，
   于是「TRTC 的渲染输出 = IVS 的输入」；
3. 在 TRTC 的 `setLocalVideoProcessListener` 回调中，每一帧纹理被
   `TEBeautyKit.process(...)` 处理后再写回 `outputFrame`，从而对画面做美颜。

### 3.2 模块划分

```
ivs-broadcast-trtc-effect/
├── app/
│   ├── build.gradle                    # 模块依赖：IVS、TRTC、腾讯特效
│   └── src/main/
│       ├── AndroidManifest.xml         # 权限 / 入口
│       ├── assets/
│       │   ├── beauty_panel/           # 腾讯特效面板配置 JSON
│       │   ├── MotionRes/              # 贴纸 / 美妆 / 抠像等素材
│       │   └── lut/                    # 滤镜 LUT
│       └── java/com/amazonaws/ivs/basicbroadcast/
│           ├── App.kt                  # Application + Dagger 入口
│           ├── LicenseConstant.java    # 腾讯特效 License（替换为自己的）
│           ├── activities/
│           │   ├── MenuActivity.kt     # 首页：注册腾讯特效面板 + License 鉴权 + 跳转
│           │   ├── CustomSourceActivity.kt   # 核心 Activity：串起 IVS / TRTC / 特效
│           │   └── PermissionActivity.kt     # 运行时权限封装
│           ├── effect/
│           │   ├── BeautyProcessor.java      # ★ 核心：TRTC 帧回调 + TEBeautyKit 处理
│           │   ├── CustomPropertyManager.java # 自定义抠像背景（SAF + 私有目录缓存）
│           │   └── utils/                    # Bitmap / Uri 工具类
│           ├── viewModel/
│           │   └── CustomSourceViewModel.kt  # IVS BroadcastSession 创建 / 释放
│           ├── common/                       # AudioRecorder 等通用工具
│           ├── data/ injection/              # Dagger DI、本地缓存
│           └── adapters/                     # 列表 / 适配器
├── dependencies.gradle                 # 全局依赖
├── settings.gradle / build.gradle      # Gradle 顶层配置
└── README.md
```

---

## 四、核心代码索引

> 想快速理解集成细节，按以下顺序看 4 个文件就够了：

### 1) [`MenuActivity.kt`](app/src/main/java/com/amazonaws/ivs/basicbroadcast/activities/MenuActivity.kt)

- 注册腾讯特效面板配置（`TEPanelDataModel` 列表）；
- 调用 `TEBeautyKit.setupSDK(...)` 完成 License 鉴权；
- 鉴权通过后跳转到 `CustomSourceActivity`。

### 2) [`CustomSourceActivity.kt`](app/src/main/java/com/amazonaws/ivs/basicbroadcast/activities/CustomSourceActivity.kt)

- `attachCustomCamera()`：
  - 从 IVS 创建 `imageSource`，拿到 `inputSurface`；
  - 创建 `TRTCCloud` 实例，并通过 `setLocalVideoProcessListener` 注册自定义美颜处理器
    `BeautyProcessor`；
- `startPreview()`：把 `inputSurface` 包装成 `TXCloudVideoView` 作为 TRTC 的本地预览输出
  ——这是「TRTC 输出 = IVS 输入」的关键一步；
- `attachCustomMicrophone()`：通过 `AudioRecorder` 把 PCM 数据写入 IVS 的 `AudioDevice`；
- `onClickCustomSeg(...)`：用户点击「自定义抠像背景」时，转交给 `CustomPropertyManager`
  打开 SAF 文件选择器。

### 3) [`BeautyProcessor.java`](app/src/main/java/com/amazonaws/ivs/basicbroadcast/effect/BeautyProcessor.java)

TRTC `TRTCVideoFrameListener` 的实现，桥接 TRTC 与腾讯特效：

```java
@Override
public void onGLContextCreated() {
    // 在 TRTC 的 GL 上下文中创建 TEBeautyKit
    this.teBeautyKit = new TEBeautyKit(context, XmagicConstant.EffectMode.PRO);
}

@Override
public int onProcessVideoFrame(TRTCVideoFrame in, TRTCVideoFrame out) {
    // 把输入纹理交给特效处理，输出纹理写回 out
    out.texture.textureId = teBeautyKit.process(
        in.texture.textureId, in.width, in.height);
    return 0;
}
```

### 4) [`CustomPropertyManager.java`](app/src/main/java/com/amazonaws/ivs/basicbroadcast/effect/CustomPropertyManager.java)

- 用 SAF（`ACTION_OPEN_DOCUMENT`）选择图片/视频，**无需运行时存储权限**；
- 选完后用 **「源流 MD5」** 作为文件名，把内容拷贝到 App 私有目录
  `cacheDir/custom_seg/seg_<md5>.<ext>`，再把本地路径交给特效 SDK；
- 写入采用 `.tmp + renameTo` 原子方式，保证「要么完整、要么不存在」，避免半成品文件被
  下次启动误用。
