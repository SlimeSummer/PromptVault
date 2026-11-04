package com.promptvault.dto;

import java.util.List;

public class GroupItemOrderRequest {

    /** 同一词组内的条目 id 列表，顺序即新 sort_order */
    private List<Long> itemIds;

    public List<Long> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<Long> itemIds) {
        this.itemIds = itemIds;
    }
}
