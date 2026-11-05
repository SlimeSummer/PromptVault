package com.promptvault.web;

import com.promptvault.common.ApiResponse;
import com.promptvault.dto.WorkspaceItemRequest;
import com.promptvault.dto.WorkspaceOrderRequest;
import com.promptvault.entity.Workspace;
import com.promptvault.entity.WorkspaceItem;
import com.promptvault.service.WorkspaceService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public ApiResponse<List<Workspace>> list() {
        return ApiResponse.ok(workspaceService.list());
    }

    @PostMapping
    public ApiResponse<Workspace> create(@RequestBody Workspace workspace) {
        return ApiResponse.ok(workspaceService.create(workspace));
    }

    /** 详情即渲染结果：动态分区 + 负向层 + 拼接文本 */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(workspaceService.render(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Workspace> update(@PathVariable Long id, @RequestBody Workspace workspace) {
        return ApiResponse.ok(workspaceService.update(id, workspace));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        workspaceService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/items")
    public ApiResponse<WorkspaceItem> addItem(@PathVariable Long id,
                                              @RequestBody WorkspaceItemRequest request) {
        return ApiResponse.ok(workspaceService.addItem(id, request));
    }

    @PutMapping("/{id}/items/{itemId}")
    public ApiResponse<WorkspaceItem> updateItem(@PathVariable Long id,
                                                 @PathVariable Long itemId,
                                                 @RequestBody WorkspaceItemRequest request) {
        return ApiResponse.ok(workspaceService.updateItem(id, itemId, request));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        workspaceService.deleteItem(id, itemId);
        return ApiResponse.ok(null);
    }

    /** 同分区内拖拽排序 */
    @PutMapping("/{id}/items/order")
    public ApiResponse<Void> reorder(@PathVariable Long id,
                                     @RequestBody WorkspaceOrderRequest request) {
        workspaceService.reorder(id, request);
        return ApiResponse.ok(null);
    }

    /** 解绑词组：拆成独立词条，之后可单独调权重 / 移动 */
    @PostMapping("/{id}/items/{itemId}/unbind")
    public ApiResponse<Map<String, Object>> unbind(@PathVariable Long id,
                                                   @PathVariable Long itemId) {
        return ApiResponse.ok(workspaceService.unbind(id, itemId));
    }
}
