package com.jsy.crmeb.modern.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.wechat.WechatMenuAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminWechatMenuController {
    private final WechatMenuAdminService wechatMenuAdminService;

    public AdminWechatMenuController(WechatMenuAdminService wechatMenuAdminService) {
        this.wechatMenuAdminService = wechatMenuAdminService;
    }

    @GetMapping("/api/admin/wechat/menu/public/get")
    public ApiResponse<ObjectNode> get() {
        return ApiResponse.ok(wechatMenuAdminService.getMenus());
    }

    @PostMapping("/api/admin/wechat/menu/public/create")
    public ApiResponse<Boolean> create(@RequestBody JsonNode body) {
        return ApiResponse.ok(wechatMenuAdminService.saveMenus(body));
    }

    @GetMapping("/api/admin/wechat/menu/public/delete")
    public ApiResponse<Boolean> delete() {
        return ApiResponse.ok(wechatMenuAdminService.deleteMenus());
    }
}
