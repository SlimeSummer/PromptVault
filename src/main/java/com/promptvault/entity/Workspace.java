package com.promptvault.entity;

/**
 * 编辑区：一次组装上下文。绑定一个 LoRA / 预设，条目按分区动态展开。
 */
public class Workspace {

    private Long id;
    private String name;
    private Long loraId;
    private Long presetId;
    private String note;
    private String createdAt;
    private String updatedAt;

    /** 列表联查字段 */
    private String loraName;
    private String presetName;
    private Long itemCount;

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

    public Long getLoraId() {
        return loraId;
    }

    public void setLoraId(Long loraId) {
        this.loraId = loraId;
    }

    public Long getPresetId() {
        return presetId;
    }

    public void setPresetId(Long presetId) {
        this.presetId = presetId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    public String getLoraName() {
        return loraName;
    }

    public void setLoraName(String loraName) {
        this.loraName = loraName;
    }

    public String getPresetName() {
        return presetName;
    }

    public void setPresetName(String presetName) {
        this.presetName = presetName;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }
}
