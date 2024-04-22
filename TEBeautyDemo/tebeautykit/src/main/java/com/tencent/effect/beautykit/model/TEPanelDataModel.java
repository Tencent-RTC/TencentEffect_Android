package com.tencent.effect.beautykit.model;


/**
 * 美颜面板数据的包装类，根据此类中的json文件的路径解析美颜属性数据
 */
public class TEPanelDataModel {

    public String jsonFilePath;
    public TEUIProperty.UICategory category;
    public String abilityType;

    public TEPanelDataModel() {
    }

    public TEPanelDataModel(String jsonFilePath, TEUIProperty.UICategory category) {
        this.jsonFilePath = jsonFilePath;
        this.category = category;
    }

    public TEPanelDataModel(String jsonFilePath, TEUIProperty.UICategory category, String abilityType) {
        this.jsonFilePath = jsonFilePath;
        this.category = category;
        this.abilityType = abilityType;
    }



}
