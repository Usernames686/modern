package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.RuntimeInfoService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminHealthController {
    private final RuntimeInfoService runtimeInfoService;

    public AdminHealthController(RuntimeInfoService runtimeInfoService) {
        this.runtimeInfoService = runtimeInfoService;
    }

    @GetMapping("/api/admin/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(runtimeInfoService.describe("admin-api"));
    }

    @GetMapping("/api/public/ping")
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.ok(Map.of("service", "admin-api", "status", "ok"));
    }
}
