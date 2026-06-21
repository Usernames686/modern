package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.store.SystemStoreAdminService;
import com.jsy.crmeb.modern.service.store.dto.SystemStoreRequest;
import com.jsy.crmeb.modern.service.store.dto.SystemStoreSearchRequest;
import com.jsy.crmeb.modern.service.store.entity.SystemStore;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSystemStoreController {
    private final SystemStoreAdminService systemStoreAdminService;

    public AdminSystemStoreController(SystemStoreAdminService systemStoreAdminService) {
        this.systemStoreAdminService = systemStoreAdminService;
    }

    @GetMapping("/api/admin/system/store/list")
    public ApiResponse<PageResponse<SystemStore>> list(@ModelAttribute SystemStoreSearchRequest request) {
        return ApiResponse.ok(systemStoreAdminService.list(request));
    }

    @GetMapping("/api/admin/system/store/getCount")
    public ApiResponse<Map<String, Integer>> getCount(@RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(systemStoreAdminService.getCount(keywords));
    }

    @PostMapping("/api/admin/system/store/save")
    public ApiResponse<Boolean> save(@RequestBody SystemStoreRequest request) {
        return ApiResponse.ok(systemStoreAdminService.save(request));
    }

    @PostMapping("/api/admin/system/store/update")
    public ApiResponse<Boolean> update(@RequestParam("id") Integer id, @RequestBody SystemStoreRequest request) {
        return ApiResponse.ok(systemStoreAdminService.update(id, request));
    }

    @GetMapping("/api/admin/system/store/update/status")
    public ApiResponse<Boolean> updateStatus(@RequestParam("id") Integer id, @RequestParam("status") Boolean status) {
        return ApiResponse.ok(systemStoreAdminService.updateStatus(id, status));
    }

    @GetMapping("/api/admin/system/store/delete")
    public ApiResponse<Boolean> delete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(systemStoreAdminService.delete(id));
    }

    @GetMapping("/api/admin/system/store/completely/delete")
    public ApiResponse<Boolean> completeDelete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(systemStoreAdminService.completeDelete(id));
    }

    @GetMapping("/api/admin/system/store/recovery")
    public ApiResponse<Boolean> recovery(@RequestParam("id") Integer id) {
        return ApiResponse.ok(systemStoreAdminService.recovery(id));
    }

    @GetMapping("/api/admin/system/store/info")
    public ApiResponse<SystemStore> info(@RequestParam("id") Integer id) {
        return ApiResponse.ok(systemStoreAdminService.info(id));
    }
}
