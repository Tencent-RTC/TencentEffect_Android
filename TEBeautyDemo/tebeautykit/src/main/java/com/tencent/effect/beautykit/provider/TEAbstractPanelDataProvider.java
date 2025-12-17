package com.tencent.effect.beautykit.provider;


import android.content.Context;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.google.gson.Gson;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.xmagic.XmagicConstant;
import com.tencent.effect.beautykit.utils.provider.ProviderUtils;
import com.tencent.xmagic.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class TEAbstractPanelDataProvider implements TEPanelDataProvider {

    private static final String TAG = TEAbstractPanelDataProvider.class.getName();

    protected Context applicationContext = null;
    protected List<TEUIProperty> allData = null;


    /**
     * Used to store the mapping between TEParam.effectName and TEUIProperty.
     */
    private final Map<String, TEUIProperty> indexUIPropertyMap = new ArrayMap<>();

    private List<TEPanelDataModel> panelDataModels = null;


    @Override
    public void setPanelDataList(List<TEPanelDataModel> dataModels) {
        this.panelDataModels = dataModels;
    }

    /**
     * Used to store the incoming data, which is the original data.
     * This data is mainly used to restore the selected state of UI on the panel to the previous state.
     */
    protected List<TEUIProperty.TESDKParam> originalParamList = null;


    @Override
    public void setUsedParams(List<TEUIProperty.TESDKParam> paramList) {
        this.originalParamList = paramList;
    }

    @Override
    public List<TEUIProperty> getPanelData(Context context) {
        if (allData != null) {
            return allData;
        }
        if (TEUIConfig.getInstance().revertEffect2Json) {
            this.originalParamList = null;
        }
        return this.forceRefreshPanelData(context);
    }

    @Override
    public List<TEUIProperty> forceRefreshPanelData(Context context) {
        this.applicationContext = context.getApplicationContext();
        if (allData == null) {
            allData = new ArrayList<>();
        } else {
            allData.clear();
        }
        indexUIPropertyMap.clear();
        TEUIProperty templateData = null;
        for (TEPanelDataModel dataModel : this.panelDataModels) {
            TEUIProperty uiProperty = this.loadUIPropertyFromJson(dataModel.jsonFilePath, context);
            if (uiProperty == null) {
                continue;
            }
            if (uiProperty.uiCategory == null) {
                uiProperty.uiCategory = dataModel.category;
            }
            uiProperty.titleType = uiProperty.displayName;
            this.initializeUIPropertyHierarchy(uiProperty.propertyList, dataModel.category, uiProperty.titleType, uiProperty);
            if (uiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY_TEMPLATE) {
                templateData = uiProperty;
            }
            this.indexUIProperty(uiProperty);
            this.allData.add(uiProperty);
        }
        this.restoreUIStateFromParams(this.originalParamList, "originalParamList");     //将上次的数据进行同步
        //被选中的模板中的美颜数据
        List<TEUIProperty.TESDKParam> checkedTemplateBeautyData = ProviderUtils.processTemplateData(templateData);
        this.restoreUIStateFromParams(checkedTemplateBeautyData, "checkedTemplateBeautyData");   //将模板项同步到美颜、滤镜上
        return allData;
    }


    @Override
    public void onTabSelected(int index) {
        if (index < 0 || index >= allData.size()) {
            return;
        }
        for (int i = 0; i < allData.size(); i++) {
            TEUIProperty item = allData.get(i);
            if (i == index) {
                item.setUiState(TEUIProperty.UIState.CHECKED_AND_IN_USE);
            } else {
                item.setUiState(TEUIProperty.UIState.INIT);
            }
        }
    }

    @Override
    public abstract List<TEUIProperty> onHandRecycleViewItemClick(TEUIProperty uiProperty);


    @Override
    public abstract List<TEUIProperty.TESDKParam> getRevertData(Context context);

    @Override
    public abstract List<TEUIProperty.TESDKParam> getCloseEffectItems(TEUIProperty uiProperty);


    @Override
    public List<TEUIProperty.TESDKParam> getUsedProperties() {
        return ProviderUtils.getUsedProperties(allData);
    }

    @Override
    public boolean isShowCompareBtn() {
        return true;
    }


    /**
     * 根据参数列表恢复UI属性的状态和值
     *
     * @param originalParamList 原始参数列表，包含需要恢复的UI属性参数
     * @param TAG               日志标签，用于调试和跟踪
     *                          <p>
     *                          方法功能：
     *                          1. 遍历参数列表，通过索引映射找到对应的UI属性
     *                          2. 更新UI属性的effectValue和extraInfo
     *                          3. 根据UI分类设置不同的UI状态：
     *                          - BEAUTY和BODY_BEAUTY类型：设置为IN_USE状态
     *                          - 其他类型：设置为CHECKED_AND_IN_USE状态
     *                          4. 同时更新父属性的UI状态
     *                          <p>
     *                          使用场景：
     *                          - 面板数据刷新时恢复上次的选中状态
     *                          - 模板数据同步到美颜、滤镜等UI属性
     */
    public void restoreUIStateFromParams(List<TEUIProperty.TESDKParam> originalParamList, String TAG) {
        if (originalParamList == null || originalParamList.isEmpty()) {
            return;
        }
        for (TEUIProperty.TESDKParam param : originalParamList) {
            if (param == null) {
                continue;
            }
            // Assuming this data is for beauty or body enhancement, we can query it using the effectName.
            TEUIProperty teuiProperty = indexUIPropertyMap.get(this.getNameMapKey(param));
            if (teuiProperty != null) {
                if (teuiProperty.sdkParam != null) {
                    teuiProperty.sdkParam.effectValue = param.effectValue;
                    teuiProperty.sdkParam.extraInfo = param.extraInfo;
                }
                if (teuiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY || teuiProperty.uiCategory == TEUIProperty.UICategory.BODY_BEAUTY) {
                    teuiProperty.setUiState(TEUIProperty.UIState.IN_USE);
                    ProviderUtils.changeParentUIState(teuiProperty, TEUIProperty.UIState.IN_USE);
                } else {
                    teuiProperty.setUiState(TEUIProperty.UIState.CHECKED_AND_IN_USE);
                    ProviderUtils.changeParentUIState(teuiProperty, TEUIProperty.UIState.CHECKED_AND_IN_USE);
                }
            }
        }
    }


    /**
     * 将UI属性添加到索引映射中，便于后续快速查找
     *
     * @param property 要添加的UI属性对象
     *                 对于BEAUTY_TEMPLATE类型的属性：
     *                 - 只有当originalParamList不为空且property.paramList不为空时才添加到映射
     *                 - 只添加包含真实美颜数据的模板节点
     *                 对于其他类型的属性：直接添加到映射中
     *                 所有属性都会设置初始UI状态为INIT
     */
    private void indexUIProperty(TEUIProperty property) {
        if (property.uiCategory == TEUIProperty.UICategory.BEAUTY_TEMPLATE) {
            if (this.originalParamList != null) {
                property.setUiState(TEUIProperty.UIState.INIT);
                if (property.paramList != null) {   //只添加有真实美颜数据的  模板节点
                    String uiIndex = this.getNameMapKey(property);
                    if (!TextUtils.isEmpty(uiIndex)) {
                        indexUIPropertyMap.put(uiIndex, property);
                    }
                }
            }
        } else {
            property.setUiState(TEUIProperty.UIState.INIT);
            String uiIndex = this.getNameMapKey(property);
            if (!TextUtils.isEmpty(uiIndex)) {
                indexUIPropertyMap.put(uiIndex, property);
            }
        }
    }


    /**
     * 为TESDKParam对象生成唯一的映射键
     *
     * @param teuiProperty UI属性对象
     * @return 映射键，如果参数为null或无效则返回null
     * <p>
     * 特殊处理逻辑：
     * - 对于BEAUTY_TEMPLATE类型且包含参数列表的模板数据，使用"BEAUTY_TEMPLATE_KEY + id"作为键
     * - 对于其他类型，委托给getNameMapKey(TESDKParam)方法处理
     */
    private String getNameMapKey(TEUIProperty teuiProperty) {
        if (teuiProperty == null) {
            return null;
        }
        if (teuiProperty.uiCategory == TEUIProperty.UICategory.BEAUTY_TEMPLATE && teuiProperty.paramList != null && !teuiProperty.paramList.isEmpty()) { //在这里判断一下是不是模板数据，如果是直接返回
            return TEUIProperty.TESDKParam.BEAUTY_TEMPLATE_EFFECT_NAME + teuiProperty.id;
        }
        if (teuiProperty.sdkParam == null) {
            return null;
        }
        return getNameMapKey(teuiProperty.sdkParam);
    }

    /**
     * 为TESDKParam对象生成唯一的映射键
     *
     * @param param SDK参数对象
     * @return 映射键，由effectName和resourcePath组合而成
     * <p>
     * 特殊处理逻辑：
     * - 对于BEAUTY_TEMPLATE类型的参数，使用"BEAUTY_TEMPLATE_KEY + effectValue"作为键
     * - 对于其他类型，组合effectName和resourcePath生成键
     */
    private String getNameMapKey(TEUIProperty.TESDKParam param) {
        StringBuilder keyBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(param.effectName)) {
            if (TEUIProperty.TESDKParam.BEAUTY_TEMPLATE_EFFECT_NAME.equals(param.effectName)) {   //在这里判断一下是不是模板数据，如果是直接返回
                return TEUIProperty.TESDKParam.BEAUTY_TEMPLATE_EFFECT_NAME + param.effectValue;
            }
            keyBuilder.append(param.effectName);
        }
        if (!TextUtils.isEmpty(param.resourcePath)) {
            keyBuilder.append(param.resourcePath);
        }
        return keyBuilder.toString();
    }


    /**
     * 初始化UI属性层次结构，递归设置所有相关参数
     *
     * @param list           UI属性列表
     * @param category       UI分类
     * @param titleType      标题类型
     * @param parentProperty 父属性对象
     *                       <p>
     *                       方法功能：
     *                       1. 设置父子属性关系
     *                       2. 设置UI分类（如果未设置）
     *                       3. 创建DL模型和SDK参数
     *                       4. 根据UI分类设置对应的effectName
     *                       5. 设置titleType
     *                       6. 将属性添加到索引映射
     *                       7. 递归处理子属性列表
     */
    private void initializeUIPropertyHierarchy(List<TEUIProperty> list, TEUIProperty.UICategory category,
                                               String titleType, TEUIProperty parentProperty) {
        for (TEUIProperty property : list) {
            property.parentUIProperty = parentProperty;
            if (property.uiCategory == null) {
                property.uiCategory = category;
            }
            ProviderUtils.createDlModelAndSDKParam(property, category);
            if (property.sdkParam != null) {
                switch (property.uiCategory) {
                    case LUT:
                        property.sdkParam.effectName = XmagicConstant.EffectName.EFFECT_LUT;
                        break;
                    case MAKEUP:
                        property.sdkParam.effectName = XmagicConstant.EffectName.EFFECT_MAKEUP;
                        break;
                    case LIGHT_MAKEUP:
                        property.sdkParam.effectName = XmagicConstant.EffectName.EFFECT_LIGHT_MAKEUP;
                        break;
                    case MOTION:
                        property.sdkParam.effectName = XmagicConstant.EffectName.EFFECT_MOTION;
                        break;
                    case SEGMENTATION:
                        property.sdkParam.effectName = XmagicConstant.EffectName.EFFECT_SEGMENTATION;
                        break;
                    default:
                        break;
                }
            }
            property.titleType = titleType;
            this.indexUIProperty(property);
            if (property.propertyList != null) {
                this.initializeUIPropertyHierarchy(property.propertyList, category, titleType, property);
            }
        }
    }

    /**
     * 从JSON文件路径加载并解析TEUIProperty对象
     *
     * @param jsonFilePath JSON文件路径，可以是绝对路径或asset路径
     * @param context      上下文对象，用于读取asset文件
     * @return 解析成功的TEUIProperty对象，如果失败则返回null
     */
    private TEUIProperty loadUIPropertyFromJson(String jsonFilePath, Context context) {
        if (TextUtils.isEmpty(jsonFilePath)) {
            return null;
        }
        String dataStr = null;
        if (jsonFilePath.startsWith(File.separator)) {
            dataStr = FileUtil.readFile(jsonFilePath);
        } else {
            dataStr = FileUtil.readAssetFile(context, jsonFilePath);
        }
        if (TextUtils.isEmpty(dataStr)) {
            return null;
        }
        return new Gson().fromJson(dataStr.trim(), TEUIProperty.class);
    }

}
