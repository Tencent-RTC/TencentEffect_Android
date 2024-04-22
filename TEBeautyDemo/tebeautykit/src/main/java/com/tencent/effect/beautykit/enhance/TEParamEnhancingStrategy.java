package com.tencent.effect.beautykit.enhance;

import com.tencent.effect.beautykit.model.TEUIProperty;

/**
 * 美颜增强模式的接口定义
 */
public interface TEParamEnhancingStrategy {

    /**
     * 获取开启增强模式的数据
     *
     * @param param
     * @return
     */
    TEUIProperty.TESDKParam enhanceParam(TEUIProperty.TESDKParam param);
}
