package com.promptvault.entity;

/**
 * LoRA 模型档案。
 */
public class Lora {

    private Long id;
    private String name;
    private String source;
    private String baseModel;
    private String triggerWords;
    private String note;
    private String coverPath;
    private String createdAt;
    private String updatedAt;
    private Long presetCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getBaseModel() {
        return baseModel;
    }

    public void setBaseModel(String baseModel) {
        this.baseModel = baseModel;
    }

    public String getTriggerWords() {
        return triggerWords;
    }

    public void setTriggerWords(String triggerWords) {
        this.triggerWords = triggerWords;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getPresetCount() {
        return presetCount;
    }

    public void setPresetCount(Long presetCount) {
        this.presetCount = presetCount;
    }
}
