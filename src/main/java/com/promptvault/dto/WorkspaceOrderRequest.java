package com.promptvault.dto;

import java.util.List;

/**
 * 编辑区同分区内拖拽排序：itemIds 的顺序即新的 sort_order。
 */
public class WorkspaceOrderRequest {

    private List<Long> itemIds;

    public List<Long> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<Long> itemIds) {
        this.itemIds = itemIds;
    }
}
