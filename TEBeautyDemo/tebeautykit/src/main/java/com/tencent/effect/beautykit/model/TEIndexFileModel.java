package com.tencent.effect.beautykit.model;

import java.util.List;
import java.util.Map;


public class TEIndexFileModel {


    /**
     * 默认展示语言，例如 "zh" / "en" / "ja"
     */
    public String defaultLanguage;

    /**
     * 面板项列表
     */
    public List<Item> data;

    public TEIndexFileModel() {
    }

    public TEIndexFileModel(String defaultLanguage, List<Item> data) {
        this.defaultLanguage = defaultLanguage;
        this.data = data;
    }

    /**
     * 面板项，对应 data 数组中的每一个元素
     */
    public static class Item {

        /**
         * 多语言名称，key 为语言代码，value 为显示名。
         * 例如：{"zh":"美颜","en":"Beauty","ja":"美顔"}
         */
        public Map<String, String> name;

        /**
         * 该面板项包含的 json 配置文件列表，路径相对于 index.json 所在目录。
         */
        public List<String> config;

        public Item() {
        }

        public Item(Map<String, String> name, List<String> config) {
            this.name = name;
            this.config = config;
        }


    }


    public static class DataSetItem {
        public List<String> effectPlanSet;
        public String config;
    }


}
