package com.promptvault.dto;

/**
 * 词条分类选项：value 用于数据存储，label 用于界面展示。
 */
public class CategoryOption {

    private String value;
    private String label;

    public CategoryOption() {
    }

    public CategoryOption(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
