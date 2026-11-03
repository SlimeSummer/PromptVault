package com.promptvault.web;

import com.promptvault.common.ApiResponse;
import com.promptvault.entity.Lora;
import com.promptvault.entity.Preset;
import com.promptvault.service.LoraService;
import com.promptvault.service.PresetService;
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

@RestController
@RequestMapping("/api/loras")
public class LoraController {

    private final LoraService loraService;
    private final PresetService presetService;

    public LoraController(LoraService loraService, PresetService presetService) {
        this.loraService = loraService;
        this.presetService = presetService;
    }

    @GetMapping
    public ApiResponse<List<Lora>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        return ApiResponse.ok(loraService.list(keyword, sort, order));
    }

    @GetMapping("/{id}")
    public ApiResponse<Lora> get(@PathVariable Long id) {
        return ApiResponse.ok(loraService.get(id));
    }

    @PostMapping
    public ApiResponse<Lora> create(@RequestBody Lora lora) {
        return ApiResponse.ok(loraService.create(lora));
    }

    @PutMapping("/{id}")
    public ApiResponse<Lora> update(@PathVariable Long id, @RequestBody Lora lora) {
        return ApiResponse.ok(loraService.update(id, lora));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        loraService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/presets")
    public ApiResponse<List<Preset>> presets(@PathVariable Long id) {
        return ApiResponse.ok(presetService.listByLora(id));
    }
}
