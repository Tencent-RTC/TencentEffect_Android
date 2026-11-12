# Beauty Panel JSON Configuration Guide

The JSON files in this directory describe how the beauty panel (`TEPanelView`) is presented, including the number and order of tabs, the feature items contained in each tab, and the icon, display name, default intensity and corresponding SDK effect parameters of every feature item.

Integration:

- Android: place this whole directory inside the `assets` folder.
- iOS: add this whole directory to any path of your main project.

All panel layout adjustments are done by editing the JSON configuration in this directory — no business code changes are required.

On the code side you only need to specify the entry file and the effect plan currently in use:

**Android**

```java
// Configure the index path and the effect plan
TEUIConfig.getInstance().setPanelViewResAndEffectPlanName("beauty_panel/index.json", EffectPlan.S1_07);
```

**iOS**

```objectivec
// Configure the index path and the effect plan
NSString *indexPath = [[NSBundle mainBundle] pathForResource:@"index" ofType:@"json"];
[[TEUIConfig shareInstance] setPanelViewRes:indexPath effectPlanName:TEEffectPlan_S1_07];
```

---

## 1. Directory Structure

The configuration consists of two kinds of files, each with its own responsibility:

```
beauty_panel/
├── index.json        ← defines which tabs the panel contains
├── panel_json/       ← defines the feature items inside each tab
│   ├── beauty_template.json
│   ├── beauty.json
│   ├── beauty_image.json
│   ├── shape.json  →  beauty_base_shape.json / beauty_general_shape.json / beauty_shape.json
│   ├── beauty_body.json
│   ├── lut.json
│   ├── beauty_makeup.json
│   └── motions.json
└── panel_icon/       ← all icon resources required by the panel
```

The mapping between the configuration and the panel elements is as follows:

```
┌──────────────────────────────────────────────────────────┐
│  Template  Beauty  Body  Filter  Makeup  Effect          │ ← the data array in index.json
├──────────────────────────────────────────────────────────┤
│   [○]     [○]    [○]   [○]     [○]     [○]              │ ← propertyList in panel_json/xxx.json
│   None   Whiten  Smooth Rosy   ...                       │
├──────────────────────────────────────────────────────────┤
│  ──────────●───────────────  40                          │ ← sdkParam.effectValue
└──────────────────────────────────────────────────────────┘
```

> Note: the following two names are hard-coded in the SDK and must not be changed. The entry file must be named `index.json`, and the panel data directory must be named `panel_json`.

---

## 2. index.json — Tab Definition

### Full Example

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

### Field Description

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `defaultLanguage` | String | Yes | Default language of the panel text. The value must be a key declared in the `name` objects below. Currently `zh` / `en` / `ja` are supported |
| `data` | Array | Yes | Tab list. The array order is the display order of the tabs from left to right |
| `data[].name` | Object | Yes | Multi-language title of the tab. This value overrides the `name` of the root node in the panel_json file |
| `data[].config` | String array | Yes | List of panel_json files that make up the tab. Only file names are allowed, no paths (the SDK always looks them up under the `panel_json/` directory) |

### Rules for Configuring Multiple Files in One Tab

Currently only the "Beauty" tab supports multiple files, i.e. the combination of `"beauty.json"`, `"beauty_image.json"` and `"shape.json"`.

Taking the "Beauty" tab as an example, it is configured with 3 files:

```json
"config": [ "beauty.json", "beauty_image.json", "shape.json" ]
```

With the configuration above, the feature items of the 3 files are concatenated in order and displayed together in the same tab:

```
Beauty Tab = None Whiten Tan Smooth AI Beauty Skin Skin Highlight Rosy   ← beauty.json
           + Contrast Saturation Clarity Sharpen Brightness ...   ← beauty_image.json
           + Big Eyes Slim Face Narrow Face Eye Distance ...      ← shape.json
```

The concatenation follows this rule:

> Starting from the 2nd file, the first element of its `propertyList` is discarded.

