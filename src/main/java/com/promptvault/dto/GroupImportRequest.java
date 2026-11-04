package com.promptvault.dto;

/** 从另一个词组整块导入条目。 */
public class GroupImportRequest {

    private Long sourceGroupId;

    public Long getSourceGroupId() {
        return sourceGroupId;
    }

    public void setSourceGroupId(Long sourceGroupId) {
        this.sourceGroupId = sourceGroupId;
    }
}
