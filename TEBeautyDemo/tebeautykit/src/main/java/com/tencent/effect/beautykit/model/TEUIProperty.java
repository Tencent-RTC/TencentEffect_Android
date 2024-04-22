package com.tencent.effect.beautykit.model;

import android.text.TextUtils;
import android.util.ArrayMap;


import com.tencent.xmagic.XmagicConstant;

import java.util.List;
import java.util.Map;

public class TEUIProperty {



    public String displayName;  //中文名称
    public String displayNameEn; //英文名称
    public String icon;   //图标
    public String resourceUri;  //如果此资源是本地集成，则在这里配置本地资源路径，如果是网络下载，那么这里配置资源的下载地址
    public String downloadPath; //对于网络下载的资源，在本地保存的文件夹路径

    public List<TEUIProperty> propertyList;
    public TESDKParam sdkParam;
    public List<TESDKParam> paramList;   //在美颜模版中使用

    public TEUIProperty parentUIProperty;
    public UICategory uiCategory;
    public TEMotionDLModel dlModel = null;


    private int uiState = 0;



    public int getUiState() {
        return uiState;
    }

    public void setUiState(int uiState) {
        this.uiState = uiState;
    }

    public static class UIState {
        public static int CHECKED_AND_IN_USE = 2; //正在生效，并且UI上是选中的     CHECKED_IN_USE
        public static int IN_USE = 1;   //正在生效的美颜属性，但是UI上是没有选中的，   IN_USE
        public static int INIT = 0;   //未选中状态
    }

    public enum UICategory {
        BEAUTY,
        BODY_BEAUTY,
        LUT,
        MOTION,
        MAKEUP,
        BEAUTY_TEMPLATE,
        SEGMENTATION,
    }


    public static class TESDKParam implements Cloneable {

        //图片类型数据
        public static final String EXTRA_INFO_BG_TYPE_IMG = "0";
        //视频类型数据
        public static final String EXTRA_INFO_BG_TYPE_VIDEO = "1";
        //seg_type的值，如果是自定义分割就是用：EXTRA_INFO_SEG_TYPE_CUSTOM ，绿幕就使用 EXTRA_INFO_SEG_TYPE_GREEN
        public static final String EXTRA_INFO_SEG_TYPE_GREEN = "green_background";
        public static final String EXTRA_INFO_SEG_TYPE_CUSTOM = "custom_background";

        public static final String EXTRA_INFO_KEY_BG_TYPE = "bgType";
        public static final String EXTRA_INFO_KEY_BG_PATH = "bgPath";
        public static final String EXTRA_INFO_KEY_SEG_TYPE = "segType";
        public static final String EXTRA_INFO_KEY_KEY_COLOR = "keyColor";
        public static final String EXTRA_INFO_KEY_MERGE_WITH_CURRENT_MOTION = "mergeWithCurrentMotion";

        public static final String EXTRA_INFO_KEY_LUT_STRENGTH = "makeupLutStrength";



        public String effectName;
        public int effectValue = 0;
        public String resourcePath;
        public Map<String, String> extraInfo;

        @Override
        public TESDKParam clone() throws CloneNotSupportedException {
            return (TESDKParam) super.clone();
        }

        @Override
        public String toString() {
            return "TEParam{" +
                    "effectName='" + effectName + '\'' +
                    ", effectValue=" + effectValue +
                    ", resourcePath='" + resourcePath + '\'' +
                    ", extraInfo=" + extraInfo +
                    '}';
        }



    }


    public  boolean isNoneItem() {
        return (this.sdkParam == null && propertyList == null && paramList == null);
    }

    /**
     * 检测是不是美颜中特殊的 none
     * @return
     */
    public boolean isBeautyMakeupNoneItem() {
        if (this.sdkParam != null && TextUtils.isEmpty(this.sdkParam.resourcePath)) {
            switch (this.sdkParam.effectName) {
                case XmagicConstant.EffectName.BEAUTY_MOUTH_LIPSTICK: //口红
                case XmagicConstant.EffectName.BEAUTY_FACE_SOFTLIGHT:  //立体
                case XmagicConstant.EffectName.BEAUTY_FACE_RED_CHEEK:  //腮红
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYE_SHADOW:  //眼影
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYE_LINER:  //眼线
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYELASH://睫毛
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYE_SEQUINS:  //眼影亮片
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYEBROW: //眉毛
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYEBALL:  //美瞳
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }


    public enum EffectValueType {
        RANGE_0_0(0, 0),
        RANGE_0_POS100(0, 100),
        RANGE_NEG100_POS100(-100, 100);
        int min;
        int max;

        EffectValueType(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }
    }

    /**
     * 用于存储特效属性对应的取值变化的map
     */
    private static final Map<String, EffectValueType> VALUE_TYPE_MAP = new ArrayMap<>();


    static {
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.EFFECT_MOTION, EffectValueType.RANGE_0_0);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.EFFECT_SEGMENTATION, EffectValueType.RANGE_0_0);

        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_CONTRAST, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_SATURATION, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_IMAGE_WARMTH, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_IMAGE_TINT, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYE_DISTANCE, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYE_ANGLE, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYE_WIDTH, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYE_HEIGHT, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYEBROW_ANGLE, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYEBROW_DISTANCE, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYEBROW_HEIGHT, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYEBROW_LENGTH, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYEBROW_THICKNESS, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYEBROW_RIDGE, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_NOSE_WING, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_NOSE_HEIGHT, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_NOSE_BRIDGE_WIDTH, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_NASION, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_MOUTH_SIZE, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_MOUTH_HEIGHT, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_MOUTH_WIDTH, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_MOUTH_POSITION, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_SMILE_FACE, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_FACE_THIN_CHIN, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_FACE_FOREHEAD, EffectValueType.RANGE_NEG100_POS100);

        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BODY_ENLARGE_CHEST_STRENGTH, EffectValueType.RANGE_NEG100_POS100);  //胸部调整
    }

    public static EffectValueType getEffectValueType(TESDKParam teParam) {
        EffectValueType type = VALUE_TYPE_MAP.get(teParam.effectName);
        if (type != null) {
            return type;
        } else {
            return EffectValueType.RANGE_0_POS100;
        }
    }


}
