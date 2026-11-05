package com.promptvault.entity;

/**
 * 编辑区存档：一条独立的配置快照，不占用编辑区下拉框。
 * payload 是 JSON 文本，包含 workspace 配置、条目引用与导出文本快照。
 */
public class WorkspaceArchive {

    private Long id;
    private String name;
    private String note;
    private Long sourceWorkspaceId;
    private String sourceWorkspaceName;
    private Long itemCount;
    private String payload;
    private String createdAt;

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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getSourceWorkspaceId() {
        return sourceWorkspaceId;
    }

    public void setSourceWorkspaceId(Long sourceWorkspaceId) {
        this.sourceWorkspaceId = sourceWorkspaceId;
    }

    public String getSourceWorkspaceName() {
        return sourceWorkspaceName;
    }

    public void setSourceWorkspaceName(String sourceWorkspaceName) {
        this.sourceWorkspaceName = sourceWorkspaceName;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
