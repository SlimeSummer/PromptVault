package com.promptvault.dto;

public class GroupRefRequest {
    private Long refGroupId;
    private Integer priority;
    private Integer sortOrder;

    public Long getRefGroupId() { return refGroupId; }
    public void setRefGroupId(Long refGroupId) { this.refGroupId = refGroupId; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
