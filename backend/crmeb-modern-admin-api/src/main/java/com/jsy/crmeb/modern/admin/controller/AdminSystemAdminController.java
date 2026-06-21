package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.admin.AdminSystemAdminService;
import com.jsy.crmeb.modern.service.admin.dto.SystemAdminListResponse;
import com.jsy.crmeb.modern.service.admin.dto.SystemAdminRequest;
import com.jsy.crmeb.modern.service.admin.dto.SystemAdminSearchRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSystemAdminController {
    private final AdminSystemAdminService adminSystemAdminService;

    public AdminSystemAdminController(AdminSystemAdminService adminSystemAdminService) {
        this.adminSystemAdminService = adminSystemAdminService;
    }

    @GetMapping("/api/admin/system/admin/list")
    public ApiResponse<PageResponse<SystemAdminListResponse>> list(@ModelAttribute SystemAdminSearchRequest request) {
        return ApiResponse.ok(adminSystemAdminService.list(request));
    }

    @PostMapping("/api/admin/system/admin/save")
    public ApiResponse<Boolean> save(@RequestBody SystemAdminRequest request) {
        return ApiResponse.ok(adminSystemAdminService.save(request));
    }

    @GetMapping("/api/admin/system/admin/delete")
    public ApiResponse<Boolean> delete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(adminSystemAdminService.delete(id));
    }

    @PostMapping("/api/admin/system/admin/update")
    public ApiResponse<Boolean> update(@RequestBody SystemAdminRequest request) {
        return ApiResponse.ok(adminSystemAdminService.update(request));
    }

    @GetMapping("/api/admin/system/admin/info")
    public ApiResponse<SystemAdminListResponse> info(@RequestParam("id") Integer id) {
        return ApiResponse.ok(adminSystemAdminService.info(id));
    }

    @GetMapping("/api/admin/system/admin/updateStatus")
    public ApiResponse<Boolean> updateStatus(@RequestParam("id") Integer id, @RequestParam("status") Boolean status) {
        return ApiResponse.ok(adminSystemAdminService.updateStatus(id, status));
    }

    @GetMapping("/api/admin/system/admin/update/isSms")
    public ApiResponse<Boolean> updateIsSms(@RequestParam("id") Integer id) {
        return ApiResponse.ok(adminSystemAdminService.updateIsSms(id));
    }
}
