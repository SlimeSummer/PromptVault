package com.promptvault.web;

import com.promptvault.common.ApiResponse;
import com.promptvault.dto.GroupItemOrderRequest;
import com.promptvault.entity.TagGroup;
import com.promptvault.entity.TagGroupItem;
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

    public TagGroupController(TagGroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ApiResponse<List<TagGroup>> list(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) Long loraId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(groupService.list(scope, loraId, keyword));
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
}
