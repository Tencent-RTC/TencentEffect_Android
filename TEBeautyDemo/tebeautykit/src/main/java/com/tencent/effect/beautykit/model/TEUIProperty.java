package com.tencent.effect.beautykit.model;

import android.text.TextUtils;
import android.util.ArrayMap;


import com.tencent.xmagic.XmagicConstant;

import java.util.List;
import java.util.Map;

public class TEUIProperty {


    public int id = -1;    //美颜模板数据中的id，用于区分美颜模板
    @Deprecated
    public String displayName;
    @Deprecated
    public String displayNameEn;
    public String icon;
    public String resourceUri; // If this resource is locally integrated, configure the local resource path here. If it is downloaded from the network, configure the download address of the resource here.
    public String downloadPath; // For resources downloaded from the network, the local folder path where they are saved.

    public List<TEUIProperty> propertyList;
    public TESDKParam sdkParam;
    public List<TESDKParam> paramList;

    /**
     * 轻美妆场景下，单项（口红/腮红/眼影/滤镜等）的展示信息映射表。
     * key 为 makeup.json 中的 effectName（例如 "beauty.faceFeatureLipsLut"、"lut"），
     * value 为对应的 UI 展示信息（name/icon）。
     * 由 beauty_makeup.json 的"轻美妆"节点顶层 itemDisplayMap 反序列化而来，其它场景无需使用。
     */
    public Map<String, LightMakeupItemDisplay> itemDisplayMap;

    /**
     * 父节点回链，仅在运行时构建，用于向上追溯层级关系（下载路径、UI 状态传播等）。
     * <p>
     * 使用 {@code transient} 排除 Gson 序列化：Gson 默认会跳过 transient 字段，
     * 从而避免"子 → 父 → 子"的无限递归导致 {@link StackOverflowError}。
     */
    public transient TEUIProperty parentUIProperty;
    public UICategory uiCategory;
    public String titleType;
    public TEMotionDLModel dlModel = null;
    public boolean verticalLayout = false;
    public boolean hasSubTitle = false;

    private int uiState = 0;


    public Map<String,String> name;
    public List<TEIndexFileModel.DataSetItem> dataList;

    public int getUiState() {
        return uiState;
    }

    public void setUiState(int uiState) {
        this.uiState = uiState;
    }

    public static class UIState {
        public static int CHECKED_AND_IN_USE = 2;
        public static int IN_USE = 1;
        public static int INIT = 0;
    }

    public enum UICategory {
        BEAUTY,
        BEAUTY_IMAGE,
        BEAUTY_SHAPE,
        BODY_BEAUTY,
        BEAUTY_MAKEUP,  //
        LUT,
        MOTION,
        MAKEUP,
        LIGHT_MAKEUP,
        LIGHT_MAKEUP_ITEM,   //轻美妆下钻态中的单项（口红/腮红/眼影/滤镜等）
        LIGHT_MAKEUP_BACK,   //轻美妆下钻态列表的"返回"占位项（兜底）
        BEAUTY_TEMPLATE,
        SEGMENTATION,
        SHAPESET,
        GREEN_BACKGROUND_V2_ITEM,   //绿幕子项
        GREEN_BACKGROUND_V2_ITEM_IMPORT_IMAGE, //绿幕子项中的导入图片项
    }


    /**
     * 轻美妆单项的展示信息。
     * 从 beauty_makeup.json "轻美妆"节点顶层的 itemDisplayMap 节点反序列化而来。
     */
    public static class LightMakeupItemDisplay {
        public Map<String, String> name;   // 多语言名称，key 为语言代码，如 {zh, en, ja}
        public String icon;
    }


    public static class TESDKParam implements Cloneable {


        public static final String BEAUTY_TEMPLATE_EFFECT_NAME = "BEAUTY_TEMPLATE";

        public static final String EXTRA_INFO_BG_TYPE_IMG = "0";
        public static final String EXTRA_INFO_BG_TYPE_VIDEO = "1";
        // The value of seg_type, if it is a custom segmentation, use: EXTRA_INFO_SEG_TYPE_CUSTOM, for green screen, use EXTRA_INFO_SEG_TYPE_GREEN.
        public static final String [] EXTRA_INFO_SEG_TYPE_GREEN = {"green_background","green_background_v2"};
        public static final String EXTRA_INFO_SEG_TYPE_CUSTOM = "custom_background";
        public static final String GREEN_PARAMS_V2 = "green_params_v2";

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
        
        public transient Object tag;

        @Override
        public TESDKParam clone() throws CloneNotSupportedException {
            return (TESDKParam) super.clone();
        }

        @Override
        public String toString() {
            return "TESDKParam{" +
                    "effectName='" + effectName + '\'' +
                    ", effectValue=" + effectValue +
                    ", resourcePath='" + resourcePath + '\'' +
                    ", extraInfo=" + extraInfo +
                    ", tag=" + tag +
                    '}';
        }
    }


    public  boolean isNoneItem() {
        return (this.sdkParam == null && propertyList == null && paramList == null);
    }

