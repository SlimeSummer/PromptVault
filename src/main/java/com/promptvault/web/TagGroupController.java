package com.promptvault.web;

import com.promptvault.common.ApiResponse;
import com.promptvault.dto.CategoryOption;
import com.promptvault.dto.GroupImportRequest;
import com.promptvault.dto.GroupItemOrderRequest;
import com.promptvault.dto.GroupRefRequest;
import com.promptvault.entity.GroupRef;
import com.promptvault.entity.TagGroup;
import com.promptvault.entity.TagGroupItem;
import com.promptvault.service.SettingService;
import com.promptvault.service.TagGroupService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class TagGroupController {

    private final TagGroupService groupService;
    private final SettingService settingService;

    public TagGroupController(TagGroupService groupService, SettingService settingService) {
        this.groupService = groupService;
        this.settingService = settingService;
    }

    @GetMapping
    public ApiResponse<List<TagGroup>> list(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) Long loraId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(groupService.list(scope, loraId, keyword, category));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryOption>> categories() {
        return ApiResponse.ok(settingService.getGroupCategories());
    }

    @GetMapping("/suggest")
    public ApiResponse<List<String>> suggest(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ApiResponse.ok(groupService.suggest(keyword, limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(groupService.detail(id));
    }

    @PostMapping
    public ApiResponse<TagGroup> create(@RequestBody TagGroup group) {
        return ApiResponse.ok(groupService.create(group));
    }

    @PutMapping("/{id}")
    public ApiResponse<TagGroup> update(@PathVariable Long id, @RequestBody TagGroup group) {
        return ApiResponse.ok(groupService.update(id, group));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        groupService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/duplicate")
    public ApiResponse<TagGroup> duplicate(@PathVariable Long id) {
        return ApiResponse.ok(groupService.duplicate(id));
    }

    // 条目管理

    @PostMapping("/{id}/items")
    public ApiResponse<TagGroupItem> addItem(@PathVariable Long id, @RequestBody TagGroupItem item) {
        return ApiResponse.ok(groupService.addItem(id, item));
    }

    @PutMapping("/{id}/items/{itemId}")
    public ApiResponse<TagGroupItem> updateItem(@PathVariable Long id,
                                                 @PathVariable Long itemId,
                                                 @RequestBody TagGroupItem item) {
        return ApiResponse.ok(groupService.updateItem(id, itemId, item));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        groupService.deleteItem(id, itemId);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/items/order")
    public ApiResponse<Void> reorder(@PathVariable Long id, @RequestBody GroupItemOrderRequest request) {
        groupService.reorder(id, request);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/items/import")
    public ApiResponse<Map<String, Object>> importItems(@PathVariable Long id,
                                                        @RequestBody GroupImportRequest request) {
        if (request == null || request.getSourceGroupId() == null) {
            throw new com.promptvault.common.BizException("请选择要导入的词组");
        }
        return ApiResponse.ok(groupService.importItems(id, request.getSourceGroupId()));
    }

    // --- 引用关系管理 ---

    @PostMapping("/{id}/refs")
    public ApiResponse<GroupRef> addRef(@PathVariable Long id, @RequestBody GroupRefRequest request) {
        if (request == null || request.getRefGroupId() == null) {
            throw new com.promptvault.common.BizException("请选择要引用的词组");
        }
        return ApiResponse.ok(groupService.addRef(id, request.getRefGroupId(), request.getPriority(), request.getSortOrder()));
    }

    @PutMapping("/{id}/refs/{refId}")
    public ApiResponse<Void> updateRef(@PathVariable Long id,
                                       @PathVariable Long refId,
                                       @RequestBody GroupRefRequest request) {
        groupService.updateRef(id, refId, request != null ? request.getPriority() : null,
                request != null ? request.getSortOrder() : null);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}/refs/{refId}")
    public ApiResponse<Void> removeRef(@PathVariable Long id, @PathVariable Long refId) {
        groupService.removeRef(id, refId);
        return ApiResponse.ok();
    }
}
