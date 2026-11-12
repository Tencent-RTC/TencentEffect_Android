package com.tencent.effect.beautykit.utils.provider;

import static com.tencent.effect.beautykit.model.TEUIProperty.GreenBackgroundItemName.BACKGROUND_V2_CORROSION;
import static com.tencent.effect.beautykit.model.TEUIProperty.GreenBackgroundItemName.BACKGROUND_V2_DE_SHADOW;
import static com.tencent.effect.beautykit.model.TEUIProperty.GreenBackgroundItemName.BACKGROUND_V2_DE_SPILL;
import static com.tencent.effect.beautykit.model.TEUIProperty.GreenBackgroundItemName.BACKGROUND_V2_SIMILARITY;
import static com.tencent.effect.beautykit.model.TEUIProperty.GreenBackgroundItemName.BACKGROUND_V2_SMOOTH;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.download.TEDownloadRegistry;
import com.tencent.effect.beautykit.download.TEDownloadUrlHelper;
import com.tencent.xmagic.XmagicConstant;
import com.tencent.effect.beautykit.model.TEMotionDLModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.FileUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ProviderUtils {

    private static final String TAG = ProviderUtils.class.getName();

    // This is used to store beauty properties that cannot be applied simultaneously, such as having three different whitening effects in the "whitening" category. These three effects cannot be applied simultaneously, so the UIState on the UI needs special handling.
    private static final String[] BEAUTY_WHITEN_EFFECT_NAMES = {
            XmagicConstant.EffectName.BEAUTY_WHITEN_0,
            XmagicConstant.EffectName.BEAUTY_WHITEN,
            XmagicConstant.EffectName.BEAUTY_WHITEN_2,
            XmagicConstant.EffectName.BEAUTY_WHITEN_3,

    };
    private static final String[] BEAUTY_BLACK_EFFECT_NAMES = {
            XmagicConstant.EffectName.BEAUTY_BLACK_1,
            XmagicConstant.EffectName.BEAUTY_BLACK_2,
    };
    private static final String[] BEAUTY_FACE_EFFECT_NAMES = {
            XmagicConstant.EffectName.BEAUTY_FACE_NATURE,
            XmagicConstant.EffectName.BEAUTY_FACE_GODNESS,
            XmagicConstant.EffectName.BEAUTY_FACE_MALE_GOD,

    };

    private static final String[] BEAUTY_SMOOTH_NAMES = {
            XmagicConstant.EffectName.BEAUTY_SMOOTH,
            XmagicConstant.EffectName.BEAUTY_SMOOTH2,
            XmagicConstant.EffectName.BEAUTY_SMOOTH3,
            XmagicConstant.EffectName.BEAUTY_SMOOTH4,
            XmagicConstant.EffectName.BEAUTY_SMOOTH5,

    };


    public static final String HTTP_NAME = "http";
    public static final String ZIP_NAME = ".zip";


    private static boolean contains(String[] names, String effectName) {
        for (String name : names) {
            if (name.equals(effectName)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Find the data corresponding to the given category in allData.
     *
     * @param allData    The complete set of data
     * @param uiCategory The category to search for
     * @return The data corresponding to the given category
     */
    public static TEUIProperty getUIPropertyByCategory(List<TEUIProperty> allData, TEUIProperty.UICategory uiCategory) {
        for (TEUIProperty uiProperty : allData) {
            if (uiCategory == uiProperty.uiCategory) {
                return uiProperty;
            }
        }
        return null;
    }


    public static void changeParamUIState(TEUIProperty teuiProperty, int uiState) {
        if (teuiProperty == null) {
            return;
        }
        teuiProperty.setUiState(uiState);
        ProviderUtils.changeParamUIState(teuiProperty.parentUIProperty, uiState);
    }


    public static void revertUIState(List<TEUIProperty> uiPropertyList, TEUIProperty currentItem) {
        if (uiPropertyList == null) {
            return;
        }
        for (TEUIProperty property : uiPropertyList) {
            if (property == null) {
                continue;
            }
            ProviderUtils.revertUIState(property.propertyList, currentItem);

            if (property.getUiState() == TEUIProperty.UIState.INIT) {
                continue;
            }
            if (property.uiCategory == TEUIProperty.UICategory.BEAUTY || property.uiCategory == TEUIProperty.UICategory.BODY_BEAUTY) {
                if (isSameEffectName(property, currentItem)) {
                    changeParamUIState(property, TEUIProperty.UIState.INIT);
                } else {
                    changeParamUIState(property, TEUIProperty.UIState.IN_USE);
                }
            } else {
                // 轻美妆与单点美妆/滤镜的互斥由上层 onClickPointMakeup / onClickLightMakeup 按 cleanLightMakeup 控制，
                // 这里只负责"同为轻美妆"套装间的单选；跨类（如点击单点美妆）时跳过，避免误清轻美妆。
                if (isLightMakeupCategory(property) && !isLightMakeupCategory(currentItem)) {
                    continue;
                }
                changeParamUIState(property, TEUIProperty.UIState.INIT);
            }
        }
    }


    /**
     * 将item的状态强制设置为 init
     *
     * @param uiPropertyList
     */
    public static void revertUIStateToInit(List<TEUIProperty> uiPropertyList) {
        if (uiPropertyList == null) {
            return;
        }
        for (TEUIProperty property : uiPropertyList) {
            if (property == null) {
                continue;
            }
            ProviderUtils.revertUIStateToInit(property.propertyList);
            if (property.getUiState() == TEUIProperty.UIState.INIT) {
                continue;
            }
            changeParamUIState(property, TEUIProperty.UIState.INIT);
        }
    }


    private static boolean isSameEffectName(TEUIProperty property, TEUIProperty property2) {
        if (property == null || property2 == null) {
            return false;
        }
        if (property.sdkParam == null || property2.sdkParam == null) {
            return false;
        }
        if (property.sdkParam.effectName.equals(property2.sdkParam.effectName)) {
            return true;
        }
        if (ProviderUtils.contains(BEAUTY_WHITEN_EFFECT_NAMES, property.sdkParam.effectName) && ProviderUtils.contains(BEAUTY_WHITEN_EFFECT_NAMES, property2.sdkParam.effectName)) {
            return true;
        }
        if (ProviderUtils.contains(BEAUTY_BLACK_EFFECT_NAMES, property.sdkParam.effectName) && ProviderUtils.contains(BEAUTY_BLACK_EFFECT_NAMES, property2.sdkParam.effectName)) {
            return true;
        }
        if (ProviderUtils.contains(BEAUTY_FACE_EFFECT_NAMES, property.sdkParam.effectName) && ProviderUtils.contains(BEAUTY_FACE_EFFECT_NAMES, property2.sdkParam.effectName)) {
            return true;
        }
        if (ProviderUtils.contains(BEAUTY_SMOOTH_NAMES, property.sdkParam.effectName) && ProviderUtils.contains(BEAUTY_SMOOTH_NAMES, property2.sdkParam.effectName)) {
            return true;
        }
        return false;
    }


    /**
     * 判断是否为轻美妆相关分类（轻美妆套装 LIGHT_MAKEUP 或下钻态单项 LIGHT_MAKEUP_ITEM）。
     *
     * @param property UI 属性对象，允许为 null
     * @return 若为轻美妆相关分类则返回 true，否则返回 false
     */
    private static boolean isLightMakeupCategory(TEUIProperty property) {
        return property != null && (property.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP
                || property.uiCategory == TEUIProperty.UICategory.LIGHT_MAKEUP_ITEM);
    }


    /**
     * Create a clone of teParam based on the usage and set the effectValue to 0.
     *
     * @param usedList The list of teParam currently in effect
     * @return A new clone of teParam with effectValue set to 0
     **/
    public static List<TEUIProperty.TESDKParam> clone0ValuedParam(List<TEUIProperty.TESDKParam> usedList) {
        if (usedList == null) {
            return null;
        }
        List<TEUIProperty.TESDKParam> resultList = new ArrayList<>();
        for (TEUIProperty.TESDKParam param : usedList) {
            try {
                TEUIProperty.TESDKParam cloneParam = param.clone();
                cloneParam.effectValue = 0;
                resultList.add(cloneParam);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return resultList;
    }


    public static void changParamValuedTo0(List<TEUIProperty.TESDKParam> usedList) {
        if (usedList == null) {
            return;
        }
        for (TEUIProperty.TESDKParam param : usedList) {
            param.effectValue = 0;
        }
    }


    public static List<TEUIProperty.TESDKParam> getUsedProperties(List<TEUIProperty> uiProperties) {
        List<TEUIProperty.TESDKParam> usedProperties = new ArrayList<>();
        TEUIProperty templateSDKParam = new TEUIProperty();
        getUsedProperties(uiProperties, usedProperties, templateSDKParam);
        if (templateSDKParam.sdkParam != null) {  //表示有模板数据.需要删除 美颜和滤镜数据
            Iterator<TEUIProperty.TESDKParam> iterator = usedProperties.iterator();
            while (iterator.hasNext()) {
                TEUIProperty.TESDKParam tesdkParam = iterator.next();
                if (ProviderUtils.isBeautyOrLutName(tesdkParam.effectName)) {
                    iterator.remove();
                }
            }
        }
        return usedProperties;
    }


    public static boolean isBeautyOrLutName(String effectName) {
        if (TextUtils.isEmpty(effectName)) {
            return false;
        }
        if (XmagicConstant.EffectName.EFFECT_LUT.equals(effectName)) {
            return true;
        }
        if (effectName.startsWith("body.")) {
            return false;
        }
        if (TEUIProperty.TESDKParam.BEAUTY_TEMPLATE_EFFECT_NAME.equals(effectName)) {
            return false;
        }
        return !XmagicConstant.EffectName.EFFECT_MOTION.equals(effectName) && !XmagicConstant.EffectName.EFFECT_MAKEUP.equals(effectName) && !XmagicConstant.EffectName.EFFECT_SEGMENTATION.equals(effectName) && !XmagicConstant.EffectName.EFFECT_LIGHT_MAKEUP.equals(effectName);
    }


    private static void getUsedProperties(List<TEUIProperty> uiProperties, List<TEUIProperty.TESDKParam> properties, TEUIProperty templateSDKParam) {
        if (uiProperties != null && !uiProperties.isEmpty()) {
            for (TEUIProperty uiProperty : uiProperties) {
                if (uiProperty == null) {
                    continue;
                }
                if (uiProperty.getUiState() != TEUIProperty.UIState.INIT && uiProperty.sdkParam != null
                        && uiProperty.uiCategory != TEUIProperty.UICategory.GREEN_BACKGROUND_V2_ITEM
                        && uiProperty.uiCategory != TEUIProperty.UICategory.LIGHT_MAKEUP_ITEM
                        && uiProperty.uiCategory != TEUIProperty.UICategory.LIGHT_MAKEUP_BACK) {
                    properties.add(uiProperty.sdkParam);
                }
                if (uiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY_TEMPLATE && uiProperty.getUiState() != TEUIProperty.UIState.INIT && uiProperty.paramList != null && !uiProperty.paramList.isEmpty()) {
                    templateSDKParam.sdkParam = uiProperty.sdkParam;
                }
                if (uiProperty.propertyList != null) {
                    getUsedProperties(uiProperty.propertyList, properties, templateSDKParam);
                }
            }
        }
    }


    /**
     * 对json中解析出来的数据 模板数据进行加工处理
     * 返回 美颜模板中被选中项的 数据
     *
     * @param teuiProperty
     */
    public static List<TEUIProperty.TESDKParam> processTemplateData(TEUIProperty teuiProperty) {
        List<TEUIProperty.TESDKParam> result = null;
        if (teuiProperty != null
                && teuiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY_TEMPLATE
                && teuiProperty.propertyList != null
                && !teuiProperty.propertyList.isEmpty()) {
            Gson gson = new Gson();
            for (TEUIProperty uiProperty : teuiProperty.propertyList) {
                TEUIProperty.TESDKParam tesdkParam = new TEUIProperty.TESDKParam();
                tesdkParam.effectName = TEUIProperty.TESDKParam.BEAUTY_TEMPLATE_EFFECT_NAME;
                tesdkParam.effectValue = uiProperty.id;
                tesdkParam.resourcePath = uiProperty.paramList != null ? gson.toJson(uiProperty.paramList) : null;
                tesdkParam.tag = uiProperty.paramList;
                uiProperty.sdkParam = tesdkParam;
                if (uiProperty.getUiState() >= TEUIProperty.UIState.IN_USE) {
                    result = uiProperty.paramList;
                }
            }
        }
        return result;
    }


    /**
     * 对上次的数据进行处理，主要是处理模板数据，这种数据比较特殊，需要给tag 赋值
     *
     * @param sdkParamList
     */
    public static void processLastData(List<TEUIProperty.TESDKParam> sdkParamList) {
        if (sdkParamList == null) {
            return;
        }
        Gson gson = new Gson();
        for (TEUIProperty.TESDKParam tesdkParam : sdkParamList) {
            if (TEUIProperty.TESDKParam.BEAUTY_TEMPLATE_EFFECT_NAME.equals(tesdkParam.effectName) && !TextUtils.isEmpty(tesdkParam.resourcePath) && tesdkParam.tag == null) {
                tesdkParam.tag = gson.fromJson(tesdkParam.resourcePath,
                        new com.google.gson.reflect.TypeToken<List<TEUIProperty.TESDKParam>>() {
                        }.getType());
            }
        }
    }


    public static void completionResPath(TEUIProperty uiProperty) {
        if (uiProperty == null) {
            return;
        }
        if (uiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY || uiProperty.uiCategory == TEUIProperty.UICategory.BODY_BEAUTY) {
            return;
        }
        if (uiProperty.sdkParam != null && !TextUtils.isEmpty(uiProperty.sdkParam.resourcePath)) {
            if (!uiProperty.sdkParam.resourcePath.contains(TEBeautyKit.getResPath())) {
                uiProperty.sdkParam.resourcePath = TEBeautyKit.getResPath() + uiProperty.sdkParam.resourcePath;
            }
        }
    }


    public static void createDlModelAndSDKParam(TEUIProperty teuiProperty, TEUIProperty.UICategory uiCategory) {
        switch (uiCategory) {
            case LUT:
            case MAKEUP:
            case MOTION:
            case LIGHT_MAKEUP:
            case SEGMENTATION:
                if (!TextUtils.isEmpty(teuiProperty.resourceUri)) {
                    String downloadPath = ProviderUtils.getDownloadPath(teuiProperty);
                    if (teuiProperty.sdkParam == null) {
                        teuiProperty.sdkParam = new TEUIProperty.TESDKParam();
                    }
                    if (teuiProperty.resourceUri.startsWith(HTTP_NAME)) {
                        String fileName = FileUtil.getFileNameByHttpUrl(teuiProperty.resourceUri);
                        if (!TextUtils.isEmpty(fileName)) {
                            teuiProperty.dlModel = new TEMotionDLModel(downloadPath, fileName, teuiProperty.resourceUri);
                            if (fileName.endsWith(ZIP_NAME)) {
                                teuiProperty.sdkParam.resourcePath = downloadPath + teuiProperty.dlModel.getFileNameNoZip();
                            } else if (!TEDownloadUrlHelper.hasExtension(fileName)) {
                                // 无扩展名地址：优先用下载时持久化的真实路径恢复
                                String recorded = TEDownloadRegistry.query(teuiProperty.resourceUri);
                                teuiProperty.sdkParam.resourcePath = !TextUtils.isEmpty(recorded)
                                        ? recorded : downloadPath + fileName;
                            } else {
                                teuiProperty.sdkParam.resourcePath = downloadPath + fileName;
                            }
                        } else {
                            // 无法从 URL 解析出文件名时，回退为原始地址
                            teuiProperty.sdkParam.resourcePath = teuiProperty.resourceUri;
                        }
                    } else {
                        teuiProperty.sdkParam.resourcePath = teuiProperty.resourceUri;
                    }
                }
                break;
            case BEAUTY_TEMPLATE:
                processBeautyTemplateLutParams(teuiProperty);
                break;
        }
    }


    /**
     * 处理美颜模板（BEAUTY_TEMPLATE）中的 LUT 参数：
     * 遍历 {@code teuiProperty.paramList}，对 effectName == EFFECT_LUT 且 resourcePath 为
     * http(s) 远端链接的项，构建 {@link TEMotionDLModel} 下载模型，并将 resourcePath
     * 补全为"下载目录 + 文件名（zip 则去掉后缀）"的本地路径。
     *
     * @param teuiProperty 模板属性节点，为空或 paramList 为空时无操作
     */
    private static void processBeautyTemplateLutParams(TEUIProperty teuiProperty) {
        if (teuiProperty == null || teuiProperty.paramList == null) {
            return;
        }
        for (TEUIProperty.TESDKParam tesdkParam : teuiProperty.paramList) {
            if (!XmagicConstant.EffectName.EFFECT_LUT.equals(tesdkParam.effectName)
                    || tesdkParam.resourcePath == null
                    || !tesdkParam.resourcePath.startsWith(HTTP_NAME)) {
                continue;
            }
            String downloadPath = ProviderUtils.getDownloadPath(teuiProperty);
            String fileName = FileUtil.getFileNameByHttpUrl(tesdkParam.resourcePath);
            if (!TextUtils.isEmpty(fileName)) {
                teuiProperty.dlModel = new TEMotionDLModel(downloadPath, fileName, tesdkParam.resourcePath);
                if (fileName.endsWith(ZIP_NAME)) {
                    tesdkParam.resourcePath = downloadPath + teuiProperty.dlModel.getFileNameNoZip();
                } else if (!TEDownloadUrlHelper.hasExtension(fileName)) {
                    String recorded = TEDownloadRegistry.query(tesdkParam.resourcePath);
                    tesdkParam.resourcePath = !TextUtils.isEmpty(recorded)
                            ? recorded : downloadPath + fileName;
                } else {
                    tesdkParam.resourcePath = downloadPath + fileName;
                }
            }
        }
    }


    private static String getDownloadPath(TEUIProperty teuiProperty) {
        if (teuiProperty == null) {
            return null;
        }
        if (!TextUtils.isEmpty(teuiProperty.downloadPath)) {
            return teuiProperty.downloadPath;
        } else {
            return ProviderUtils.getDownloadPath(teuiProperty.parentUIProperty);
        }
    }


    public static TEUIProperty.TESDKParam createNoneItem(String effectName) {
        TEUIProperty.TESDKParam param = new TEUIProperty.TESDKParam();
        param.effectName = effectName;
        return param;
    }


    public static boolean findFirstInUseItemAndMakeChecked(List<TEUIProperty> allData) {
        if (allData == null) {
            return false;
        }
        for (TEUIProperty teuiProperty : allData) {
            if (teuiProperty.propertyList != null) {
                if (findFirstInUseItemAndMakeChecked(teuiProperty.propertyList)) {
                    return true;
                }
            } else if (teuiProperty.sdkParam != null && teuiProperty.getUiState() == TEUIProperty.UIState.IN_USE) {
                teuiProperty.setUiState(TEUIProperty.UIState.CHECKED_AND_IN_USE);
                ProviderUtils.changeParentUIState(teuiProperty, TEUIProperty.UIState.CHECKED_AND_IN_USE);
                return true;
            }
        }
        return false;
    }


    public static void changeParentUIState(TEUIProperty current, int uiState) {
        TEUIProperty parent = current.parentUIProperty;
        if (parent != null) {
            parent.setUiState(uiState);
            ProviderUtils.changeParentUIState(parent, uiState);
        }
    }

    public static boolean isPointMakeup(TEUIProperty.TESDKParam sdkParam) {
        if (sdkParam == null || TextUtils.isEmpty(sdkParam.effectName)) {
            return false;
        }
        return pointMakeupEffectName.contains(sdkParam.effectName);
    }

    public static List<String> pointMakeupEffectName = Arrays.asList(
            XmagicConstant.EffectName.BEAUTY_MOUTH_LIPSTICK,
            XmagicConstant.EffectName.BEAUTY_FACE_RED_CHEEK,
            XmagicConstant.EffectName.BEAUTY_FACE_SOFTLIGHT,
            XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYE_SHADOW,
            XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYE_LINER,
            XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYELASH,
            XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYE_SEQUINS,
            XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYEBROW,
            XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYEBALL,
            XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYELIDS,
            XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYEWOCAN,
            XmagicConstant.EffectName.EFFECT_LUT
    );


    public static String getGreenParamsV2(TEUIProperty teuiProperty) {
        int[] result = new int[5];
        if (teuiProperty.propertyList == null || teuiProperty.propertyList.isEmpty()) {
        } else {
            for (TEUIProperty item : teuiProperty.propertyList) {
                if (item.sdkParam == null) {
                    continue;
                }
                if (BACKGROUND_V2_SIMILARITY.equals(item.sdkParam.effectName)) {
                    result[0] = item.sdkParam.effectValue;
                } else if (BACKGROUND_V2_SMOOTH.equals(item.sdkParam.effectName)) {
                    result[1] = item.sdkParam.effectValue;
                } else if (BACKGROUND_V2_CORROSION.equals(item.sdkParam.effectName)) {
                    result[2] = item.sdkParam.effectValue;
                } else if (BACKGROUND_V2_DE_SPILL.equals(item.sdkParam.effectName)) {
                    result[3] = item.sdkParam.effectValue;
                } else if (BACKGROUND_V2_DE_SHADOW.equals(item.sdkParam.effectName)) {
                    result[4] = item.sdkParam.effectValue;
                }
            }
        }
        return valueGSParamsV2(result);
    }


    private static String valueGSParamsV2(int[] params) {
        // 使用StringBuilder构建结果字符串
        StringBuilder result = new StringBuilder("[");
        for (float item : params) {
            result.append(item).append(",");
        }
        // 删除最后一个逗号
        if (result.length() > 1) { // 确保 StringBuilder 不为空
            result.deleteCharAt(result.length() - 1);
        }
        result.append("]");
        return result.toString();
    }


    public static TEUIProperty getImportTEUIPropertyItem(TEUIProperty teuiProperty) {
        List<TEUIProperty> teuiPropertyList = teuiProperty.propertyList;
        if (teuiPropertyList == null || teuiPropertyList.isEmpty()) {
            return null;
        }
        for (TEUIProperty property : teuiPropertyList) {
            if (property.uiCategory == TEUIProperty.UICategory.GREEN_BACKGROUND_V2_ITEM_IMPORT_IMAGE) {
                return property;
            }
        }
        return null;
    }


    /**
     * 移除 {@code teuiProperty} 下 <b>包含染发 LUT 子项的那一整个分组</b>。
     * <p>
     * 行为说明：
     * <ol>
     *     <li>在 {@code teuiProperty} 为根的属性树中递归查找 effectName 等于
     *         {@link XmagicConstant.EffectName#BEAUTY_HAIR_COLOR_LUT} 的节点；</li>
     *     <li>取该命中节点的 <b>父节点</b>，将其从 {@code teuiProperty.propertyList} 中整体移除；</li>
     *     <li>若未找到、或命中节点无父节点，则不做任何操作。</li>
     * </ol>
     * 典型使用场景：某些套餐（如 S1_00）需要在 {@code BEAUTY_MAKEUP} 分类下裁掉
     * 染发 LUT 所属的整个分组。
     *
     * @param teuiProperty 待处理的父节点，为空或 propertyList 为空时无操作
     */
    public static void removeHairColorGroup(TEUIProperty teuiProperty) {
        if (teuiProperty == null || teuiProperty.propertyList == null || teuiProperty.propertyList.isEmpty()) {
            return;
        }
        TEUIProperty parent = getParentByEffectName(teuiProperty, XmagicConstant.EffectName.BEAUTY_HAIR_COLOR_LUT);
        if (parent != null) {
            teuiProperty.propertyList.remove(parent);
        }
    }

    /**
     * 在 {@code teuiProperty} 为根的属性树中，深度优先递归查找
     * {@link TEUIProperty.TESDKParam#effectName} 等于 {@code effectName} 的第一个节点，
     * 并返回该命中节点的 <b>父节点</b>。
     * <p>
     * 说明：此处 <b>不依赖</b> {@link TEUIProperty#parentUIProperty}（该字段在部分调用时机尚未回填），
     * 而是通过在递归下探时显式携带 parent 参数来定位父节点。
     * <p>
     * 匹配规则：
     * <ul>
     *     <li>当前节点若 {@code sdkParam != null} 且 effectName 完全相等，则视为命中，返回携带下来的 parent；</li>
     *     <li>否则按顺序递归其 {@code propertyList} 中的每个子节点（此时当前节点作为下一层的 parent）；</li>
     *     <li>返回首个命中的 parent。若命中节点本身即为根（parent 为 null），返回 null。</li>
     * </ul>
     *
     * @param teuiProperty 待查找的属性树根节点，为空时返回 null
     * @param effectName   要匹配的 effectName，为空时返回 null
     * @return 首个命中节点的父节点；未找到或命中节点无父节点时返回 null
     */
    private static TEUIProperty getParentByEffectName(TEUIProperty teuiProperty, String effectName) {
        if (teuiProperty == null || TextUtils.isEmpty(effectName)) {
            return null;
        }
        return findParentByEffectName(null, teuiProperty, effectName);
    }

    /**
     * 递归实现：以 {@code current} 为当前节点、{@code parent} 为其父节点向下查找。
     * 命中时返回 parent；未命中则继续遍历 current 的子节点。
     */
    private static TEUIProperty findParentByEffectName(TEUIProperty parent,
                                                       TEUIProperty current,
                                                       String effectName) {
        if (current == null) {
            return null;
        }
        if (current.sdkParam != null && effectName.equals(current.sdkParam.effectName)) {
            return parent;
        }
        if (current.propertyList == null || current.propertyList.isEmpty()) {
            return null;
        }
        for (TEUIProperty child : current.propertyList) {
            TEUIProperty foundParent = findParentByEffectName(current, child, effectName);
            if (foundParent != null) {
                return foundParent;
            }
        }
        return null;
    }
    
    

    /**
     * 删除 {@code teuiProperty.propertyList} 中 {@link TEUIProperty#uiCategory}
     * 命中 {@code categories} 的一级子项（只遍历一层）。
     * <p>
     * 常用于套餐规则中"保留父分类但裁掉某些子分类"的场景。
     *
     * @param teuiProperty 待处理的父属性；为空或 propertyList 为空时无操作
     * @param categories   需要删除的子分类；为空时无操作
     */
    public static void removeChildrenByCategory(TEUIProperty teuiProperty,
                                                TEUIProperty.UICategory... categories) {
        if (teuiProperty == null
                || teuiProperty.propertyList == null
                || teuiProperty.propertyList.isEmpty()
                || categories == null
                || categories.length == 0) {
            return;
        }
        Iterator<TEUIProperty> iterator = teuiProperty.propertyList.iterator();
        while (iterator.hasNext()) {
            TEUIProperty item = iterator.next();
            if (item == null) {
                continue;
            }
            for (TEUIProperty.UICategory category : categories) {
                if (item.uiCategory == category) {
                    iterator.remove();
                    break;
                }
            }
        }
    }

}