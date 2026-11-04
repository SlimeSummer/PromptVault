package com.promptvault.dto;

import java.util.List;

/** 批量删除词条请求：稳定 JSON body 契约，避免 path/query 切换。 */
public class IdsRequest {

    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
