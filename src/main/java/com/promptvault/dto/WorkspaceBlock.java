package com.promptvault.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 编辑区渲染块：一个 tag / 一个词组 / 绑定的预设，在某个分区里的一段内容。
 */
public class WorkspaceBlock {

    private String key;
    /** tag / group / preset */
    private String itemType;
    /** tag / group 对应的 workspace_item.id；preset 为 null */
    private Long workspaceItemId;
    private Long groupId;
    private String groupName;
    private String section;
    /** 手动分区覆盖；null 表示跟随标签分类 */
    private String sectionOverride;
    private String polarity;
    private String sourceName;
    /** 预设块不可拖动、不可删除 */
    private boolean locked;
    private boolean unbindable;
    private Integer sortOrder;
    private List<Map<String, Object>> tags = new ArrayList<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Long getWorkspaceItemId() {
        return workspaceItemId;
    }

    public void setWorkspaceItemId(Long workspaceItemId) {
        this.workspaceItemId = workspaceItemId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getSectionOverride() {
        return sectionOverride;
    }

    public void setSectionOverride(String sectionOverride) {
        this.sectionOverride = sectionOverride;
    }

    public String getPolarity() {
        return polarity;
    }

    public void setPolarity(String polarity) {
        this.polarity = polarity;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isUnbindable() {
        return unbindable;
    }

    public void setUnbindable(boolean unbindable) {
        this.unbindable = unbindable;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<Map<String, Object>> getTags() {
        return tags;
    }

    public void setTags(List<Map<String, Object>> tags) {
        this.tags = tags;
    }
}
