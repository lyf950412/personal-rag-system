package com.rag.controller;

import com.rag.dto.ApiResponse;
import com.rag.service.SystemConfigService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class SystemConfigController {
    private final SystemConfigService configService;
    
    public SystemConfigController(SystemConfigService configService) {
        this.configService = configService;
    }
    
    @GetMapping
    public ApiResponse<Map<String, Object>> getAllConfigs() {
        return ApiResponse.success(configService.getAllConfigs());
    }
    
    @PutMapping
    public ApiResponse<Void> updateConfig(@RequestBody Map<String, Object> configs) {
        configService.updateConfigs(configs);
        return ApiResponse.success("配置更新成功", null);
    }
    
    @PutMapping("/{key}")
    public ApiResponse<Void> updateSingleConfig(@PathVariable String key, @RequestBody Map<String, Object> payload) {
        Object value = payload.get("value");
        if (value != null) {
            configService.updateConfig(key, value);
        }
        return ApiResponse.success("配置更新成功", null);
    }
}
