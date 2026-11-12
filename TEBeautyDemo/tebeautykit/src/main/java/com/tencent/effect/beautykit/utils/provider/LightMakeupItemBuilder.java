package com.tencent.effect.beautykit.utils.provider;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.LogUtils;
import com.tencent.xmagic.XmagicConstant;
import com.tencent.xmagic.util.FileUtil;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 轻美妆下钻态单项列表构造器。
 * <p>
 * 在用户二次点击某个已选中的轻美妆套装时，根据：
 * <ol>
 *   <li>「轻美妆」节点的 {@code itemDisplayMap}（用于单项的多语言名称、图标）</li>
 *   <li>该套装目录下的 {@code makeup.json}（用于确定实际配置了哪些单项）</li>
 * </ol>
 * 构造出一组 {@link TEUIProperty#uiCategory} 为 {@link TEUIProperty.UICategory#LIGHT_MAKEUP_ITEM} 的子项。
 * <p>
 * 排序规则：按 itemDisplayMap 的声明顺序排列（保留 Gson 对 LinkedHashMap 的插入顺序），
 * makeup.json 中出现但 itemDisplayMap 未配置的 effectName 一律被忽略（避免未配置的项以无意义文案出现在 UI 上）。
 * 此外，makeup.json 中 {@code alpha<=0} 或 {@code resourcePath} 为空的占位项表示该套装未包含此单项，同样不展示。
 */
public class LightMakeupItemBuilder {

    private static final String TAG = "LightMakeupItemBuilder";

    private static final String MAKEUP_JSON_FILE_NAME = "makeup.json";

    /**
     * 为给定的轻美妆套装 property 构造下钻态的单项列表。
     *
     * @param lightMakeupProperty 已选中的轻美妆套装 property（父 property）
     * @return 单项列表（按 itemDisplayMap 声明顺序排列），如果无法生成则返回 null
     */
    public static List<TEUIProperty> buildItemList(TEUIProperty lightMakeupProperty) {
        if (lightMakeupProperty == null) {
            LogUtils.w(TAG, "buildItemList: lightMakeupProperty is null");
            return null;
        }
        if (lightMakeupProperty.sdkParam == null) {
            LogUtils.w(TAG, "buildItemList: sdkParam is null");
            return null;
        }
        TEUIProperty rootProperty = findRootLightMakeupProperty(lightMakeupProperty);
        if (rootProperty == null || rootProperty.itemDisplayMap == null
                || rootProperty.itemDisplayMap.isEmpty()) {
            LogUtils.w(TAG, "buildItemList: itemDisplayMap is missing");
            return null;
        }
        String resourcePath = lightMakeupProperty.sdkParam.resourcePath;
        if (TextUtils.isEmpty(resourcePath)) {
            LogUtils.w(TAG, "buildItemList: resourcePath is empty, cannot locate makeup.json");
            return null;
        }
        List<String> configuredEffectNames = parseMakeupJson(resourcePath);
        if (configuredEffectNames == null || configuredEffectNames.isEmpty()) {
            LogUtils.w(TAG, "buildItemList: makeup.json is empty or not found under " + resourcePath);
            return null;
        }

        List<TEUIProperty> items = new ArrayList<>();
        // 按 itemDisplayMap 声明顺序遍历，取 makeup.json 中实际配置（alpha>0 且 resourcePath 非空）的项
        for (Map.Entry<String, TEUIProperty.LightMakeupItemDisplay> entry
                : rootProperty.itemDisplayMap.entrySet()) {
            String effectName = entry.getKey();
            if (!configuredEffectNames.contains(effectName)) {
                continue;
            }
            TEUIProperty.LightMakeupItemDisplay display = entry.getValue();
            if (display == null) {
                continue;
            }
            items.add(createLightMakeupItem(lightMakeupProperty, effectName, display));
        }
        return items;
    }

    /**
     * 构造一个单项（口红/腮红/眼影/滤镜等）。
     */
    private static TEUIProperty createLightMakeupItem(TEUIProperty parent, String effectName,
                                                      TEUIProperty.LightMakeupItemDisplay display) {
        TEUIProperty item = new TEUIProperty();
        item.uiCategory = TEUIProperty.UICategory.LIGHT_MAKEUP_ITEM;
        item.name = display.name;
        item.icon = display.icon;
        item.parentUIProperty = parent;
        // sdkParam.effectName 用作 extraInfo 读写时的 key，不下发给 SDK；
        // effectValue 作为父套装 extraInfo 强度的快照缓存，供通用滑竿逻辑直接读取。
        TEUIProperty.TESDKParam param = new TEUIProperty.TESDKParam();
        param.effectName = effectName;
        param.effectValue = resolveItemStrength(parent, effectName);
        item.sdkParam = param;
        return item;
    }

    /**
     * 轻美妆单项强度读取范式（供下钻构建时写入 item 的 effectValue 快照）：
     * <ul>
     *     <li>lut 项：extraInfo["lut"] &gt; extraInfo["makeupLutStrength"]（老字段） &gt; 0</li>
     *     <li>非 lut 项：extraInfo[effectName] &gt; parent.effectValue（整体强度）</li>
     * </ul>
     * 保留对老字段 makeupLutStrength 的回退，是为了兼容外部仅保存老字段并回灌 UI 的场景。
     */
    private static int resolveItemStrength(TEUIProperty parent, String effectName) {
        if (parent == null || parent.sdkParam == null) {
            return 0;
        }
        Map<String, String> extraInfo = parent.sdkParam.extraInfo;
        if (extraInfo != null) {
            String override = extraInfo.get(effectName);
            if (!TextUtils.isEmpty(override)) {
                Integer v = parseIntSafely(override);
                if (v != null) {
                    return clampStrength(v);
                }
            }
            if (XmagicConstant.EffectName.EFFECT_LUT.equals(effectName)) {
                String oldLut = extraInfo.get(TEUIProperty.TESDKParam.EXTRA_INFO_KEY_LUT_STRENGTH);
                if (!TextUtils.isEmpty(oldLut)) {
                    Integer v = parseIntSafely(oldLut);
                    if (v != null) {
                        return clampStrength(v);
                    }
                }
                return 0;
            }
        }
        if (XmagicConstant.EffectName.EFFECT_LUT.equals(effectName)) {
            return 0;
        }
        return clampStrength(parent.sdkParam.effectValue);
    }

    private static Integer parseIntSafely(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static int clampStrength(int v) {
        if (v < 0) return 0;
        if (v > 100) return 100;
        return v;
    }

    /**
     * 读取某套轻美妆目录下的 makeup.json，提取出所有"已配置"的 effectName。
     * <p>
     * 一项被判定为"已配置"当且仅当：{@code alpha > 0} 且 {@code resourcePath} 非空。
     * 其余项（如 alpha=0 或 resourcePath 为空的占位项）表示套装未包含该单项，UI 不下发、不展示。
     *
     * @param resourcePath 该套妆的本地资源目录（相对 TEBeautyKit.getResPath() 或绝对路径）
     * @return 该套妆实际已配置的 effectName 列表；读取失败返回 null
     */
    private static List<String> parseMakeupJson(String resourcePath) {
        File jsonFile = new File(resolveMakeupJsonPath(resourcePath));
        if (!jsonFile.exists()) {
            LogUtils.w(TAG, "makeup.json not found at " + jsonFile.getAbsolutePath());
            return null;
        }
        String jsonStr = FileUtil.readFile(jsonFile.getAbsolutePath());
        if (TextUtils.isEmpty(jsonStr)) {
            return null;
        }
        try {
            Type listType = new TypeToken<List<MakeupItem>>() {
            }.getType();
            List<MakeupItem> list = new Gson().fromJson(jsonStr, listType);
            if (list == null) {
                return null;
            }
            List<String> result = new ArrayList<>();
            for (MakeupItem item : list) {
                if (item == null || TextUtils.isEmpty(item.effectName)) {
                    continue;
                }
                // 未配置的项（alpha<=0 或 无资源）不展示
                if (item.alpha <= 0f || TextUtils.isEmpty(item.resourcePath)) {
                    continue;
                }
                result.add(item.effectName);
            }
            return result;
        } catch (Exception e) {
            LogUtils.e(TAG, "parseMakeupJson failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * 将套装的 resourcePath 解析成 makeup.json 的绝对路径。
     * <p>
     * resourcePath 可能为相对路径（如 "MotionRes/light_makeup/light_baixi"，此时需拼接
     * {@link TEBeautyKit#getResPath()}），也可能是绝对路径（http 下载场景）。
     */
    private static String resolveMakeupJsonPath(String resourcePath) {
        if (resourcePath.startsWith(File.separator)) {
            return resourcePath + File.separator + MAKEUP_JSON_FILE_NAME;
        }
        String resPath = TEBeautyKit.getResPath();
        String base = TextUtils.isEmpty(resPath) ? resourcePath : resPath + resourcePath;
        return base + File.separator + MAKEUP_JSON_FILE_NAME;
    }

    /**
     * 向上查找持有 itemDisplayMap 的节点（通常是「轻美妆」二级标题节点）。
     */
    private static TEUIProperty findRootLightMakeupProperty(TEUIProperty property) {
        TEUIProperty p = property;
        while (p != null) {
            if (p.itemDisplayMap != null && !p.itemDisplayMap.isEmpty()) {
                return p;
            }
            p = p.parentUIProperty;
        }
        return null;
    }

    /**
     * makeup.json 的元素结构，仅用于本工具类反序列化。
     */
    private static class MakeupItem {
        String effectName;
        float alpha;
        String resourcePath;
    }
}
