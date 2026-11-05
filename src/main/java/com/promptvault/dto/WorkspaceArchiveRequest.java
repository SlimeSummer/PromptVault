package com.promptvault.dto;

/**
 * 保存存档 / 读取存档的请求。
 */
public class WorkspaceArchiveRequest {

    /** 保存时必填；读取时用于指定目标编辑区 */
    private Long workspaceId;
    private String name;
    private String note;

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
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
}
