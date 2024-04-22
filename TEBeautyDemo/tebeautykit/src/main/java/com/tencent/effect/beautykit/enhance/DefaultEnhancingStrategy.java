package com.tencent.effect.beautykit.enhance;

import com.tencent.xmagic.XmagicConstant;
import com.tencent.effect.beautykit.model.TEUIProperty;

/**
 * 增强模式的默认实现
 */
public class DefaultEnhancingStrategy implements TEParamEnhancingStrategy {



    @Override
    public TEUIProperty.TESDKParam enhanceParam(TEUIProperty.TESDKParam param){
        if (param == null) {
            return null;
        }
        switch (param.effectName) {
            //对于滤镜、动效、美妆、分割、以及美体不进行增强
            case XmagicConstant.EffectName.EFFECT_LUT:
            case XmagicConstant.EffectName.EFFECT_MAKEUP:
            case XmagicConstant.EffectName.EFFECT_MOTION:
            case XmagicConstant.EffectName.EFFECT_SEGMENTATION:
            case XmagicConstant.EffectName.BODY_AUTOTHIN_BODY_STRENGTH:
            case XmagicConstant.EffectName.BODY_LEG_STRETCH:
            case XmagicConstant.EffectName.BODY_ENLARGE_CHEST_STRENGTH:
            case XmagicConstant.EffectName.BODY_SLIM_HEAD_STRENGTH:
            case XmagicConstant.EffectName.BODY_SLIM_LEG_STRENGTH:
            case XmagicConstant.EffectName.BODY_WAIST_STRENGTH:
            case XmagicConstant.EffectName.BODY_THIN_SHOULDER_STRENGTH:
                return param;
            case XmagicConstant.BeautyConstant.BEAUTY_FACE_REMOVE_WRINKLE:  //祛皱
            case XmagicConstant.BeautyConstant.BEAUTY_FACE_REMOVE_LAW_LINE: //祛法令纹
            case XmagicConstant.BeautyConstant.BEAUTY_MOUTH_LIPSTICK:  //口红
            case XmagicConstant.BeautyConstant.BEAUTY_WHITEN:     //美白
            case XmagicConstant.BeautyConstant.BEAUTY_FACE_SOFTLIGHT:  //立体
            case XmagicConstant.BeautyConstant.BEAUTY_FACE_SHORT:  //短脸
            case XmagicConstant.BeautyConstant.BEAUTY_FACE_V:  //V脸
            case XmagicConstant.BeautyConstant.BEAUTY_EYE_DISTANCE:   //眼距
            case XmagicConstant.BeautyConstant.BEAUTY_NOSE_HEIGHT:  //鼻子位置
                return changeParamValue(param,1.3f);
            case XmagicConstant.BeautyConstant.BEAUTY_EYE_LIGHTEN:    //亮眼
                return changeParamValue(param,1.5f);
            case XmagicConstant.BeautyConstant.BEAUTY_FACE_RED_CHEEK:   //腮红
                return changeParamValue(param,1.8f);
            default: {
                return changeParamValue(param,1.2f);
            }
        }
    }




    private TEUIProperty.TESDKParam changeParamValue(TEUIProperty.TESDKParam param,float multiple){
        TEUIProperty.TESDKParam resultParam = null;
        try {
            resultParam = param.clone();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (resultParam == null) {
            return param;
        }
        resultParam.effectValue = (int) (multiple * param.effectValue);
        return resultParam;
    }
}
