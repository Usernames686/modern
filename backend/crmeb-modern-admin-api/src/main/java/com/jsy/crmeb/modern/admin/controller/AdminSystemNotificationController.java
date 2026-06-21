package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.system.SystemNotificationAdminService;
import com.jsy.crmeb.modern.service.system.dto.NotificationInfoResponse;
import com.jsy.crmeb.modern.service.system.dto.NotificationUpdateRequest;
import com.jsy.crmeb.modern.service.system.entity.SystemNotification;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSystemNotificationController {
    private final SystemNotificationAdminService notificationAdminService;

    public AdminSystemNotificationController(SystemNotificationAdminService notificationAdminService) {
        this.notificationAdminService = notificationAdminService;
    }

    @GetMapping("/api/admin/system/notification/list")
    public ApiResponse<List<SystemNotification>> list(@RequestParam(name = "sendType", required = false) Integer sendType) {
        return ApiResponse.ok(notificationAdminService.list(sendType));
    }

    @PostMapping("/api/admin/system/notification/wechat/switch/{id}")
    public ApiResponse<String> wechatSwitch(@PathVariable("id") Integer id) {
        notificationAdminService.wechatSwitch(id);
        return ApiResponse.ok("更改成功");
    }

    @PostMapping("/api/admin/system/notification/routine/switch/{id}")
    public ApiResponse<String> routineSwitch(@PathVariable("id") Integer id) {
        notificationAdminService.routineSwitch(id);
        return ApiResponse.ok("更改成功");
    }

    @PostMapping("/api/admin/system/notification/sms/switch/{id}")
    public ApiResponse<String> smsSwitch(@PathVariable("id") Integer id) {
        notificationAdminService.smsSwitch(id);
        return ApiResponse.ok("更改成功");
    }

    @GetMapping("/api/admin/system/notification/detail")
    public ApiResponse<NotificationInfoResponse> detail(
            @RequestParam("id") Integer id,
            @RequestParam("detailType") String detailType) {
        return ApiResponse.ok(notificationAdminService.detail(id, detailType));
    }

    @PostMapping("/api/admin/system/notification/update")
    public ApiResponse<Boolean> update(@Valid @RequestBody NotificationUpdateRequest request) {
        return ApiResponse.ok(notificationAdminService.update(request));
    }
}
