package com.tencent.effect.beautykit.utils;


import android.text.TextUtils;

import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.model.TEUIProperty;


public class PanelDisplay {
    public static String getDisplayName(TEUIProperty uiProperty) {
        if (uiProperty == null) {
            return null;
        }
        if (uiProperty.name != null) {
            String displayKey = TextUtils.isEmpty(TEUIConfig.getInstance().disPlayNameKey) ?
                    TEUIConfig.getInstance().defaultDisPlayNameKey : TEUIConfig.getInstance().disPlayNameKey;
            String name = uiProperty.name.get(displayKey);
            if (TextUtils.isEmpty(name)) {
                return "";
            } else {
                return name;
            }
        }
        String displayName =
                TEUIConfig.getInstance().isUseDisplayName() ? uiProperty.displayName : uiProperty.displayNameEn;
        if (displayName == null) {
            displayName = uiProperty.displayName;
        }
        return displayName;
    }


}
