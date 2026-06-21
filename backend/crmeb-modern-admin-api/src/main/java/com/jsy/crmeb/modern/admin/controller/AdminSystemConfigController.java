package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.system.SystemConfigAdminService;
import com.jsy.crmeb.modern.service.system.dto.SystemFormCheckRequest;
import com.jsy.crmeb.modern.service.system.dto.SystemFormTempRequest;
import com.jsy.crmeb.modern.service.system.entity.SystemConfig;
import com.jsy.crmeb.modern.service.system.entity.SystemFormTemp;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSystemConfigController {
    private final SystemConfigAdminService systemConfigAdminService;

    public AdminSystemConfigController(SystemConfigAdminService systemConfigAdminService) {
        this.systemConfigAdminService = systemConfigAdminService;
    }

    @GetMapping("/api/admin/system/form/temp/info")
    public ApiResponse<SystemFormTemp> formTempInfo(@RequestParam("id") Integer id) {
        return ApiResponse.ok(systemConfigAdminService.formTempInfo(id));
    }

    @GetMapping("/api/admin/system/form/temp/list")
    public ApiResponse<PageResponse<SystemFormTemp>> formTempList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(systemConfigAdminService.formTempList(page, limit, keywords));
    }

    @PostMapping("/api/admin/system/form/temp/save")
    public ApiResponse<Boolean> formTempSave(@RequestBody SystemFormTempRequest request) {
        return ApiResponse.ok(systemConfigAdminService.formTempSave(request));
    }

    @PostMapping("/api/admin/system/form/temp/update")
    public ApiResponse<Boolean> formTempUpdate(
            @RequestParam("id") Integer id,
            @RequestBody SystemFormTempRequest request) {
        return ApiResponse.ok(systemConfigAdminService.formTempUpdate(id, request));
    }

    @GetMapping("/api/admin/system/form/temp/delete")
    public ApiResponse<Boolean> formTempDelete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(systemConfigAdminService.formTempDelete(id));
    }

    @GetMapping("/api/admin/system/config/info")
    public ApiResponse<Map<String, String>> configInfo(@RequestParam("formId") Integer formId) {
        return ApiResponse.ok(systemConfigAdminService.info(formId));
    }

    @PostMapping("/api/admin/system/config/save/form")
    public ApiResponse<String> saveForm(@Valid @RequestBody SystemFormCheckRequest request) {
        systemConfigAdminService.saveForm(request);
        return ApiResponse.ok("表单保存成功");
    }

    @GetMapping("/api/admin/system/config/get/upload/type")
    public ApiResponse<SystemConfig> uploadType() {
        return ApiResponse.ok(systemConfigAdminService.getUploadType());
    }

    @GetMapping("/api/admin/system/config/get/change/color")
    public ApiResponse<SystemConfig> getChangeColor() {
        return ApiResponse.ok(systemConfigAdminService.getChangeColor());
    }

    @PostMapping("/api/admin/system/config/save/change/color")
    public ApiResponse<String> saveChangeColor(@RequestBody Map<String, Object> request) {
        Object value = request == null ? null : request.get("value");
        systemConfigAdminService.saveChangeColor(value == null ? null : String.valueOf(value));
        return ApiResponse.ok("保存成功");
    }

    @PostMapping("/api/admin/system/config/saveuniq")
    public ApiResponse<String> saveUnique(
            @RequestParam("key") String key,
            @RequestParam("value") String value) {
        systemConfigAdminService.saveUnique(key, value);
        return ApiResponse.ok("保存成功");
    }

    @PostMapping("/api/admin/system/config/clear/cache")
    public ApiResponse<String> clearCache() {
        int count = systemConfigAdminService.clearCache();
        return ApiResponse.ok(count > 0 ? "清除成功，已清理 " + count + " 个缓存" : "清除成功");
    }
}
