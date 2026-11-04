package com.promptvault.dto;

import java.util.List;

/**
 * 设置页保存请求：分类与底模一次性提交。
 */
public class SettingsRequest {

    private List<CategoryOption> tagCategories;
    private List<CategoryOption> groupCategories;
    private List<String> loraBaseModels;

    public List<CategoryOption> getTagCategories() {
        return tagCategories;
    }

    public void setTagCategories(List<CategoryOption> tagCategories) {
        this.tagCategories = tagCategories;
    }

    public List<CategoryOption> getGroupCategories() {
        return groupCategories;
    }

    public void setGroupCategories(List<CategoryOption> groupCategories) {
        this.groupCategories = groupCategories;
    }

    public List<String> getLoraBaseModels() {
        return loraBaseModels;
    }

    public void setLoraBaseModels(List<String> loraBaseModels) {
        this.loraBaseModels = loraBaseModels;
    }
}
