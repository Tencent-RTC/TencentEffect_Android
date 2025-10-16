package com.tencent.effect.beautykit.utils;



import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.model.TEUIProperty;



public class PanelDisplay {
    public static String getDisplayName(TEUIProperty uiProperty) {
        if (uiProperty == null) {
            return null;
        }
        String displayName = TEUIConfig.getInstance().isDefaultLanguage() ? uiProperty.displayName : uiProperty.displayNameEn;
        if (displayName == null) {
            displayName = uiProperty.displayName;
        }
        return displayName;
    }









}