    /**
     * 当前节点是否是一级主 tab 节点（即面板顶部 mainTabLayout 承载的节点）。
     *
     * @return 无父节点时返回 true
     */
    public boolean isMainTabNode() {
        return this.parentUIProperty == null;
    }

    /**
     * 当前节点是否是二级标题节点（即面板 subTabLayout 承载的节点）。
     * <p>
     * 注意 {@link #hasSubTitle} 的语义是"该节点的<b>孩子</b>是二级标题"，
     * 因此判断"自己是不是二级标题"需要看父节点的该标记。
     *
     * @return 父节点存在且父节点标记了 hasSubTitle 时返回 true
     */
    public boolean isSubTitleNode() {
        return this.parentUIProperty != null && this.parentUIProperty.hasSubTitle;
    }

    /**
     * 用于判断是否是 绿幕2中的导入图片的item
     *
     * @return
     */
    public boolean isGSV2ImportImageItem() {
        return (uiCategory == UICategory.GREEN_BACKGROUND_V2_ITEM_IMPORT_IMAGE);
    }


    public boolean isBeautyMakeupNoneItem() {
        // 轻美妆下钻态单项（LIGHT_MAKEUP_ITEM）复用单点美妆的 effectName 且 resourcePath 为空，
        // 但它不是"美妆无效果占位项"，需排除，否则会被 setSeekBarState 误判而隐藏滑竿。
        if (this.uiCategory == UICategory.LIGHT_MAKEUP_ITEM) {
            return false;
        }
        if (this.sdkParam != null && TextUtils.isEmpty(this.sdkParam.resourcePath)) {
            switch (this.sdkParam.effectName) {
                case XmagicConstant.EffectName.BEAUTY_MOUTH_LIPSTICK:
                case XmagicConstant.EffectName.BEAUTY_FACE_SOFTLIGHT:
                case XmagicConstant.EffectName.BEAUTY_FACE_RED_CHEEK:
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYE_SHADOW:
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYE_LINER:
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYELASH:
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYE_SEQUINS:
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYEBROW:
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYEBALL:
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYELIDS:
                case XmagicConstant.EffectName.BEAUTY_FACE_MAKEUP_EYEWOCAN:
                case XmagicConstant.EffectName.BEAUTY_HAIR_COLOR_LUT:
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
        RANGE_NEG100_POS100(-100, 100),
        RANG_1_POS100(1,100),
        RANG_0_POS3(0,3),
        RANG_0_POS5(0,5);
        
        
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


    private static final Map<String, EffectValueType> VALUE_TYPE_MAP = new ArrayMap<>();


    static {
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.EFFECT_MOTION, EffectValueType.RANGE_0_0);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.EFFECT_SEGMENTATION, EffectValueType.RANGE_0_0);

        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_CONTRAST, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_SATURATION, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_IMAGE_WARMTH, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_IMAGE_TINT, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_IMAGE_BRIGHTNESS, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYE_DISTANCE, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYE_ANGLE, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYE_WIDTH, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYE_HEIGHT, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYE_OUT_CORNER, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_EYE_POSITION, EffectValueType.RANGE_NEG100_POS100);
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
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BEAUTY_FACE_FOREHEAD2, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BODY_ENLARGE_CHEST_STRENGTH, EffectValueType.RANGE_NEG100_POS100);
        VALUE_TYPE_MAP.put(XmagicConstant.EffectName.BODY_SLIM_ARM_STRENGTH, EffectValueType.RANGE_NEG100_POS100);

        //新版绿幕 子属性的调节范围
        VALUE_TYPE_MAP.put(GreenBackgroundItemName.BACKGROUND_V2_SIMILARITY, EffectValueType.RANGE_0_POS100);
        VALUE_TYPE_MAP.put(GreenBackgroundItemName.BACKGROUND_V2_SMOOTH, EffectValueType.RANG_1_POS100);
        VALUE_TYPE_MAP.put(GreenBackgroundItemName.BACKGROUND_V2_CORROSION, EffectValueType.RANG_0_POS3);
        VALUE_TYPE_MAP.put(GreenBackgroundItemName.BACKGROUND_V2_DE_SPILL, EffectValueType.RANGE_0_POS100);
        VALUE_TYPE_MAP.put(GreenBackgroundItemName.BACKGROUND_V2_DE_SHADOW, EffectValueType.RANG_0_POS5);
    }

    public static EffectValueType getEffectValueType(TESDKParam teParam) {
        EffectValueType type = VALUE_TYPE_MAP.get(teParam.effectName);
        if (type != null) {
            return type;
        } else {
            return EffectValueType.RANGE_0_POS100;
        }
    }


    public static class GreenBackgroundItemName {
        public static final String BACKGROUND_V2_SIMILARITY = "green_background_v2.similarity";
        public static final String BACKGROUND_V2_SMOOTH = "green_background_v2.smooth";
        public static final String BACKGROUND_V2_CORROSION = "green_background_v2.corrosion";
        public static final String BACKGROUND_V2_DE_SPILL = "green_background_v2.despill";
        public static final String BACKGROUND_V2_DE_SHADOW = "green_background_v2.deshadow";
    }
    
    

}
