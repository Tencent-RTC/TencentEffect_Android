# 美颜面板 JSON 配置说明

本目录下的 JSON 文件用于描述美颜面板（`TEPanelView`）的呈现形态，包括 Tab 的数量与顺序、各 Tab 内包含的功能项，以及每个功能项的图标、名称、默认强度和对应的 SDK 效果参数。

集成方式如下：

- Android：将本目录整体放置于 `assets` 文件夹内。
- iOS：将本目录引入主工程的任意路径下。

面板样式的调整均通过修改本目录下的 JSON 配置完成，无需改动业务代码。

代码侧仅需指定入口文件与当前使用的套餐：

**Android**

```java
// 配置 index 路径与套餐
TEUIConfig.getInstance().setPanelViewResAndEffectPlanName("beauty_panel/index.json", EffectPlan.S1_07);
```

**iOS**

```objectivec
// 配置 index 路径与套餐
NSString *indexPath = [[NSBundle mainBundle] pathForResource:@"index" ofType:@"json"];
[[TEUIConfig shareInstance] setPanelViewRes:indexPath effectPlanName:TEEffectPlan_S1_07];
```

---

## 一、目录结构

配置由两类文件构成，各自承担独立职责：

```
beauty_panel/
├── index.json        ← 定义面板包含哪些 Tab
├── panel_json/       ← 定义每个 Tab 内的功能项
│   ├── beauty_template.json
│   ├── beauty.json
│   ├── beauty_image.json
│   ├── shape.json  →  beauty_base_shape.json / beauty_general_shape.json / beauty_shape.json
│   ├── beauty_body.json
│   ├── lut.json
│   ├── beauty_makeup.json
│   └── motions.json
└── panel_icon/       ← 面板所需的全部图标资源
```

配置与面板元素的对应关系如下：

```
┌──────────────────────────────────────────────┐
│  模板   美颜   美体   滤镜   美妆   特效      │ ← index.json 的 data 数组
├──────────────────────────────────────────────┤
│  [○]   [○]   [○]   [○]   [○]   [○]         │ ← panel_json/xxx.json 的 propertyList
│  关闭  美白  磨皮  红润  ...                 │
├──────────────────────────────────────────────┤
│  ──────────●───────────────  40              │ ← sdkParam.effectValue
└──────────────────────────────────────────────┘
```

> 注意：以下两个名称已在代码中固定，不得修改。入口文件必须命名为 `index.json`，面板数据目录必须命名为 `panel_json`。

---

## 二、index.json —— Tab 定义

### 完整示例

```json
{
    "defaultLanguage": "zh",
    "data": [
        {
            "name": { "zh": "模板", "en": "Template", "ja": "テンプレート" },
            "config": [ "beauty_template.json" ]
        },
        {
            "name": { "zh": "美颜", "en": "Beauty", "ja": "美顔" },
            "config": [ "beauty.json", "beauty_image.json", "shape.json" ]
        }
    ]
}
```

### 字段说明

| 字段                | 类型     | 必填  | 说明                                                                    |
| ----------------- | ------ | --- | --------------------------------------------------------------------- |
| `defaultLanguage` | String | 是   | 面板文案的默认语言，取值必须为下方 `name` 中已声明的 key。当前支持 `zh` / `en` / `ja`            |
| `data`            | 数组     | 是   | Tab 列表，数组顺序即为 Tab 自左至右的展示顺序                                           |
| `data[].name`     | 对象     | 是   | Tab 标题的多语言文案，该取值将覆盖 panel_json 文件中根节点的 `name`                         |
| `data[].config`   | 字符串数组  | 是   | 构成该 Tab 的 panel_json 文件列表。仅填写文件名，不得包含路径（代码固定从 `panel_json/` 目录下检索）    |

### 单个 Tab 配置多个文件的处理规则

当前仅「美颜」Tab 支持配置多个文件，即 `"beauty.json"`、`"beauty_image.json"`、`"shape.json"` 的组合。

以「美颜」Tab 为例，其配置了 3 个文件：

```json
"config": [ "beauty.json", "beauty_image.json", "shape.json" ]
```

上述配置将使 3 个文件中的功能项按顺序拼接，统一展示于同一 Tab 内：

```
美颜 Tab = 关闭 美白 美黑 磨皮 AI美颜 皮肤高光 红润   ← beauty.json
         + 对比度 饱和度 清晰度 锐化 亮度 ...        ← beauty_image.json
         + 大眼 瘦脸 窄脸 眼距 ...                   ← shape.json
```

拼接过程遵循以下规则：