The reason behind this rule: the first item of every panel_json `propertyList` is a "None / Off" placeholder, and only one of them needs to be kept after concatenation. Therefore:

- when adding a file to `config`, keep a placeholder item at the beginning of its `propertyList`, otherwise its first real feature item will be dropped;
- if the file is used standalone and never concatenated, the placeholder is exactly the "None" button shown on the panel.

### Common Tab Modifications

| Requirement | How to do it |
| --- | --- |
| Reorder tabs | Change the order of the elements in the `data` array |
| Change a tab title | Change the value of the corresponding language in `data[].name` |
| Remove a tab | Delete the whole corresponding element from `data` |
| Add a tab | Add a new element to `data` and point its `config` to a newly created panel_json file |
| Merge two tabs | Keep one of the elements and append the other's file name to its `config` array. Currently only merging `"beauty.json"`, `"beauty_image.json"` and `"shape.json"` is supported |

---

## 3. panel_json/*.json — Feature Item Definition

### 3.1 Structure: Nested propertyList

Each file forms a configuration tree: the root node describes the whole tab, `propertyList` holds its children, and each child can declare the next level through its own `propertyList`.

```json
{
  "name": { "zh": "美颜", "en": "Beauty", "ja": "美顔" },   // root node: basic info of the tab
  "uiCategory": "BEAUTY",
  "uiState": 2,
  "propertyList": [

    { "name": { "zh": "关闭" },                              // ① placeholder: no sdkParam
      "icon": "beauty_panel/panel_icon/beauty/none.png" },

    { "name": { "zh": "美白" },                              // ② has children: tapping opens the next level
      "icon": "beauty_panel/panel_icon/beauty/beauty_whiten.png",
      "uiState": 2,
      "propertyList": [
        { "name": { "zh": "靓白" },                          //    └ leaf node: supports slider adjustment
          "icon": "beauty_panel/panel_icon/beauty/beauty_whiten_liangbai.png",
          "sdkParam": { "effectName": "beauty.lutFoundationAlpha0", "effectValue": 40 },
          "uiState": 2 },
        { "name": { "zh": "自然" },
          "icon": "beauty_panel/panel_icon/beauty/beauty_whiten_nature.png",
          "sdkParam": { "effectName": "beauty.lutFoundationAlpha", "effectValue": 0 } }
      ]
    },

    { "name": { "zh": "AI 美颜" },                           // ③ first-level leaf node: directly adjustable
      "icon": "beauty_panel/panel_icon/beauty/beauty_smooth_gan.png",
      "sdkParam": { "effectName": "beauty.skinRetouchAI", "effectValue": 40 } }
  ]
}
```

The node type is determined by whether `propertyList` and `sdkParam` are configured:

| `propertyList` | `sdkParam` | Node type and behavior |
| --- | --- | --- |
| Yes | No | Group node: tapping expands its children (e.g. the 4 effects under "Whiten") |
| No | Yes | Leaf node (adjustable): takes effect on tap and supports slider adjustment |
| No | No | Placeholder: corresponds to "None / Off" on the panel, tapping disables this kind of effect |

### 3.2 Field Quick Reference

| Field | Type | Description |
| --- | --- | --- |
| `name` | Object | Multi-language display name, e.g. `{"zh":"磨皮","en":"Smooth Skin","ja":"美肌"}`. It must contain the key of the active language, otherwise the item shows no text |
| `icon` | String | Icon path, see section 4 |
| `sdkParam` | Object | Effect parameters of the item, containing `effectName`, `effectValue`, `resourcePath`, etc. Only required for leaf nodes |
| `propertyList` | Array | Child item list |
| `uiCategory` | Enum | Feature category. It determines the rendering mode and the auto-fill logic of `effectName`, see 3.3. A child node inherits the parent value when not declared |
| `uiState` | int | Initial state of the item: `0` disabled / `1` enabled / `2` enabled and selected by default, see 3.4 |
| `resourceUri` | String | Material path (filter image, sticker package, makeup package, background image, etc.), see section 4 |
| `downloadPath` | String | Local directory where downloaded remote materials are stored. Configuring it on the root node is enough, child nodes inherit it |
| `hasSubTitle` | bool | When `true`, the first level of children is rendered as group titles and the panel shows an extra row of secondary tabs (already enabled in `motions.json` and `beauty_makeup.json`) |
| `verticalLayout` | bool | When `true`, children are arranged in a vertical grid; horizontal scrolling by default |
| `paramList` | Array | Template files only: one template maps to a group of sdkParam |
| `id` | int | Template files only: template number, must be globally unique |
| `dataList` | Array | `shape.json` only: distributes configuration by effect plan, see section 6 |

