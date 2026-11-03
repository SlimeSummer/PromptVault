package com.promptvault.dto;

import java.util.List;

/**
 * 预设条目排序请求：按数组顺序重排某个预设内的条目。
 */
public class PresetItemOrderRequest {

    private List<Long> itemIds;

    public List<Long> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<Long> itemIds) {
        this.itemIds = itemIds;
    }
}
