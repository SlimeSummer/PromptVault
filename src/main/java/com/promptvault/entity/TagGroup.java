package com.promptvault.entity;

/**
 * 词组：可被整块引用的经验组合（例如"赛博朋克夜街"、"角色全套穿搭+画风锚点"）。
 * 支持 global / lora 两种 scope，kind 预留 global_negative 用于全局负面词层。
 */
public class TagGroup {

    private Long id;
    private String name;
    /** normal / global_negative */
    private String kind;
    /** global / lora */
    private String scope;
    private Long loraId;
    /** 词组分类，取值由 setting.group.categories 驱动 */
    private String category;
    private String note;
    private Integer sortOrder;
    private String createdAt;
    private String updatedAt;

    /** 列表联查字段 */
    private Long itemCount;
    private String loraName;
    /** 被引用的次数（列表/详情展示用） */
    private Long incomingRefCount;

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

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Long getLoraId() {
        return loraId;
    }

    public void setLoraId(Long loraId) {
        this.loraId = loraId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }

    public String getLoraName() {
        return loraName;
    }

    public void setLoraName(String loraName) {
        this.loraName = loraName;
    }

    public Long getIncomingRefCount() {
        return incomingRefCount;
    }

    public void setIncomingRefCount(Long incomingRefCount) {
        this.incomingRefCount = incomingRefCount;
    }
}