### 3.3 uiCategory Values

| Value | Applicable file | Description |
| --- | --- | --- |
| `BEAUTY_TEMPLATE` | `beauty_template.json` | One-tap beauty package, uses `paramList` instead of `sdkParam` |
| `BEAUTY` | `beauty.json` | Beauty items, support slider adjustment and parameter reset |
| `BEAUTY_IMAGE` | `beauty_image.json` | Image quality items |
| `BEAUTY_SHAPE` | the three `*_shape.json` | Face shape items |
| `BODY_BEAUTY` | `beauty_body.json` | Body beauty items |
| `LUT` | `lut.json` | Filters. `effectName` is not needed, the panel fills it automatically |
| `BEAUTY_MAKEUP` | root node of `beauty_makeup.json` | Makeup container |
| `LIGHT_MAKEUP` | "Light Makeup" under Makeup | Full makeup look, `effectName` not needed |
| `MAKEUP` | "Full Makeup" under Effect | Full makeup look, `effectName` not needed |
| `MOTION` | root node and sticker items of `motions.json` | Sticker animations. Slider adjustment is not supported, `effectName` is auto-filled |
| `SEGMENTATION` | "Virtual Background" under Effect | Background segmentation / green screen. Slider adjustment is not supported, `effectName` is auto-filled |
| `GREEN_BACKGROUND_V2_ITEM` | sub-parameters of green screen V2 | Adjustable items such as similarity and smoothness |
| `GREEN_BACKGROUND_V2_ITEM_IMPORT_IMAGE` | "Import Image" of green screen V2 | Opens the system album on tap |
| `SHAPESET` | `shape.json` | Configuration dispatcher, not rendered itself |

> If a value outside the table above is used, it is treated as not configured and the parent category is inherited, which may lead to unexpected rendering.

### 3.3.1 Light Makeup Per-Item Editing: itemDisplayMap

An `itemDisplayMap` can be configured on the "Light Makeup" category node (the container whose
`uiCategory` is `LIGHT_MAKEUP`) to enable per-item strength editing — tapping an already selected
package a second time drills down into its individual items:

```json
{
  "uiCategory": "LIGHT_MAKEUP",
  "itemDisplayMap": {
    "lut":                       { "name": { "zh": "滤镜", "en": "LUT" }, "icon": "beauty_panel/panel_icon/beauty/beauty_image_tint.png" },
    "beauty.faceFeatureLipsLut": { "name": { "zh": "口红", "en": "Lipstick" }, "icon": "beauty_panel/panel_icon/beauty/beauty_lips.png" }
  },
  "propertyList": [ "...packages..." ]
}
```

- The key is the `effectName` of the item and must match the `effectName` used in the `makeup.json`
  inside the package resource directory;
- Whether an item is actually displayed depends on that package's `makeup.json` satisfying
  **`alpha > 0` and a non-empty `resourcePath`**. Different packages may therefore expose different
  numbers of editable items; unconfigured items are hidden automatically;
- When `itemDisplayMap` is omitted, light makeup falls back to whole-package strength adjustment only
  and no edit badge is shown.

