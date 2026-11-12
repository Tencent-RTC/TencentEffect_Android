package com.tencent.effect.beautykit.provider;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.tencent.effect.beautykit.config.EffectPlan;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.model.TEIndexFileModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.xmagic.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于加载并解析美颜面板的 index.json 配置文件。
 * <p>
 * index.json 结构参考 {@link TEIndexFileModel}。
 */
public class IndexFileProvider {

    private static final String TAG = "IndexFileLoader";

    /**
     * index.json 所在目录下用于存放子面板 json 的相对目录名。
     */
    private static final String PANEL_JSON_DIR = "panel_json/";

    /**
     * index.json 文件名，用于从完整路径推导父目录。
     */
    private static final String INDEX_JSON_FILE_NAME = "index.json";

    /**
     * Gson 是线程安全的，复用同一个实例即可，避免每次解析都 new。
     */
    private static final Gson GSON = new Gson();

    /**
     * 从文件（绝对路径）或 asset 加载 index.json 并解析为 {@link TEIndexFileModel}。
     *
     * @param context      Android 上下文，读取 asset 时必需
     * @param jsonFilePath 以 {@link File#separator} 开头视为绝对路径，否则视为 asset 相对路径
     * @return 解析成功返回模型对象，否则返回 {@code null}
     */
    public TEIndexFileModel loadIndexJson(Context context, String jsonFilePath) {
        if (context == null || TextUtils.isEmpty(jsonFilePath)) {
            Log.e(TAG, "loadIndexJson: invalid params, context or jsonFilePath is empty.");
            return null;
        }
        String dataStr = readFileContent(context, jsonFilePath);
        return parse(dataStr);
    }

    /**
     * 直接解析给定的 json 字符串。
     *
     * @param jsonContent json 字符串
     * @return 解析后的 {@link TEIndexFileModel}，失败返回 {@code null}
     */
    public TEIndexFileModel parse(String jsonContent) {
        if (TextUtils.isEmpty(jsonContent)) {
            Log.e(TAG, "parse: json content is empty.");
            return null;
        }
        try {
            return GSON.fromJson(jsonContent.trim(), TEIndexFileModel.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "parse: json syntax error, " + e.getMessage());
            return null;
        }
    }

    /**
     * 按 {@link TEIndexFileModel.Item#config} 加载并合并出一个可展示的 {@link TEUIProperty}。
     * <p>
     * 主要步骤：
     * <ol>
     *   <li>依次读取 item.config 中每个 json 文件；</li>
     *   <li>若为 {@link TEUIProperty.UICategory#SHAPESET}，则根据当前套餐 {@link EffectPlan} 展开
     *       {@link TEUIProperty#dataList} 中匹配的子配置；</li>
     *   <li>将上述结果按顺序合并（多余的标题项会被去掉，A1_00 / BEAUTY_IMAGE 不合并）；</li>
     *   <li>最后交给 {@link EffectPlan#filter(TEUIProperty)} 做套餐相关过滤。</li>
     * </ol>
     *
     * @param item    面板项配置
     * @param context Android 上下文
     * @return 合并 + 过滤后的属性；若无有效数据返回 {@code null}
     */
    public TEUIProperty loadUIPropertyFromItem(TEIndexFileModel.Item item, Context context) {
        if (item == null || item.config == null || item.config.isEmpty()) {
            Log.e(TAG, "loadUIPropertyFromItem: item or item.config is empty.");
            return null;
        }
        String parentPath = getPanelJsonParentPath();
        List<TEUIProperty> resultList = new ArrayList<>();

        for (String jsonPath : item.config) {
            TEUIProperty property = readAndParseUIProperty(context, parentPath, jsonPath);
            if (property == null) {
                continue;
            }
            if (property.uiCategory == TEUIProperty.UICategory.SHAPESET) {
                property = this.expandShapeSet(context, parentPath, property);
                if (property == null) {
                    continue;
                }
            }
            property.name = item.name;
            resultList.add(property);
        }

        if (resultList.isEmpty()) {
            return null;
        }
        TEUIProperty merged = mergeUIProperties(resultList);
        return TEUIConfig.getInstance().getEffectPlan().filter(merged);
    }

    // =========================================================================
    // 私有辅助方法
    // =========================================================================

    /**
     * 根据路径类型（绝对路径 / asset 路径）读取文件内容。
     */
    private String readFileContent(Context context, String jsonFilePath) {
        if (TextUtils.isEmpty(jsonFilePath)) {
            return null;
        }
        if (jsonFilePath.startsWith(File.separator)) {
            return FileUtil.readFile(jsonFilePath);
        }
        return FileUtil.readAssetFile(context, jsonFilePath);
    }