> 自第 2 个文件起，其 `propertyList` 中的首个元素将被丢弃。

该规则的设计原因在于：每个 panel_json 的 `propertyList` 首位均为「关闭 / 无」占位项，拼接后仅需保留一个。因此：

- 向 `config` 中新增文件时，须在该文件的 `propertyList` 起始位置保留一个占位项，否则其首个有效功能项将被忽略；
- 若该文件仅独立使用、不参与拼接，则该占位项即为面板上实际展示的「关闭」按钮。

### Tab 的常见修改方式

| 需求           | 操作方式                                                                                                  |
| ------------ | ----------------------------------------------------------------------------------------------------- |
| 调整 Tab 排列顺序  | 调整 `data` 数组中元素的先后顺序                                                                                  |
| 修改 Tab 标题    | 修改 `data[].name` 中对应语言的取值                                                                             |
| 移除某个 Tab     | 删除 `data` 中对应的完整元素                                                                                    |
| 新增一个 Tab     | 在 `data` 中新增元素，并将 `config` 指向新建的 panel_json 文件                                                        |
| 合并两个 Tab     | 保留其中一个元素，并将另一元素的文件名追加至其 `config` 数组。当前仅支持 `"beauty.json"`、`"beauty_image.json"`、`"shape.json"` 三者的合并 |

---

## 三、panel_json/*.json —— 功能项定义

### 3.1 结构：嵌套式 propertyList

每个文件构成一棵配置树：根节点描述整个 Tab，`propertyList` 中为其子项，子项可继续通过 `propertyList` 声明下一级内容。

```json
{
  "name": { "zh": "美颜", "en": "Beauty", "ja": "美顔" },   // 根节点：该 Tab 的基本信息
  "uiCategory": "BEAUTY",
  "uiState": 2,
  "propertyList": [

    { "name": { "zh": "关闭" },                              // ① 占位项：未配置 sdkParam
      "icon": "beauty_panel/panel_icon/beauty/none.png" },

    { "name": { "zh": "美白" },                              // ② 含子项：点击后进入下一级
      "icon": "beauty_panel/panel_icon/beauty/beauty_whiten.png",
      "uiState": 2,
      "propertyList": [
        { "name": { "zh": "靓白" },                          //    └ 叶子节点：支持进度条调节
          "icon": "beauty_panel/panel_icon/beauty/beauty_whiten_liangbai.png",
          "sdkParam": { "effectName": "beauty.lutFoundationAlpha0", "effectValue": 40 },
          "uiState": 2 },
        { "name": { "zh": "自然" },
          "icon": "beauty_panel/panel_icon/beauty/beauty_whiten_nature.png",
          "sdkParam": { "effectName": "beauty.lutFoundationAlpha", "effectValue": 0 } }
      ]
    },

    { "name": { "zh": "AI 美颜" },                           // ③ 一级叶子节点：可直接调节
      "icon": "beauty_panel/panel_icon/beauty/beauty_smooth_gan.png",
      "sdkParam": { "effectName": "beauty.skinRetouchAI", "effectValue": 40 } }
  ]
}
```

节点类型由是否配置 `propertyList` 与 `sdkParam` 共同决定：

| `propertyList` | `sdkParam` | 节点类型与表现                              |
| -------------- | ---------- | ------------------------------------ |
| 有              | 无          | 分组节点：点击后展开子项（如「美白」下属的 4 种效果）         |
| 无              | 有          | 叶子节点（可调项）：点击即生效，支持进度条调节              |
| 无              | 无          | 占位项：对应面板中的「关闭 / 无」，点击表示不启用该类效果       |

### 3.2 字段速查表

| 字段               | 类型     | 说明                                                                             |
| ---------------- | ------ | ------------------------------------------------------------------------------ |
| `name`           | 对象     | 多语言显示名称，如 `{"zh":"磨皮","en":"Smooth Skin","ja":"美肌"}`。须包含当前生效语言的 key，否则该项将无文案展示 |
| `icon`           | String | 图标路径，详见第四节                                                                     |
| `sdkParam`       | 对象     | 该项对应的效果参数，包含 `effectName`、`effectValue`、`resourcePath` 等，仅叶子节点需要配置             |
| `propertyList`   | 数组     | 子项列表                                                                           |
| `uiCategory`     | 枚举     | 功能分类，决定渲染方式及 `effectName` 的自动填充逻辑，详见 3.3。子节点未声明时自动继承父节点取值                     |
| `uiState`        | int    | 该项的初始状态：`0` 未启用／`1` 已生效／`2` 已生效且默认选中，详见 3.4                                   |
| `resourceUri`    | String | 素材路径（滤镜图、贴纸包、妆容包、背景图等），详见第四节                                                   |
| `downloadPath`   | String | 网络素材下载后的本地存储目录。配置于根节点即可，子节点自动继承                                                |
| `hasSubTitle`    | bool   | 取值为 `true` 时，第一层子项作为分组标题渲染，面板将额外展示一行二级 Tab（`motions.json`、`beauty_makeup.json` 已启用） |
| `verticalLayout` | bool   | 取值为 `true` 时子项以纵向网格排列，默认为横向滚动                                                  |
| `paramList`      | 数组     | 仅模板文件使用：一个模板对应一组 sdkParam                                                      |
| `id`             | int    | 仅模板文件使用：模板编号，须全局唯一                                                             |
| `dataList`       | 数组     | 仅 `shape.json` 使用：按套餐分发配置，详见第六节                                                |

