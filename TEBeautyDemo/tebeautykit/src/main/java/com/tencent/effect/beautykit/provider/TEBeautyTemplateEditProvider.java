package com.tencent.effect.beautykit.provider;

import android.content.Context;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.effect.beautykit.manager.TEParamManager;
import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.utils.provider.ProviderUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 一键美颜编辑页面
 */
public class TEBeautyTemplateEditProvider extends TEAbstractPanelDataProvider {


    private TEParamManager teParamManager = new TEParamManager();

    /**
     * 用于存放还原美颜效果的数据
     * 由于美颜面板中的数据还原不能使用 json中的数据，需要使用外部传入的数据，所以此处特殊处理
     */
    private String originalParamData = null;

    public void setOriginalParamData(String originalParamData) {
        this.originalParamData = originalParamData;
    }

    @Override
    public List<TEUIProperty> onItemClick(TEUIProperty uiProperty) {
        if (uiProperty == null) {
            return null;
        } else {
            List<TEUIProperty> processData = new ArrayList<>();
            for (TEUIProperty property : allData) {   //通过此方法将美颜和美体数据分开
                if (property.uiCategory == uiProperty.uiCategory) {
                    processData.add(property);
                }
            }
            if ((uiProperty.propertyList == null && uiProperty.sdkParam != null) || uiProperty.isNoneItem()) {
                ProviderUtils.revertUIState(processData, uiProperty);
                ProviderUtils.changeParamUIState(uiProperty, TEUIProperty.UIState.CHECKED_AND_IN_USE);
            }
            return uiProperty.propertyList;
        }
    }


    /**
     * 此处的还原逻辑如下
     * 1. 获取当前面板上选中的所有数据，这个数据就是当前美颜效果的数据
     * 2. 将所有数据的值都调整为0，并且添加进teParamManager中
     * 3. 解析需要还原的到的美颜效果数据
     * 4. 将这个数据设置给provider的originalParamList变量，用于界面还原处理
     * 5. 再将这个数据数据写入 teParamManager中
     * 6. 通过getNewPanelData方法对数据进行刷新操作
     * 7. 通过teParamManager的getParams()方法返回用于还原美颜效果的数据
     *
     * @param context 应用上下文
     * @return
     */
    @Override
    public List<TEUIProperty.TESDKParam> getRevertData(Context context) {
        this.teParamManager.clear();
        //获取所有使用的美颜属性
        List<TEUIProperty.TESDKParam> list = this.getUsedProperties();
        //将数据的数值修改为0
        for (TEUIProperty.TESDKParam teParam : list) {
            teParam.effectValue = 0;
            this.teParamManager.putTEParam(teParam);
        }
        Type type = new TypeToken<List<TEUIProperty.TESDKParam>>() {
        }.getType();
        //因为还原的时候需要将效果和面板选中状态都还原到 原始数据的状态  originalParamList 就是用于还原面板选中状态的数据
        this.originalParamList = new Gson().fromJson(this.originalParamData, type);
        this.teParamManager.putTEParams(this.originalParamList);
        this.forceRefreshPanelData(context);
        return this.teParamManager.getParams();
    }


    @Override
    public List<TEUIProperty.TESDKParam> getCloseEffectItems(TEUIProperty uiProperty) {
        for (TEUIProperty teuiProperty : allData) {
            if (teuiProperty == uiProperty) {
                List<TEUIProperty.TESDKParam> usedList = ProviderUtils.getUsedProperties(teuiProperty.propertyList);
                ProviderUtils.changParamValuedTo0(usedList);
                return usedList;
            }
        }
        return null;
    }


    @Override
    public List<TEUIProperty.TESDKParam> getUsedProperties() {
        List<TEUIProperty.TESDKParam> properties = ProviderUtils.getUsedProperties(allData);
        List<TEUIProperty.TESDKParam> resultList = new ArrayList<>();
        for (TEUIProperty.TESDKParam param : properties) {
            try {
                resultList.add(param.clone());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return resultList;
    }

    @Override
    public boolean isShowCompareBtn() {
        return true;
    }


}
