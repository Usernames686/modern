package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.system.SystemGroupAdminService;
import com.jsy.crmeb.modern.service.system.dto.SystemGroupDataRequest;
import com.jsy.crmeb.modern.service.system.dto.SystemGroupRequest;
import com.jsy.crmeb.modern.service.system.entity.SystemGroup;
import com.jsy.crmeb.modern.service.system.entity.SystemGroupData;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSystemGroupController {
    private final SystemGroupAdminService systemGroupAdminService;

    public AdminSystemGroupController(SystemGroupAdminService systemGroupAdminService) {
        this.systemGroupAdminService = systemGroupAdminService;
    }

    @GetMapping("/api/admin/system/group/list")
    public ApiResponse<PageResponse<SystemGroup>> groupList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(systemGroupAdminService.groupList(page, limit, keywords));
    }

    @GetMapping("/api/admin/system/group/info")
    public ApiResponse<SystemGroup> groupInfo(@RequestParam("id") Integer id) {
        return ApiResponse.ok(systemGroupAdminService.groupInfo(id));
    }

    @PostMapping("/api/admin/system/group/save")
    public ApiResponse<Boolean> groupSave(@RequestParam Map<String, String> params) {
        return ApiResponse.ok(systemGroupAdminService.groupSave(groupRequest(params)));
    }

    @PostMapping("/api/admin/system/group/update")
    public ApiResponse<Boolean> groupUpdate(@RequestParam Map<String, String> params) {
        return ApiResponse.ok(systemGroupAdminService.groupUpdate(groupRequest(params)));
    }

    @GetMapping("/api/admin/system/group/delete")
    public ApiResponse<Boolean> groupDelete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(systemGroupAdminService.groupDelete(id));
    }

    @GetMapping("/api/admin/system/group/data/list")
    public ApiResponse<PageResponse<SystemGroupData>> groupDataList(
            @RequestParam("gid") Integer gid,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(systemGroupAdminService.groupDataList(gid, status, page, limit, keywords));
    }

    @GetMapping("/api/admin/system/group/data/info")
    public ApiResponse<SystemGroupData> groupDataInfo(@RequestParam("id") Integer id) {
        return ApiResponse.ok(systemGroupAdminService.groupDataInfo(id));
    }

    @PostMapping("/api/admin/system/group/data/save")
    public ApiResponse<Boolean> groupDataSave(@RequestBody SystemGroupDataRequest request) {
        return ApiResponse.ok(systemGroupAdminService.groupDataSave(request));
    }

    @PostMapping("/api/admin/system/group/data/update")
    public ApiResponse<Boolean> groupDataUpdate(
            @RequestParam("id") Integer id,
            @RequestBody SystemGroupDataRequest request) {
        return ApiResponse.ok(systemGroupAdminService.groupDataUpdate(id, request));
    }

    @GetMapping("/api/admin/system/group/data/delete")
    public ApiResponse<Boolean> groupDataDelete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(systemGroupAdminService.groupDataDelete(id));
    }

    private SystemGroupRequest groupRequest(Map<String, String> params) {
        SystemGroupRequest request = new SystemGroupRequest();
        request.setId(intValue(params.get("id")));
        request.setName(params.get("name"));
        request.setInfo(params.get("info"));
        request.setFormId(intValue(params.get("formId")));
        return request;
    }

    private Integer intValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
