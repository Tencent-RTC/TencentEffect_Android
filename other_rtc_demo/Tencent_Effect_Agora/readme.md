# Tencent Effect × Agora 集成 Demo 模块说明文档

> 本文档面向"参考此 Demo 接入腾讯特效（Xmagic）到声网 Agora RTC"的 Android 开发者。
> 文档梳理了 Demo 的整体结构、核心模块及关键调用链路，帮助你快速理解 Demo 并将其裁剪到自己的工程中。

---

## 快速运行demo

在项目中app module下找到LicenseConstant.java 类。填写自己的美颜license 信息和 声网appid ,然后在app module的build.gradle文件中奖applicationId 的值修改为美颜license 对应的包名称。

## 1. 项目整体结构

项目由 3 个 Gradle 模块组成（详见 [settings.gradle](settings.gradle) 与 [build.gradle](build.gradle)）：

| 模块              | 角色        | 说明                                                                                               |
|:--------------- |:--------- |:------------------------------------------------------------------------------------------------ |
| `app`           | 业务/Demo 层 | Demo 的入口（Activity）、Agora RTC 接入、`IVideoFrameObserver` 自实现的 "纯手工" 接入方案。                           |
| `agora_adapter` | 适配层（SDK）  | 腾讯特效官方提供的 Agora 适配 SDK：`TEBeautyAgoraAdapter`，封装了 Texture 处理与方向旋转，开发者只需"调一个 bind 即可接入"。          |
| `Xmagic`        | UI/工具层    | `TEPanelView` 美颜面板的设置页 `TEBeautyAdapterSettingsView`，自定义抠图属性管理器 `CustomPropertyManager`，以及部分工具类。 |

核心依赖（见 [build.gradle](build.gradle)）：

```groovy
effect    = "com.tencent.mediacloud:TencentEffect_S1-04:4.2.0.15" // 腾讯特效核心 SDK
beautykit = "com.tencent.mediacloud:TEBeautyKit:4.2.0.6"          // 腾讯特效 UI 套件
agorartc  = "io.agora.rtc:full-sdk:4.3.1"                          // 声网 RTC SDK
```

---

## 2. Demo 提供的两种接入方式

Demo 同时演示了 **两种** 把腾讯特效接入 Agora 的方式，开发者可根据自身工程情况二选一：

| 方式                                     | 入口 Activity                                                                                           | 关键依赖                                           | 适用场景                                              |
|:-------------------------------------- |:----------------------------------------------------------------------------------------------------- |:---------------------------------------------- |:------------------------------------------------- |
| ① 直接使用 Agora `IVideoFrameObserver`（手工） | [MainActivity.java](app/src/main/java/com/tencent/effect/agora/demo/activity/MainActivity.java)       | 仅依赖 `app` 模块下的 `observer` 包                    | 需要对 VideoFrame 处理流程有完全掌控、或要在已有的自定义视频处理流水线中做改造的场景。 |
| ② 使用 Tencent 官方 Agora 适配器              | [AdapterActivity.java](app/src/main/java/com/tencent/effect/agora/demo/activity/AdapterActivity.java) | 依赖 `agora_adapter` 模块中的 `TEBeautyAgoraAdapter` | 推荐方式：一行 `bind` 即接入，无需自己处理 buffer 转换、镜像、旋转等问题。     |

## 3. 启动与初始化模块

### 3.1 [DemoApplication.java](app/src/main/java/com/tencent/effect/agora/demo/DemoApplication.java)

应用启动时完成腾讯特效初始化，关键工作有三件：

1. 通过 `TEBeautyKit.setResPath(...)` 指定特效资源解压目录（`xmagic_dir`）。
2. 通过 `TEPanelViewResModel` + `TEUIConfig` 设置 UI 面板使用的素材包（`S1_04` 套餐：美颜、滤镜、美妆、动效、分割）。
3. 通过 `TEBeautyKit.setTELicense(...)` 进行预鉴权（License）。

### 3.2 [SplashActivity.java](app/src/main/java/com/tencent/effect/agora/demo/activity/SplashActivity.java)

负责：

- 申请 `RECORD_AUDIO` / `CAMERA` / `READ_EXTERNAL_STORAGE` 权限。
- 调用 `TEBeautyKit.copyRes(...)` 把 assets 中的特效资源解压到沙箱目录（首次启动）。
- 再次调用 `TEBeautyKit.setTELicense(...)` 完成正式鉴权，鉴权成功后才能进入两个示例页面。

### 3.3 [LicenseConstant.java](app/src/main/java/com/tencent/effect/agora/demo/beauty/LicenseConstant.java)

存放三个常量，**接入方需要替换为自己的**：

```java
mXMagicLicenceUrl  // 腾讯特效 License URL
mXMagicKey         // 腾讯特效 License Key
appId              // 声网 Agora 的 App ID
```

---

## 4. 方式①：基于 IVideoFrameObserver 的手工接入

> 入口：[MainActivity.java](app/src/main/java/com/tencent/effect/agora/demo/activity/MainActivity.java)

### 4.1 模块组成

位于 `app/src/main/java/com/tencent/effect/agora/demo/observer/` 下：

```
observer/
├── AgoraVideoFrameObserver.java          // 实现 IVideoFrameObserver 的核心类（捕获回调）
├── AgoraVideoFrameObserverListener.java  // 回调接口（onProcessVideoFrame 等）
├── AgoraVideoFrameObserverHelper.java    // 镜像策略（前/后置 × 本地/远端镜像组合）
├── AgoraTextureProvider.java             // 仅在方式②中使用，这里忽略
└── converter/
    ├── IAgoraTextureConverter.java       // VideoFrame → 2D 纹理 ID 的统一抽象
    ├── AgoraTextureBufferConverter.java  // TextureBuffer 类型走这里（OES/2D 纹理 + Matrix 校正）
    ├── AgoraNV21BufferConverter.java     // I420/小分辨率 走这里（YUV → 2D 纹理）
    └── AgoraVideoFrameConvertListener.java
```

业务侧美颜处理：

```
beauty/
└── TencentBeautyProcessor.java           // 持有 TEBeautyKit，实现 onProcessVideoFrame 走美颜
```

---

## 5. 方式②：使用 TEBeautyAgoraAdapter（推荐）

> 入口：[AdapterActivity.java](app/src/main/java/com/tencent/effect/agora/demo/activity/AdapterActivity.java)

### 5.1 模块组成

`agora_adapter` 模块（包名 `com.tencent.effect.adapter.agora`）：

```
api/
├── ITEBeautyAdapter.java          // 适配器主接口（bind/unbind/notifyXxx）
├── IAgoraTextureProvider.java     // 由业务层实现：对接 Agora 引擎的 Provider
├── AgoraCustomTextureListener.java// 适配器回调（GL 创建/纹理处理/销毁）
└── DeviceDirection.java           // 设备方向枚举（Sensor 用）
impl/
├── TEBeautyAgoraAdapter.java      // 适配器主实现：内部维护 XmagicApi
├── TextureProcessor.java          // 处理纹理 + 计算 TEImageOrientation
└── TESensorManager.java           // 监听加速度传感器，输出当前设备方向
```

业务侧 Provider 实现：[AgoraTextureProvider.java](app/src/main/java/com/tencent/effect/agora/demo/observer/AgoraTextureProvider.java) —— 把方式①里的 `AgoraVideoFrameObserver` 包装成 `IAgoraTextureProvider`。 

## 
