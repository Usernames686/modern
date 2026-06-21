package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.system.SmsAdminService;
import com.jsy.crmeb.modern.service.system.dto.SmsApplyTemplateRequest;
import com.jsy.crmeb.modern.service.system.entity.SmsRecord;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSmsController {
    private final SmsAdminService smsAdminService;

    public AdminSmsController(SmsAdminService smsAdminService) {
        this.smsAdminService = smsAdminService;
    }

    @GetMapping("/api/admin/sms/temps")
    public ApiResponse<Map<String, Object>> templates(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return ApiResponse.ok(smsAdminService.templates(page, limit));
    }

    @PostMapping("/api/admin/sms/temp/apply")
    public ApiResponse<Boolean> applyTemplate(@Valid @RequestBody SmsApplyTemplateRequest request) {
        return ApiResponse.ok(smsAdminService.applyTemplate(request));
    }

    @PostMapping("/api/admin/sms/config/save")
    public ApiResponse<Boolean> saveConfig(@RequestParam Map<String, String> params) {
        return ApiResponse.ok(smsAdminService.saveConfig(params));
    }

    @GetMapping("/api/admin/sms/record/list")
    public ApiResponse<PageResponse<SmsRecord>> records(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "template", required = false) String template) {
        return ApiResponse.ok(smsAdminService.records(page, limit, phone, template));
    }

    @GetMapping("/api/admin/pass/user/record")
    public ApiResponse<Map<String, Object>> oldRecords(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "template", required = false) String template) {
        return ApiResponse.ok(smsAdminService.oldRecordResult(page, limit, phone, template));
    }

    @GetMapping("/api/admin/pass/isLogin")
    public ApiResponse<Map<String, Object>> isLogin() {
        return ApiResponse.ok(smsAdminService.isLogin());
    }

    @GetMapping("/api/admin/pass/info")
    public ApiResponse<Map<String, Object>> info() {
        return ApiResponse.ok(smsAdminService.passInfo());
    }

    @GetMapping("/api/admin/sms/logout")
    public ApiResponse<Map<String, Object>> number() {
        return ApiResponse.ok(smsAdminService.passInfo());
    }

    @PostMapping("/api/admin/pass/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(smsAdminService.safeExternalResult("短信平台登录"));
    }

    @GetMapping("/api/admin/pass/logout")
    public ApiResponse<Map<String, Object>> logout() {
        return ApiResponse.ok(smsAdminService.safeExternalResult("短信平台退出登录"));
    }

    @GetMapping("/api/admin/pass/sendUserCode")
    public ApiResponse<Map<String, Object>> sendUserCode(@RequestParam Map<String, String> params) {
        return ApiResponse.ok(smsAdminService.safeExternalResult("短信验证码发送"));
    }

    @PostMapping("/api/admin/pass/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(smsAdminService.safeExternalResult("短信平台开户"));
    }

    @PostMapping("/api/admin/sms/modify/sign")
    public ApiResponse<Map<String, Object>> modifySign(@RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(smsAdminService.safeExternalResult("短信签名同步"));
    }

    @PostMapping("/api/admin/pass/meal/code")
    public ApiResponse<Map<String, Object>> payCode(@RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(smsAdminService.safeExternalResult("短信套餐支付码生成"));
    }

    @GetMapping("/api/admin/pass/meal/list")
    public ApiResponse<Map<String, Object>> mealList(@RequestParam Map<String, String> params) {
        return ApiResponse.ok(Map.of("data", new Object[0], "count", 0, "localMode", true));
    }
}
