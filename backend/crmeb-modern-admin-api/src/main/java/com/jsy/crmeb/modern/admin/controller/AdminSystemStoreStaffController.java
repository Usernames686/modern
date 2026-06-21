package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.store.SystemStoreStaffAdminService;
import com.jsy.crmeb.modern.service.store.dto.SystemStoreStaffRequest;
import com.jsy.crmeb.modern.service.store.dto.SystemStoreStaffResponse;
import com.jsy.crmeb.modern.service.store.entity.SystemStoreStaff;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSystemStoreStaffController {
    private final SystemStoreStaffAdminService staffAdminService;

    public AdminSystemStoreStaffController(SystemStoreStaffAdminService staffAdminService) {
        this.staffAdminService = staffAdminService;
    }

    @GetMapping("/api/admin/system/store/staff/list")
    public ApiResponse<PageResponse<SystemStoreStaffResponse>> list(
            @RequestParam(name = "storeId", required = false, defaultValue = "0") Integer storeId,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) {
        return ApiResponse.ok(staffAdminService.list(storeId, page, limit));
    }

    @PostMapping("/api/admin/system/store/staff/save")
    public ApiResponse<Boolean> save(
            @RequestParam Map<String, String> params,
            @RequestBody(required = false) SystemStoreStaffRequest body) {
        return ApiResponse.ok(staffAdminService.save(mergeRequest(params, body)));
    }

    @PostMapping("/api/admin/system/store/staff/update")
    public ApiResponse<Boolean> update(
            @RequestParam Map<String, String> params,
            @RequestBody(required = false) SystemStoreStaffRequest body) {
        SystemStoreStaffRequest request = mergeRequest(params, body);
        Integer id = intValue(params.get("id"));
        if (id == null) {
            id = request.getId();
        }
        return ApiResponse.ok(staffAdminService.update(id, request));
    }

    @GetMapping("/api/admin/system/store/staff/delete")
    public ApiResponse<Boolean> delete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(staffAdminService.delete(id));
    }

    @GetMapping("/api/admin/system/store/staff/update/status")
    public ApiResponse<Boolean> updateStatus(
            @RequestParam("id") Integer id,
            @RequestParam("status") String status) {
        return ApiResponse.ok(staffAdminService.updateStatus(id, statusValue(status)));
    }

    @GetMapping("/api/admin/system/store/staff/info")
    public ApiResponse<SystemStoreStaff> info(@RequestParam("id") Integer id) {
        return ApiResponse.ok(staffAdminService.info(id));
    }

    private SystemStoreStaffRequest mergeRequest(Map<String, String> params, SystemStoreStaffRequest body) {
        SystemStoreStaffRequest request = body == null ? new SystemStoreStaffRequest() : body;
        if (params == null || params.isEmpty()) {
            return request;
        }
        if (request.getId() == null) request.setId(intValue(params.get("id")));
        if (request.getUid() == null) request.setUid(intValue(params.get("uid")));
        if (request.getStoreId() == null) request.setStoreId(intValue(params.get("storeId")));
        if (request.getVerifyStatus() == null) request.setVerifyStatus(statusValue(params.get("verifyStatus")));
        if (request.getStatus() == null) request.setStatus(statusValue(params.get("status")));
        if (request.getAvatar() == null) request.setAvatar(params.get("avatar"));
        if (request.getStaffName() == null) request.setStaffName(params.get("staffName"));
        if (request.getPhone() == null) request.setPhone(params.get("phone"));
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

    private Integer statusValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("true".equalsIgnoreCase(value)) {
            return 1;
        }
        if ("false".equalsIgnoreCase(value)) {
            return 0;
        }
        return intValue(value);
    }
}
