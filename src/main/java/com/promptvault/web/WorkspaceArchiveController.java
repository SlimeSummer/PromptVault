package com.promptvault.web;

import com.promptvault.common.ApiResponse;
import com.promptvault.dto.WorkspaceArchiveRequest;
import com.promptvault.entity.WorkspaceArchive;
import com.promptvault.service.WorkspaceArchiveService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 编辑区存档：独立于 workspace 列表，支持保存、读取、导入、导出、删除。
 */
@RestController
@RequestMapping("/api/archives")
public class WorkspaceArchiveController {

    private final WorkspaceArchiveService archiveService;

    public WorkspaceArchiveController(WorkspaceArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @GetMapping
    public ApiResponse<List<WorkspaceArchive>> list() {
        return ApiResponse.ok(archiveService.list());
    }

    /** 详情包含 payload，前端据此导出 JSON 文件 */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(archiveService.detail(id));
    }

    @PostMapping
    public ApiResponse<WorkspaceArchive> save(@RequestBody WorkspaceArchiveRequest request) {
        return ApiResponse.ok(archiveService.save(request));
    }

    /** 导入 JSON 存档，只入库，不覆盖当前编辑区 */
    @PostMapping("/import")
    public ApiResponse<WorkspaceArchive> importArchive(@RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(archiveService.importArchive(payload));
    }

    /** 读取存档：覆盖目标编辑区的配置与条目 */
    @PostMapping("/{id}/restore")
    public ApiResponse<Map<String, Object>> restore(@PathVariable Long id,
                                                    @RequestBody WorkspaceArchiveRequest request) {
        return ApiResponse.ok(archiveService.restore(id, request == null ? null : request.getWorkspaceId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        archiveService.delete(id);
        return ApiResponse.ok(null);
    }
}
