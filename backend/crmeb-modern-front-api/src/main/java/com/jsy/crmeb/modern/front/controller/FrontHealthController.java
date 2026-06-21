package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.RuntimeInfoService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontHealthController {
    private final RuntimeInfoService runtimeInfoService;

    public FrontHealthController(RuntimeInfoService runtimeInfoService) {
        this.runtimeInfoService = runtimeInfoService;
    }

    @GetMapping("/api/front/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(runtimeInfoService.describe("front-api"));
    }

    @GetMapping("/api/public/ping")
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.ok(Map.of("service", "front-api", "status", "ok"));
    }
}
