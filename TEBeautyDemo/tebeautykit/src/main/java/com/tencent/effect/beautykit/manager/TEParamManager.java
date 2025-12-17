package com.tencent.effect.beautykit.manager;

import android.text.TextUtils;
import android.util.Log;

import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.utils.provider.ProviderUtils;
import com.tencent.xmagic.XmagicConstant;
import com.tencent.effect.beautykit.model.TEUIProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Used for managing beauty attributes of TEParam.
 * When users set beauty data to the SDK, they can also set it to this class.
 * When users want to retrieve the currently effective beauty attributes, they can use the getParams method of this class to obtain them.
 * The main use case is when the user sets beauty attributes to the SDK, and after disabling the beauty effect, when enabling it again, it restores to the previous non-beauty effect.
 * Usage:
 * When setting beauty attributes to the SDK, call the putTEParam or putTEParams method of this class.
 * When the user disables the beauty effect, they can use the getParams method to retrieve the previously set beauty attributes.
 * The data obtained in the second step can be saved to local storage or memory.
 * When using the beauty effect again, retrieve the data from the third step and set it to the beauty SDK to restore the previous effect.
 */
public class TEParamManager {

    private final Map<String, TEUIProperty.TESDKParam> allData = new LinkedHashMap<>();


    public void putTEParam(TEUIProperty.TESDKParam param) {
        if (param != null && !TextUtils.isEmpty(param.effectName)) {
            if (XmagicConstant.EffectName.EFFECT_LIGHT_MAKEUP.equals(param.effectName)) { //如果是轻美妆
                //当设置了轻美妆的时候 需要删除 单点美妆和滤镜
                this.removePointMakeup();
            }
            if (ProviderUtils.isPointMakeup(param) && TEUIConfig.getInstance().cleanLightMakeup) {
                //当设置了单点妆容或滤镜的时候 需要删除 轻美妆
                this.removeLightMakeup();
            }
            if (!this.isCameraMoveItem(param)) {
                allData.put(getKey(param), param);
            }
        }
    }

    private void removePointMakeup() {
        for (String effectName : ProviderUtils.pointMakeupEffectName) {
            allData.remove(effectName);
        }
    }

    private void removeLightMakeup() {
        allData.remove(XmagicConstant.EffectName.EFFECT_LIGHT_MAKEUP);
    }


    public void remove(TEUIProperty.TESDKParam param) {
        if (param == null) {
            return;
        }
        allData.remove(getKey(param));
    }


    public void putTEParams(List<TEUIProperty.TESDKParam> paramList) {
        if (paramList != null && !paramList.isEmpty()) {
            for (TEUIProperty.TESDKParam teParam : paramList) {
                putTEParam(teParam);
            }
        }
    }


    /**
     * 获取模板数据
     * @return
     */
    public TEUIProperty.TESDKParam getTemplateData() {
        return allData.get(TEUIProperty.TESDKParam.BEAUTY_TEMPLATE_EFFECT_NAME);
    }


    private String getKey(TEUIProperty.TESDKParam param) {
        switch (param.effectName) {
            case XmagicConstant.EffectName.BEAUTY_SMOOTH:
            case XmagicConstant.EffectName.BEAUTY_SMOOTH2:
            case XmagicConstant.EffectName.BEAUTY_SMOOTH3:
            case XmagicConstant.EffectName.BEAUTY_SMOOTH4:
                return XmagicConstant.EffectName.BEAUTY_SMOOTH;
            case XmagicConstant.EffectName.BEAUTY_WHITEN_0:
            case XmagicConstant.EffectName.BEAUTY_WHITEN:
            case XmagicConstant.EffectName.BEAUTY_WHITEN_2:
            case XmagicConstant.EffectName.BEAUTY_WHITEN_3:
                return XmagicConstant.EffectName.BEAUTY_WHITEN;
            case XmagicConstant.EffectName.BEAUTY_BLACK_1:
            case XmagicConstant.EffectName.BEAUTY_BLACK_2:
                return XmagicConstant.EffectName.BEAUTY_BLACK_1;
            case XmagicConstant.EffectName.BEAUTY_FACE_NATURE:
            case XmagicConstant.EffectName.BEAUTY_FACE_GODNESS:
            case XmagicConstant.EffectName.BEAUTY_FACE_MALE_GOD:
                return XmagicConstant.EffectName.BEAUTY_FACE_NATURE;
            case XmagicConstant.EffectName.EFFECT_MAKEUP:
            case XmagicConstant.EffectName.EFFECT_SEGMENTATION:
            case XmagicConstant.EffectName.EFFECT_MOTION:
                return XmagicConstant.EffectName.EFFECT_MOTION;
            default:
                return param.effectName;
        }
    }


    /**
     * @param param
     * @return
     */
    private boolean isCameraMoveItem(TEUIProperty.TESDKParam param) {
        if (!XmagicConstant.EffectName.EFFECT_MOTION.equals(param.effectName)) {
            return false;
        }
        String resPath = param.resourcePath;
        boolean mergeWithCurrentMotion = false;
        if (param.extraInfo != null) {
            String merge = param.extraInfo.get("mergeWithCurrentMotion");
            if ("true".equals(merge)) {
                mergeWithCurrentMotion = true;
            }
        }
        return resPath != null && resPath.contains("video_camera_move_") && mergeWithCurrentMotion;
    }


    public List<TEUIProperty.TESDKParam> getParams() {
        return new ArrayList<>(allData.values());
    }


    public void clear() {
        this.allData.clear();
    }


    public boolean isEmpty() {
        return this.allData.isEmpty();
    }

}
