package com.promptvault.web;

import com.promptvault.common.ApiResponse;
import com.promptvault.dto.SettingsRequest;
import com.promptvault.service.SettingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    public ApiResponse<SettingsRequest> load() {
        return ApiResponse.ok(settingService.load());
    }

    @PutMapping
    public ApiResponse<SettingsRequest> save(@RequestBody SettingsRequest request) {
        return ApiResponse.ok(settingService.save(request));
    }
}