### 3.3 uiCategory 取值说明

| 取值                                      | 适用文件                     | 说明                                  |
| --------------------------------------- | ------------------------ | ----------------------------------- |
| `BEAUTY_TEMPLATE`                       | `beauty_template.json`   | 一键美颜套装，使用 `paramList` 替代 `sdkParam` |
| `BEAUTY`                                | `beauty.json`            | 美颜类，支持进度条调节及参数还原                    |
| `BEAUTY_IMAGE`                          | `beauty_image.json`      | 画质调整类                               |
| `BEAUTY_SHAPE`                          | 三个 `*_shape.json`        | 美型类                                 |
| `BODY_BEAUTY`                           | `beauty_body.json`       | 美体类                                 |
| `LUT`                                   | `lut.json`               | 滤镜类，无需配置 `effectName`，由面板自动填充       |
| `BEAUTY_MAKEUP`                         | `beauty_makeup.json` 根节点 | 美妆容器                                |
| `LIGHT_MAKEUP`                          | 美妆下的「轻美妆」                | 整套妆容，无需配置 `effectName`              |
| `MAKEUP`                                | 特效下的「整妆」                 | 整套妆容，无需配置 `effectName`              |
| `MOTION`                                | `motions.json` 根节点及贴纸项   | 贴纸动效，不支持进度条调节，`effectName` 自动填充     |
| `SEGMENTATION`                          | 特效下的「虚拟背景」               | 背景分割 / 绿幕，不支持进度条调节，`effectName` 自动填充 |
| `GREEN_BACKGROUND_V2_ITEM`              | 绿幕 V2 的子参数               | 相似度、平滑度等可调项                         |
| `GREEN_BACKGROUND_V2_ITEM_IMPORT_IMAGE` | 绿幕 V2 的「导入图片」            | 点击后拉起系统相册                           |
| `SHAPESET`                              | `shape.json`             | 配置分发器，自身不参与渲染                       |

> 若填写了上表以外的枚举值，将被视为未配置并继承父节点分类，可能导致渲染方式异常。

### 3.3.1 轻美妆单项编辑：itemDisplayMap

在「轻美妆」类目节点（`uiCategory` 为 `LIGHT_MAKEUP` 的容器）上可配置 `itemDisplayMap`，
用于开启「选中套装后二次点击下钻、逐项调节强度」的编辑能力：

```json
{
  "uiCategory": "LIGHT_MAKEUP",
  "itemDisplayMap": {
    "lut":                              { "name": { "zh": "滤镜", "en": "LUT" }, "icon": "beauty_panel/panel_icon/beauty/beauty_image_tint.png" },
    "beauty.faceFeatureLipsLut":        { "name": { "zh": "口红", "en": "Lipstick" }, "icon": "beauty_panel/panel_icon/beauty/beauty_lips.png" }
  },
  "propertyList": [ "...各套装..." ]
}
```

- key 为单项的 `effectName`，须与套装资源目录下 `makeup.json` 中的 `effectName` 一致；
- 某单项最终是否展示，取决于该套装的 `makeup.json` 中该项满足 **`alpha > 0` 且 `resourcePath` 非空**；
  因此不同套装的可编辑项数量可以不同，未配置的项会自动隐藏；
- 不配置 `itemDisplayMap` 时，轻美妆退化为仅可调节整套强度，不展示编辑角标。

> **强制性约束**：新增轻美妆套装时，其 `makeup.json` 的**数组顺序**必须与 `itemDisplayMap` 的
> **声明顺序**保持一致。Android 按 `itemDisplayMap` 的声明顺序渲染，iOS 按 `makeup.json` 的数组
> 顺序渲染，两者不一致会导致双端面板的单项排列出现差异。