> **Mandatory constraint**: when adding a new light makeup package, the **array order** of its
> `makeup.json` must match the **declaration order** of `itemDisplayMap`. Android renders items in
> `itemDisplayMap` declaration order while iOS renders them in `makeup.json` array order, so a
> mismatch causes the two platforms to display items in different orders.

### 3.4 uiState: Initial Effective State of a Feature Item

```json
{ "name": {"zh": "磨皮"}, "uiState": 1,          // the parent node must be marked as well, otherwise the first-level panel is not highlighted
  "propertyList": [
    { "name": {"zh": "幼龄"}, "uiState": 1,
      "sdkParam": { "effectName": "smooth.smooth", "effectValue": 40 } }
  ]
}
```

| Value | Meaning |
| --- | --- |
| `0` (default when not configured) | Disabled |
| `1` | Enabled (text highlighted, but not the currently selected item) |
| `2` | Enabled and selected by default |

Note: when `uiState` is configured on a leaf node, all of its parent nodes must be configured accordingly, otherwise the upper-level panel will not show the highlighted state.

---

## 4. Path Configuration Rules

| Field | Format requirement | Example |
| --- | --- | --- |
| `icon` | assets-relative path | `beauty_panel/panel_icon/beauty/none.png` |
| `icon` | local absolute path (starting with `/`) | `/sdcard/xxx/icon.png` |
| `icon` | remote image URL | `https://xxx.com/icon.png` |
| `resourceUri` | relative path of a local built-in material | `MotionRes/2dMotionRes/video_keaituya`<br>`light_material/lut/baixi_lf.png` |
| `resourceUri` | remote material URL | `https://xxx.com/xxx.zip`, must be used together with `downloadPath` |
| `sdkParam.resourcePath` | SDK built-in resource path | `/images/beauty/lips_fuguhong.png` |
| `config[]` of `index.json` | file name only | `beauty.json` |

Local materials: `resourceUri` is a path relative to the material extraction directory. The full prefix is appended automatically at runtime, so absolute paths must not be used.

Example of remote material configuration:

```json
{
  "name": { "zh": "动效" },
  "downloadPath": "MotionRes/motions/",        // ← configured on the root node, shared by all children
  "uiCategory": "MOTION",
  "propertyList": [
    { "name": { "zh": "新贴纸" },
      "icon": "https://xxx.com/icon.png",
      "resourceUri": "https://xxx.com/new_sticker.zip" }   // ← downloaded and extracted to downloadPath automatically
  ]
}
```

---

## 5. Multi-language

The language of the panel text is chosen with the following priority:

```
TEUIConfig.getInstance().setDisPlayNameKey("en") called in code   ← takes precedence
        ↓ when not set
"defaultLanguage": "zh" in index.json
```

Once the language key is determined, the panel reads the value from the `name` object of each node.

> If the `name` of a node lacks the key of the current language, that feature item shows no text on the panel. When adding a new language, the `name` configuration must be completed for every node in every file. Currently all files provide the three languages `zh` / `en` / `ja`.

---

## 6. The Special File shape.json

This file contains no feature items. Its purpose is to decide which face shape configuration to load according to the effect plan passed in from code:

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

| Field | Description |
| --- | --- |
| `effectPlanSet` | List of effect plan names, matching `EffectPlan.XXX` in code one by one |
| `config` | The configuration file to load when the plan matches |

The first matching record wins and its file is loaded, replacing `shape.json` itself. The demo passes `EffectPlan.S1_07`, so `beauty_shape.json` (containing all 41 face shape items) is actually loaded.

> If the current plan name does not appear in any `effectPlanSet`, the face shape part will be empty, i.e. no face shape items are shown in the tab. When face shape items disappear after switching plans, check this configuration first.

---

## 7. Plan Capability Scope and Category Trimming

The second parameter `EffectPlan` represents the capability scope authorized by the license. The panel automatically hides categories that are unavailable for the current plan, even if they are fully configured in JSON.

