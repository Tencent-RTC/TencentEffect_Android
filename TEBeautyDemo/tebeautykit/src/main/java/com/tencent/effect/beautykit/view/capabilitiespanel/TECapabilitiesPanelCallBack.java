package com.tencent.effect.beautykit.view.capabilitiespanel;



import com.tencent.effect.beautykit.model.TECapabilitiesModel;

/**
 * TEBeautyPanel的点击事件回调
 */
public interface TECapabilitiesPanelCallBack {

    /**
     * 当关闭或打开特效开关时回调此方法
     *
     * @param isChecked
     */
    void onCheckedChanged(TECapabilitiesModel capabilitiesItem, boolean isChecked);


    /**
     * 当点击拍照按钮的时候回调
     */
    void onCameraClick();
}