### 3.4 uiState：设置功能项的初始生效状态

```json
{ "name": {"zh": "磨皮"}, "uiState": 1,          // 父节点须同步标记，否则一级面板不会高亮
  "propertyList": [
    { "name": {"zh": "幼龄"}, "uiState": 1,
      "sdkParam": { "effectName": "smooth.smooth", "effectValue": 40 } }
  ]
}
```

| 取值            | 含义                   |
| ------------- | -------------------- |
| `0`（未配置时的默认值） | 未启用                  |
| `1`           | 已生效（文案高亮，但非当前选中项）    |
| `2`           | 已生效且默认选中             |

说明：为叶子节点配置 `uiState` 时，其全部父节点须同步配置，否则上层面板不会呈现高亮状态。

---

## 四、路径配置规范

| 字段                        | 写法要求             | 示例                                                                          |
| ------------------------- | ---------------- | --------------------------------------------------------------------------- |
| `icon`                    | assets 相对路径      | `beauty_panel/panel_icon/beauty/none.png`                                   |
| `icon`                    | 本地绝对路径（以 `/` 开头） | `/sdcard/xxx/icon.png`                                                      |
| `icon`                    | 网络图片地址           | `https://xxx.com/icon.png`                                                  |
| `resourceUri`             | 本地内置素材的相对路径      | `MotionRes/2dMotionRes/video_keaituya`<br>`light_material/lut/baixi_lf.png` |
| `resourceUri`             | 网络素材地址           | `https://xxx.com/xxx.zip`，须配合 `downloadPath` 使用                             |
| `sdkParam.resourcePath`   | SDK 内置资源路径       | `/images/beauty/lips_fuguhong.png`                                          |
| `index.json` 的 `config[]` | 仅填写文件名           | `beauty.json`                                                               |

本地素材：`resourceUri` 填写的是相对于素材解压目录的路径，运行时将自动拼接完整前缀，因此不应写入绝对路径。

网络素材配置示例：

```json
{
  "name": { "zh": "动效" },
  "downloadPath": "MotionRes/motions/",        // ← 配置于根节点，全部子项共用
  "uiCategory": "MOTION",
  "propertyList": [
    { "name": { "zh": "新贴纸" },
      "icon": "https://xxx.com/icon.png",
      "resourceUri": "https://xxx.com/new_sticker.zip" }   // ← 自动下载并解压至 downloadPath
  ]
}
```

---

## 五、多语言

面板文案的语言选取遵循以下优先级：

```
代码中调用 TEUIConfig.getInstance().setDisPlayNameKey("en")   ← 优先生效
        ↓ 未设置时
index.json 中的 "defaultLanguage": "zh"
```

确定语言 key 后，面板将据此从各节点的 `name` 中取值。

> 若某节点的 `name` 缺少当前语言对应的 key，该功能项在面板中将不展示文案。新增语言时，须为全部文件的全部节点补齐 `name` 配置。当前所有文件均已提供 `zh` / `en` / `ja` 三种语言。

---

## 六、特殊文件 shape.json

该文件不包含任何功能项，其作用是依据代码传入的套餐，决定加载对应的美型配置：

```json
{
    "uiCategory": "SHAPESET",
    "dataList": [
        { "effectPlanSet": ["A1_01","A1_02","A1_05","A1_06"], "config": "beauty_base_shape.json" },
        { "effectPlanSet": ["A1_03","A1_04"],                 "config": "beauty_general_shape.json" },
        { "effectPlanSet": ["S1_00","S1_01","S1_02","S1_03","S1_04","S1_07"], "config": "beauty_shape.json" }
    ]
}
```

| 字段              | 说明                                 |
| --------------- | ---------------------------------- |
| `effectPlanSet` | 套餐名称列表，与代码中的 `EffectPlan.XXX` 一一对应 |
| `config`        | 命中该套餐时加载的配置文件                      |

匹配至首条符合条件的记录后即加载对应文件，`shape.json` 自身被替换。Demo 中传入的套餐为 `EffectPlan.S1_07`，因此实际加载 `beauty_shape.json`（包含全量 41 项美型）。

> 若当前套餐名称未出现在任何 `effectPlanSet` 中，美型部分将为空，即 Tab 内不展示任何美型功能项。切换套餐后美型消失时，请优先排查此处配置。

---

## 七、套餐能力范围与分类裁剪

接口第二个参数 `EffectPlan` 代表 License 授权的能力范围。面板会依据套餐自动隐藏当前不可用的功能分类，即使 JSON 中已完成配置也不会展示。

