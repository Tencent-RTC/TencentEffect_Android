package com.tencent.effect.beautykit.config;

import com.tencent.effect.beautykit.model.TEUIProperty;
import com.tencent.effect.beautykit.model.TEUIProperty.UICategory;
import com.tencent.effect.beautykit.utils.provider.ProviderUtils;

import java.util.EnumSet;
import java.util.List;


/**
 * 腾讯特效套餐枚举类
 * 定义了腾讯特效SDK套餐类型。
 * <p>
 * 每个套餐通过重写 {@link #applyPlanRule(TEUIProperty)} 声明本套餐对
 * {@link TEUIProperty} 的过滤规则；外部只调用统一入口 {@link #filter(TEUIProperty)}。
 * <p>
 * 阅读代码时约定：{@link #ALWAYS_KEEP} 中的三项（模板 / 美颜 / 滤镜）
 * 已在 {@link #filter(TEUIProperty)} 入口前置放行，各枚举无需再重复处理。
 */
public enum EffectPlan {

    /**
     * 仅保留通用三项（模板 / 美颜 / 滤镜），其余全部丢弃。
     */
    A1_00 {
        @Override
        protected TEUIProperty applyPlanRule(TEUIProperty property) {
            // 通用三项已在 filter() 前置放行，这里其余分类全部丢弃
            return null;
        }
    },

    /**
     * 保留 BEAUTY_IMAGE / BEAUTY_SHAPE / BEAUTY_MAKEUP，均改写为 BEAUTY。
     */
    A1_01 {
        @Override
        protected TEUIProperty applyPlanRule(TEUIProperty property) {
            switch (property.uiCategory) {
                case BEAUTY_IMAGE:
                case BEAUTY_SHAPE:
                    return property;
                default:
                    return null;
            }
        }
    },

    /**
     * 保留美颜三项（改写为 BEAUTY）+ MOTION（子项裁掉 MAKEUP、SEGMENTATION）。
     */
    A1_02 {
        @Override
        protected TEUIProperty applyPlanRule(TEUIProperty property) {
            switch (property.uiCategory) {
                case BEAUTY_IMAGE:
                case BEAUTY_SHAPE:
                    return property;
                case MOTION:
                    ProviderUtils.removeChildrenByCategory(property, UICategory.MAKEUP, UICategory.SEGMENTATION);
                    return property;
                default:
                    return null;
            }
        }
    },

    /**
     * 保留美颜三项（改写为 BEAUTY）+ MOTION（子项裁掉 MAKEUP、SEGMENTATION）。
     */
    A1_03 {
        @Override
        protected TEUIProperty applyPlanRule(TEUIProperty property) {
            switch (property.uiCategory) {
                case BEAUTY_IMAGE:
                case BEAUTY_SHAPE:
                    return property;
                case MOTION:
                    ProviderUtils.removeChildrenByCategory(property, UICategory.MAKEUP, UICategory.SEGMENTATION);
                    return property;
                default:
                    return null;
            }
        }
    },

    /**
     * 保留 BEAUTY_IMAGE / BEAUTY_SHAPE / BEAUTY_MAKEUP，均改写为 BEAUTY。
     */
    A1_04 {
        @Override
        protected TEUIProperty applyPlanRule(TEUIProperty property) {
            switch (property.uiCategory) {
                case BEAUTY_IMAGE:
                case BEAUTY_SHAPE:
                    return property;
                default:
                    return null;
            }
        }
    },

    /**
     * 保留美颜三项（改写为 BEAUTY）+ MOTION（子项裁掉 MAKEUP）。
     */
    A1_05 {
        @Override
        protected TEUIProperty applyPlanRule(TEUIProperty property) {
            switch (property.uiCategory) {
                case BEAUTY_IMAGE:
                case BEAUTY_SHAPE:
                    return property;
                case MOTION:
                    ProviderUtils.removeChildrenByCategory(property, UICategory.MAKEUP);
                    return property;
                default:
                    return null;
            }
        }
    },

    /**
     * 保留美颜三项（改写为 BEAUTY）+ MOTION（子项裁掉 SEGMENTATION）。
     */
    A1_06 {
        @Override
        protected TEUIProperty applyPlanRule(TEUIProperty property) {
            switch (property.uiCategory) {
                case BEAUTY_IMAGE:
                case BEAUTY_SHAPE:
                    return property;
                case MOTION:
                    ProviderUtils.removeChildrenByCategory(property, UICategory.SEGMENTATION);
                    return property;
                default:
                    return null;
            }
        }
    },

    /**
     * 保留 BEAUTY_IMAGE / BEAUTY_SHAPE（改写为 BEAUTY）+ BEAUTY_MAKEUP（保留原分类，
     * 子项裁掉 LIGHT_MAKEUP）。
     */
    S1_00 {
        @Override
        protected TEUIProperty applyPlanRule(TEUIProperty property) {
            switch (property.uiCategory) {
                case BEAUTY_IMAGE:
                case BEAUTY_SHAPE:
                    return property;
                case BEAUTY_MAKEUP:
                    ProviderUtils.removeChildrenByCategory(property, UICategory.LIGHT_MAKEUP);
                    ProviderUtils.removeHairColorGroup(property);
                    return property;
                default:
                    return null;
            }
        }
    },

    /**
     * 黑名单模式：几乎全部保留；丢弃 BODY_BEAUTY；MOTION 子项裁掉 SEGMENTATION。
     */
    S1_01 {
        @Override
        protected TEUIProperty applyPlanRule(TEUIProperty property) {
            if (property.uiCategory == UICategory.BODY_BEAUTY) {
                return null;
            }
            if (property.uiCategory == UICategory.MOTION) {
                ProviderUtils.removeChildrenByCategory(property, UICategory.SEGMENTATION);
            }
            return property;
        }
    },

