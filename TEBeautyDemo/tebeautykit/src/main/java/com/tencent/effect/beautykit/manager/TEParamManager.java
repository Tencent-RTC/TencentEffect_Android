package com.tencent.effect.beautykit.manager;

import android.text.TextUtils;
import com.tencent.xmagic.XmagicConstant;
import com.tencent.effect.beautykit.model.TEUIProperty;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * 用于对美颜属性TEParam进行管理
 * 在用户将美颜数据设置给SDK的时候，可以也给此类设置一份，
 * 当用户想要获取当前美颜生效的属性时，就可以通过此类的getParams方法进行获取
 *
 * 主要的使用场景是在客户给SDK设置了美颜属性之后，当关闭美颜效果之后，在再次打开美颜的时候，恢复到上次的没美颜效果
 * 使用方法：
 * 1. 在设置美颜美颜属性给SDK的时候，调用此类的putTEParam或者putTEParams方法
 * 2. 在用户关闭美颜效果的时候，可以通过getParams方法获取用户设置过的美颜属性
 * 3. 可以将第二步获取到的数据保存到本地或者内存中
 * 4. 再次使用美颜的时候，将第三步的数据获取到，设置给美颜SDK，就可以恢复之前的效果。
 *
 */
public class TEParamManager {

    private final Map<String, TEUIProperty.TESDKParam> allData = new LinkedHashMap<>();

    /**
     * 添加特效属性
     *
     * @param param
     */
    public void putTEParam(TEUIProperty.TESDKParam param) {
        if (param != null && !TextUtils.isEmpty(param.effectName)) {
            if (!this.isCameraMoveItem(param)) {  //不添加 运镜数据
                allData.put(getKey(param), param);
            }
        }
    }




    /**
     * 添加特效属性列表
     *
     * @param paramList
     */
    public void putTEParams(List<TEUIProperty.TESDKParam> paramList) {
        if (paramList != null && paramList.size() > 0) {
            for (TEUIProperty.TESDKParam teParam : paramList) {
                putTEParam(teParam);
            }
        }
    }


    private String getKey(TEUIProperty.TESDKParam param) {
        //需要对美白和瘦脸  美妆、动效、分割 进行特殊处理
        switch (param.effectName) {
            case XmagicConstant.EffectName.BEAUTY_WHITEN:
            case XmagicConstant.EffectName.BEAUTY_WHITEN_2:
            case XmagicConstant.EffectName.BEAUTY_WHITEN_3:
                return XmagicConstant.EffectName.BEAUTY_WHITEN;
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
     * 判断是否是运镜资源，需要特殊处理
     *
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


    /**
     * 获取所有的设置过的特效属性
     *
     * @return
     */
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
