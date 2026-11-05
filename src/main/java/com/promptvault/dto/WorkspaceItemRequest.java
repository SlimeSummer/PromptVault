package com.promptvault.dto;

/**
 * 编辑区新增/修改条目的请求。
 */
public class WorkspaceItemRequest {

    /** tag / group */
    private String itemType;
    private Long refId;
    /** pos / neg（仅 tag 有效） */
    private String polarity;
    private Double weight;
    /** 手动指定分区；null 表示跟随标签分类 */
    private String sectionOverride;

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
}
