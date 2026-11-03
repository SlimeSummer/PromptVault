package com.promptvault.dto;

/**
 * 批量导入请求：一段文本，按逗号/换行拆分。
 */
public class TagBatchRequest {

    private String text;
    private String category;
    private Long loraId;

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
}
