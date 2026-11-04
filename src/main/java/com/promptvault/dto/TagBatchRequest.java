package com.promptvault.dto;

/**
 * 批量导入请求：一段文本，按逗号/换行拆分。
 */
public class TagBatchRequest {

    private String text;
    /** autoCategory=false 时必填；autoCategory=true 时忽略 */
    private String category;
    private Long loraId;
    /** true 时根据关键词自动判断每条词条的分类，category 字段被忽略 */
    private Boolean autoCategory = false;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getLoraId() {
        return loraId;
    }

    public void setLoraId(Long loraId) {
        this.loraId = loraId;
    }

    public Boolean getAutoCategory() {
        return autoCategory;
    }

    public void setAutoCategory(Boolean autoCategory) {
        this.autoCategory = autoCategory;
    }
}