    /**
     * 得到 panel_json 所在的父目录（含结尾 "/"）。
     * 例如 index.json 路径为 {@code xxx/beauty_panel/index.json}，
     * 返回 {@code xxx/beauty_panel/panel_json/}。
     */
    private String getPanelJsonParentPath() {
        String indexJsonPath = TEUIConfig.getInstance().panelViewIndexJsonFilePath;
        String parentDir = indexJsonPath == null ? "" : indexJsonPath.replace(INDEX_JSON_FILE_NAME, "");
        return parentDir + PANEL_JSON_DIR;
    }

    /**
     * 读取并解析单个 UI 属性 json。
     *
     * @param parentPath panel_json 父目录（含结尾 "/"）
     * @param jsonPath   相对文件名
     * @return 解析成功的 {@link TEUIProperty}；文件为空或 json 非法时返回 {@code null}
     */
    private TEUIProperty readAndParseUIProperty(Context context, String parentPath, String jsonPath) {
        if (TextUtils.isEmpty(jsonPath)) {
            return null;
        }
        String dataStr = readFileContent(context, parentPath + jsonPath.trim());
        if (TextUtils.isEmpty(dataStr)) {
            return null;
        }
        try {
            return GSON.fromJson(dataStr.trim(), TEUIProperty.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "readAndParseUIProperty: json syntax error, path=" + jsonPath + ", msg=" + e.getMessage());
            return null;
        }
    }

    /**
     * 展开 SHAPESET：从 {@link TEUIProperty#dataList} 中挑选与当前套餐匹配的项，
     * 逐个读取对应的 json 并追加到 {@code resultList}。
     * <p>
     * 与旧逻辑相比：任何一项读取失败不再中断，改为继续尝试后续项，避免因单个文件缺失丢掉整组数据。
     */
    private TEUIProperty expandShapeSet(Context context, String parentPath, TEUIProperty shapeSetProperty) {
        if (shapeSetProperty.dataList == null || shapeSetProperty.dataList.isEmpty()) {
            return null;
        }
        String currentPlanName = TEUIConfig.getInstance().getEffectPlan().toString();
        for (TEIndexFileModel.DataSetItem dataSetItem : shapeSetProperty.dataList) {
            if (dataSetItem == null || dataSetItem.effectPlanSet == null ||
                    TextUtils.isEmpty(dataSetItem.config)) {
                continue;
            }
            if (!dataSetItem.effectPlanSet.contains(currentPlanName)) {
                continue;
            }
            TEUIProperty property = readAndParseUIProperty(context, parentPath, dataSetItem.config);
            if (property != null) {
                return property;
            }
        }
        return null;
    }

    /**
     * 将多个 UI 属性合并到第一个上返回。
     * <p>
     * 合并规则（保持与原实现语义一致）：
     * <ul>
     *   <li>以第一个属性作为基础容器；</li>
     *   <li>若当前套餐为 {@link EffectPlan#A1_00}，或子项 uiCategory 为
     *       {@link TEUIProperty.UICategory#BEAUTY_IMAGE}，则跳过（A1_00 无画质调整）；</li>
     *   <li>被合并项 propertyList 的第 0 项（一般是标题占位）会被移除后再追加。</li>
     * </ul>
     */
    private TEUIProperty mergeUIProperties(List<TEUIProperty> resultList) {
        if (resultList.size() == 1) {
            return resultList.get(0);
        }
        TEUIProperty base = resultList.get(0);
        EffectPlan currentPlan = TEUIConfig.getInstance().getEffectPlan();
        for (int i = 1; i < resultList.size(); i++) {
            TEUIProperty item = resultList.get(i);
            if (item == null) {
                continue;
            }
            // A1_00 套餐没有画质调整，其他套餐都有
            if (currentPlan == EffectPlan.A1_00 && item.uiCategory == TEUIProperty.UICategory.BEAUTY_IMAGE) {
                continue;
            }
            if (item.propertyList == null || item.propertyList.isEmpty()) {
                continue;
            }
            if (base.propertyList == null) {
                base.propertyList = new ArrayList<>();
            }
            // 去掉子面板自带的标题占位项后再合并
            item.propertyList.remove(0);
            base.propertyList.addAll(item.propertyList);
        }
        return base;
    }
}