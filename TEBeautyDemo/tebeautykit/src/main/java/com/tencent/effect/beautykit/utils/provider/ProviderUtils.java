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
import com.tencent.xmagic.XmagicConstant;
import com.tencent.effect.beautykit.model.TEMotionDLModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.FileUtil;

import org.light.utils.GsonUtils;

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
                if (uiProperty.getUiState() != TEUIProperty.UIState.INIT && uiProperty.sdkParam != null && uiProperty.uiCategory != TEUIProperty.UICategory.GREEN_BACKGROUND_V2_ITEM) {
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
                if (teuiProperty.getUiState() == TEUIProperty.UIState.CHECKED_AND_IN_USE) {
                    result = uiProperty.paramList;
                }
            }
        }
        return result;
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
                    if (teuiProperty.resourceUri.startsWith(HTTP_NAME)) {
                        teuiProperty.dlModel = new TEMotionDLModel(downloadPath, FileUtil.getFileNameByHttpUrl(teuiProperty.resourceUri), teuiProperty.resourceUri);
                    }
                    if (teuiProperty.sdkParam == null) {
                        teuiProperty.sdkParam = new TEUIProperty.TESDKParam();
                    }
                    if (teuiProperty.resourceUri.startsWith(HTTP_NAME)) {
                        String fileName = FileUtil.getFileNameByHttpUrl(teuiProperty.resourceUri);
                        if (!TextUtils.isEmpty(fileName) && fileName.endsWith(ZIP_NAME)) {
                            fileName = teuiProperty.dlModel.getFileNameNoZip();
                        }
                        teuiProperty.sdkParam.resourcePath = downloadPath + fileName;
                    } else {
                        teuiProperty.sdkParam.resourcePath = teuiProperty.resourceUri;
                    }
                }
                break;
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

}