- Always kept for all plans: Template, Beauty, Filter
- Other categories are trimmed by plan, see [Plan Contents](https://cloud.tencent.com/document/product/616/67043)

---

## 8. Common Configuration Scenarios

### 1. Remove a feature item

Delete the whole node of that feature item from the corresponding file. For example, to remove "Tan", delete the entire node containing `"zh": "美黑"` in `beauty.json`.

### 2. Change the default intensity of an item

Modify `sdkParam.effectValue`. For example, to change the default value of the smoothing item "幼龄" from 40 to 60:

```json
{ "name": { "zh": "幼龄" }, "uiState": 1,
  "sdkParam": { "effectName": "smooth.smooth", "effectValue": 60 } }
```

### 3. Make an item effective as soon as the panel opens

Set `"uiState": 1` on the leaf node and all of its parent nodes; use `2` if it should also be selected by default.

### 4. Add a filter

Append a node to the end of the `propertyList` in `lut.json` (`effectName` is not needed):

```json
{
  "name": { "zh": "胶片", "en": "Film", "ja": "フィルム" },
  "icon": "beauty_panel/panel_icon/lut_icon/jiaopian.png",
  "resourceUri": "light_material/lut/jiaopian.png",
  "sdkParam": { "effectValue": 60 }
}
```

Also place the icon file under `panel_icon/lut_icon/`.

### 5. Add a sticker

Append a node to the "Sticker" group in `motions.json`:

```json
{
  "name": { "zh": "新贴纸", "en": "New", "ja": "新規" },
  "icon": "beauty_panel/panel_icon/motions_icon/new.png",
  "resourceUri": "MotionRes/2dMotionRes/new_sticker"
}
```

### 6. Add a beauty item

All beauty items are already defined in `beauty.json`, `beauty_image.json`, `beauty_base_shape.json`, `beauty_general_shape.json` and `beauty_shape.json`. The panel loads the corresponding items automatically according to the effect plan passed in, so adding new ones is usually unnecessary.

### 7. Split "Face Shape" into a standalone tab

Edit `index.json`: remove `shape.json` from the `config` of "Beauty" and add a new tab:

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

## 9. Troubleshooting

| Symptom | What to check |
| --- | --- |
| A tab is not displayed | ① the path configured in `index.json` is wrong; ② the file name in the tab's `config` is wrong or the JSON has a syntax error (files that fail to parse are skipped silently) |
| A whole category (Body / Makeup / Effect, etc.) is missing | The current `EffectPlan` does not support that category, see section 7 |
| All face shape items are missing | The current plan name is not included in the `effectPlanSet` of `shape.json`, see section 6 |
| A feature item has an empty name | The `name` lacks the key of the current language, see section 5 |
| An icon is not displayed | The `icon` path is wrong, or the image is not placed under `panel_icon/` |
| The first feature item is missing after concatenation | The file is not the first one in `config` and its `propertyList` lacks a placeholder at the beginning, see section 2 |
| No effect after selecting an item | ① the `resourceUri` / `resourcePath` path is wrong (empty material paths are filtered out silently without throwing); ② `effectName` is misconfigured |
| The slider cannot be dragged | The item belongs to the `MOTION` / `SEGMENTATION` category, which does not support intensity adjustment |
| The panel loads slowly | Too many feature items. `beauty_makeup.json` and `beauty_template.json` are relatively large and can be trimmed as needed |

---

## 10. Mandatory Constraints

1. The entry file must be named `index.json` and the panel data directory must be named `panel_json`; neither can be renamed.
2. The `config` of `index.json` may only contain file names; providing a path prevents the file from being loaded.
3. The `name` of a child file's root node is overridden by the `name` in `index.json`; when they differ, `index.json` wins.
4. The `id` in template files must be globally unique — saving and restoring parameters relies on this field.
5. JSON does not support comments. Validate the syntax after editing, since a syntax error invalidates the whole file.
6. When adding a light makeup package, the array order of its `makeup.json` must match the declaration order of the light makeup node's `itemDisplayMap`. See 3.3.1.
