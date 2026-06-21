package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.marketing.ActivityStyleAdminService;
import com.jsy.crmeb.modern.service.marketing.dto.ActivityStyleRequest;
import com.jsy.crmeb.modern.service.marketing.dto.ActivityStyleResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminActivityStyleController {
    private final ActivityStyleAdminService activityStyleAdminService;

    public AdminActivityStyleController(ActivityStyleAdminService activityStyleAdminService) {
        this.activityStyleAdminService = activityStyleAdminService;
    }

    @GetMapping("/api/admin/activitystyle/list")
    public ApiResponse<PageResponse<ActivityStyleResponse>> list(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "type", required = false) Boolean type,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "status", required = false) Boolean status,
            @RequestParam(name = "runningStatus", required = false) Integer runningStatus,
            @RequestParam(name = "method", required = false) Integer method,
            @RequestParam(name = "starttime", required = false) String starttime,
            @RequestParam(name = "endtime", required = false) String endtime) {
        return ApiResponse.ok(activityStyleAdminService.list(page, limit, type, name, status, runningStatus, method, starttime, endtime));
    }

    @GetMapping("/api/admin/activitystyle/info")
    public ApiResponse<ActivityStyleResponse> info(@RequestParam("id") Integer id) {
        return ApiResponse.ok(activityStyleAdminService.info(id));
    }

    @PostMapping("/api/admin/activitystyle/save")
    public ApiResponse<Boolean> save(@RequestBody ActivityStyleRequest request) {
        return ApiResponse.ok(activityStyleAdminService.save(request));
    }

    @PostMapping("/api/admin/activitystyle/update")
    public ApiResponse<Boolean> update(@RequestBody ActivityStyleRequest request) {
        return ApiResponse.ok(activityStyleAdminService.update(request));
    }

    @GetMapping("/api/admin/activitystyle/delete")
    public ApiResponse<Boolean> delete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(activityStyleAdminService.delete(id));
    }

    @PostMapping("/api/admin/activitystyle/status")
    public ApiResponse<Boolean> status(@RequestBody Map<String, Object> request) {
        Object idValue = request == null ? null : request.get("id");
        Object statusValue = request == null ? null : request.get("status");
        Integer id = idValue instanceof Number number ? number.intValue() : Integer.valueOf(String.valueOf(idValue));
        Boolean status = statusValue instanceof Boolean bool ? bool : Boolean.valueOf(String.valueOf(statusValue));
        return ApiResponse.ok(activityStyleAdminService.updateStatus(id, status));
    }
}