- 全部套餐均保留：模板、美颜、滤镜
- 其余分类按套餐裁剪，详见 [各套餐内容](https://cloud.tencent.com/document/product/616/67043)

---

## 八、常见配置场景

### 1. 移除某个功能项

在对应文件中删除该功能项的完整节点即可。例如需移除「美黑」，在 `beauty.json` 中删除 `"zh": "美黑"` 所在的整个节点。

### 2. 调整某项的默认强度

修改 `sdkParam.effectValue`。例如将磨皮「幼龄」的默认值由 40 调整为 60：

```json
{ "name": { "zh": "幼龄" }, "uiState": 1,
  "sdkParam": { "effectName": "smooth.smooth", "effectValue": 60 } }
```

### 3. 设置某项进入面板即生效

为该叶子节点及其全部父节点配置 `"uiState": 1`；如需同时默认选中，则配置为 `2`。

### 4. 新增滤镜

在 `lut.json` 的 `propertyList` 末尾追加节点（无需配置 `effectName`）：

```json
{
  "name": { "zh": "胶片", "en": "Film", "ja": "フィルム" },
  "icon": "beauty_panel/panel_icon/lut_icon/jiaopian.png",
  "resourceUri": "light_material/lut/jiaopian.png",
  "sdkParam": { "effectValue": 60 }
}
```

同时将图标文件放置于 `panel_icon/lut_icon/`。

### 5. 新增贴纸

在 `motions.json` 的「贴纸」分组中追加节点：

```json
{
  "name": { "zh": "新贴纸", "en": "New", "ja": "新規" },
  "icon": "beauty_panel/panel_icon/motions_icon/new.png",
  "resourceUri": "MotionRes/2dMotionRes/new_sticker"
}
```

### 6. 新增美颜项

当前全部美颜项已在 `beauty.json`、`beauty_image.json`、`beauty_base_shape.json`、`beauty_general_shape.json`、`beauty_shape.json` 中完成定义，面板将依据传入的套餐自动加载对应功能项，因此通常无需新增。

### 7. 将「美型」拆分为独立 Tab

修改 `index.json`：从「美颜」的 `config` 中移除 `shape.json`，并新增一个 Tab：

```json
{
    "name": { "zh": "美颜", "en": "Beauty", "ja": "美顔" },
    "config": [ "beauty.json", "beauty_image.json" ]
},
{
    "name": { "zh": "美型", "en": "Face Shape", "ja": "フェイスシェイプ" },
    "config": [ "shape.json" ]
}
```

---

## 九、问题排查

| 现象                  | 排查方向                                                                        |
| ------------------- | --------------------------------------------------------------------------- |
| Tab 未展示             | ① `index.json` 路径配置有误；② 该 Tab 的 `config` 文件名有误或 JSON 语法错误（解析失败的文件将被静默跳过）    |
| 某分类（美体／美妆／特效等）整体缺失  | 当前 `EffectPlan` 不支持该分类，详见第七节                                                |
| 美型功能项全部缺失           | `shape.json` 的 `effectPlanSet` 中未包含当前套餐名称，详见第六节                             |
| 功能项名称为空             | `name` 中缺少当前语言对应的 key，详见第五节                                                 |
| 图标未显示               | `icon` 路径配置有误，或图片未放置于 `panel_icon/` 目录                                      |
| 拼接后首个功能项缺失          | 该文件在 `config` 中非首位，且 `propertyList` 起始位置缺少占位项，详见第二节                         |
| 选中后效果未生效            | ① `resourceUri` / `resourcePath` 路径有误（素材类路径为空时会被自动过滤且不抛出异常）；② `effectName` 配置错误 |
| 进度条无法拖动             | 该功能项属于 `MOTION` / `SEGMENTATION` 分类，本身不支持强度调节                               |
| 面板加载缓慢              | 功能项数量过多，其中 `beauty_makeup.json`、`beauty_template.json` 体积较大，可按需精简           |

---

## 十、强制性约束

1. 入口文件必须命名为 `index.json`，面板数据目录必须命名为 `panel_json`，二者均不可重命名。
2. `index.json` 的 `config` 仅可填写文件名，填写路径将导致文件无法加载。
3. 子文件根节点的 `name` 会被 `index.json` 中的 `name` 覆盖，两处不一致时以 `index.json` 为准。
4. 模板文件中的 `id` 必须全局唯一，参数的保存与回显依赖该字段。
5. JSON 不支持注释，修改后须校验语法，语法错误将导致该文件整体失效。
6. 新增轻美妆套装时，`makeup.json` 的数组顺序须与轻美妆节点 `itemDisplayMap` 的声明顺序一致，详见 3.3.1。
