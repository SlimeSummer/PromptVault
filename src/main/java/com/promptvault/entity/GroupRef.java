package com.promptvault.entity;

/**
 * 词组引用关系：group_id 引用 ref_group_id
 * 优先级 priority 数字越小越高，同优先级按 sort_order 排序
 */
public class GroupRef {
    private Long id;
    private Long groupId;
    private Long refGroupId;
    /** 优先级：0 最高 */
    private Integer priority;
    /** 同优先级下的顺序 */
    private Integer sortOrder;

    /** 联查字段：被引用的组的名称 */
    private String refGroupName;
    /** 联查字段：被引用的组的条目数 */
    private Long refItemCount;
    /** 联查字段：被引用的组的 scope */
    private String refScope;
    private String refLoraName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getRefGroupId() { return refGroupId; }
    public void setRefGroupId(Long refGroupId) { this.refGroupId = refGroupId; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getRefGroupName() { return refGroupName; }
    public void setRefGroupName(String refGroupName) { this.refGroupName = refGroupName; }
    public Long getRefItemCount() { return refItemCount; }
    public void setRefItemCount(Long refItemCount) { this.refItemCount = refItemCount; }
    public String getRefScope() { return refScope; }
    public void setRefScope(String refScope) { this.refScope = refScope; }
    public String getRefLoraName() { return refLoraName; }
    public void setRefLoraName(String refLoraName) { this.refLoraName = refLoraName; }
}
