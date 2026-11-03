package com.promptvault.web;

import com.promptvault.common.ApiResponse;
import com.promptvault.dto.PresetItemOrderRequest;
import com.promptvault.entity.Preset;
import com.promptvault.entity.PresetItem;
import com.promptvault.service.PresetService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/presets")
public class PresetController {

    private final PresetService presetService;

    public PresetController(PresetService presetService) {
        this.presetService = presetService;
    }

    @PostMapping
    public ApiResponse<Preset> create(@RequestBody Preset preset) {
        return ApiResponse.ok(presetService.create(preset));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(presetService.detail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Preset> update(@PathVariable Long id, @RequestBody Preset preset) {
        return ApiResponse.ok(presetService.update(id, preset));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        presetService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/duplicate")
    public ApiResponse<Preset> duplicate(@PathVariable Long id) {
        return ApiResponse.ok(presetService.duplicate(id));
    }

    @PostMapping("/{id}/items")
    public ApiResponse<PresetItem> addItem(@PathVariable Long id, @RequestBody PresetItem item) {
        return ApiResponse.ok(presetService.addItem(id, item));
    }

    @PutMapping("/{id}/items/{itemId}")
    public ApiResponse<PresetItem> updateItem(@PathVariable Long id,
                                              @PathVariable Long itemId,
                                              @RequestBody PresetItem item) {
        return ApiResponse.ok(presetService.updateItem(id, itemId, item));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        presetService.deleteItem(id, itemId);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/items/order")
    public ApiResponse<Void> reorder(@PathVariable Long id,
                                     @RequestBody PresetItemOrderRequest request) {
        presetService.reorder(id, request);
        return ApiResponse.ok();
    }
}
