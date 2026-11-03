package com.promptvault.web;

import com.promptvault.common.ApiResponse;
import com.promptvault.dto.CategoryOption;
import com.promptvault.dto.TagBatchRequest;
import com.promptvault.entity.Tag;
import com.promptvault.service.SettingService;
import com.promptvault.service.TagService;
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
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;
    private final SettingService settingService;

    public TagController(TagService tagService, SettingService settingService) {
        this.tagService = tagService;
        this.settingService = settingService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(tagService.page(keyword, category, page, size));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(tagService.stats());
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryOption>> categories() {
        return ApiResponse.ok(settingService.getCategories());
    }

    @PostMapping
    public ApiResponse<Tag> create(@RequestBody Tag tag) {
        return ApiResponse.ok(tagService.create(tag));
    }

    @PutMapping("/{id}")
    public ApiResponse<Tag> update(@PathVariable Long id, @RequestBody Tag tag) {
        return ApiResponse.ok(tagService.update(id, tag));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/batch")
    public ApiResponse<Map<String, Object>> batch(@RequestBody TagBatchRequest request) {
        return ApiResponse.ok(tagService.batchImport(request));
    }
}