    /**
     * 黑名单模式：几乎全部保留；丢弃 BODY_BEAUTY；MOTION 子项裁掉 SEGMENTATION。
     */
    S1_02 {
        @Override
        protected TEUIProperty applyPlanRule(TEUIProperty property) {
            if (property.uiCategory == UICategory.BODY_BEAUTY) {
                return null;
            }
            if (property.uiCategory == UICategory.MOTION) {
                ProviderUtils.removeChildrenByCategory(property, UICategory.SEGMENTATION);
            }
            return property;
        }
    },

    /**
     * 黑名单模式：几乎全部保留；仅丢弃 BODY_BEAUTY。
     */
    S1_03 {
        @Override
        protected TEUIProperty applyPlanRule(TEUIProperty property) {
            if (property.uiCategory == UICategory.BODY_BEAUTY) {
                return null;
            }
            return property;
        }
    },

    /**
     * 黑名单模式：几乎全部保留；仅丢弃 BODY_BEAUTY。
     */
    S1_04 {
        @Override
        protected TEUIProperty applyPlanRule(TEUIProperty property) {
            if (property.uiCategory == UICategory.BODY_BEAUTY) {
                return null;
            }
            return property;
        }
    },

    /**
     * 全部保留。
     */
    S1_07 {
        @Override
        protected TEUIProperty applyPlanRule(TEUIProperty property) {
            return property;
        }
    };

    /**
     * 无论何种套餐都直接放行的分类（模板 / 美颜 / 滤镜）。
     * <p>
     * 在 {@link #filter(TEUIProperty)} 入口处做前置放行，因此各枚举常量的
     * {@link #applyPlanRule(TEUIProperty)} 无需再重复处理这三项。
     */
    public static final EnumSet<UICategory> ALWAYS_KEEP =
            EnumSet.of(UICategory.BEAUTY_TEMPLATE, UICategory.BEAUTY, UICategory.LUT);

    /**
     * 按本套餐的规则过滤 {@link TEUIProperty}。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>入参或其 {@code uiCategory} 为空时原样返回；</li>
     *   <li>{@code uiCategory} 命中 {@link #ALWAYS_KEEP} 时直接放行；</li>
     *   <li>其余情况委派给具体枚举常量重写的 {@link #applyPlanRule(TEUIProperty)}；</li>
     *   <li>最后统一做一次"孤儿子标题"展平后处理。</li>
     * </ol>
     *
     * @param property 待过滤的 UI 属性
     * @return 过滤后的对象；返回 {@code null} 表示应被丢弃
     */
    public final TEUIProperty filter(TEUIProperty property) {
        if (property == null || property.uiCategory == null) {
            return property;
        }
        // 通用放行：模板 / 美颜 / 滤镜（所有套餐一视同仁）
        if (ALWAYS_KEEP.contains(property.uiCategory)) {
            return property;
        }
        // 各套餐自身规则
        TEUIProperty result = applyPlanRule(property);
        if (result != null &&
                (result.uiCategory == UICategory.BEAUTY_IMAGE || result.uiCategory == UICategory.BEAUTY_SHAPE ||
                        result.uiCategory == UICategory.BEAUTY_MAKEUP)) {
            property.uiCategory = UICategory.BEAUTY;
        }
        // 通用后处理：展平只剩单个子标题分组的孤儿结构
        flattenSingleSubTitleGroup(result);
        return result;
    }

    /**
     * 由每个枚举常量重写：应用本套餐的具体规则。
     * <p>
     * 调用方 {@link #filter(TEUIProperty)} 已保证：
     * <ol>
     *   <li>{@code property} 与 {@code property.uiCategory} 非空</li>
     *   <li>{@code property.uiCategory} 不属于 {@link #ALWAYS_KEEP}</li>
     * </ol>
     * 默认返回 {@code null}（未配置规则的套餐一律丢弃）。
     */
    protected TEUIProperty applyPlanRule(TEUIProperty property) {
        return null;
    }

    /**
     * 展平"孤儿子标题分组"。
     * <p>
     * 当节点原本按"子标题分组"组织成两级结构（例如 BEAUTY_MAKEUP → [眼妆分组 → 眼影/眼线/...]），
     * 若套餐规则裁剪后只剩下唯一一个子标题分组，这个分组标题就成了多余的空壳，
     * 会在 UI 上多出一个无意义的分组头。此时把它的孩子提升上来，压平成一级。
     * <pre>
     *   before: parent(hasSubTitle) → [group] → [A, B, C]
     *   after : parent(hasSubTitle) → [A, B, C]
     * </pre>
     * 满足以下所有条件时执行展平：
     * <ol>
     *   <li>{@code parent} 非空且标记了 {@link TEUIProperty#hasSubTitle}</li>
     *   <li>{@code parent.propertyList} 恰好只有一个孩子（唯一分组）</li>
     *   <li>该唯一分组自身的 {@code propertyList} 非空</li>
     * </ol>
     */
    private static void flattenSingleSubTitleGroup(TEUIProperty parent) {
        if (parent == null || !parent.hasSubTitle) {
            return;
        }
        if (parent.propertyList == null || parent.propertyList.size() != 1) {
            return;
        }
        List<TEUIProperty> grandChildren = parent.propertyList.get(0).propertyList;
        if (grandChildren == null || grandChildren.isEmpty()) {
            return;
        }
        parent.propertyList.clear();
        parent.propertyList.addAll(grandChildren);
        parent.hasSubTitle = false;
    }

}
