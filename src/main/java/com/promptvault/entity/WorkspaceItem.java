package com.promptvault.entity;

/**
 * 编辑区条目：只记录"插入了什么"，渲染时再展开成具体标签。
 * itemType = tag / group；正负向由条目自身携带。
 */
public class WorkspaceItem {

    private Long id;
    private Long workspaceId;
    private String itemType;
    private Long refId;
    private String polarity;
    private Double weight;
    private String sectionOverride;
    private Integer sortOrder;
    private String createdAt;

    /** 联查字段（itemType = tag 时有效） */
    private String prompt;
    private String nameZh;
    private String category;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public String getPolarity() {
        return polarity;
    }

    public void setPolarity(String polarity) {
        this.polarity = polarity;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public String getSectionOverride() {
        return sectionOverride;
    }

    public void setSectionOverride(String sectionOverride) {
        this.sectionOverride = sectionOverride;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getNameZh() {
        return nameZh;
    }

    public void setNameZh(String nameZh) {
        this.nameZh = nameZh;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
