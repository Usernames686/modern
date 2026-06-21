package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.wechat.WechatReplyAdminService;
import com.jsy.crmeb.modern.service.wechat.dto.WechatReplyRequest;
import com.jsy.crmeb.modern.service.wechat.dto.WechatReplySearchRequest;
import com.jsy.crmeb.modern.service.wechat.entity.WechatReply;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminWechatReplyController {
    private final WechatReplyAdminService wechatReplyAdminService;

    public AdminWechatReplyController(WechatReplyAdminService wechatReplyAdminService) {
        this.wechatReplyAdminService = wechatReplyAdminService;
    }

    @GetMapping("/api/admin/wechat/keywords/reply/list")
    public ApiResponse<PageResponse<WechatReply>> list(@ModelAttribute WechatReplySearchRequest request) {
        return ApiResponse.ok(wechatReplyAdminService.list(request));
    }

    @PostMapping("/api/admin/wechat/keywords/reply/save")
    public ApiResponse<Boolean> save(@Valid @RequestBody WechatReplyRequest request) {
        return ApiResponse.ok(wechatReplyAdminService.create(request));
    }

    @PostMapping("/api/admin/wechat/keywords/reply/update")
    public ApiResponse<Boolean> update(@Valid @RequestBody WechatReplyRequest request) {
        return ApiResponse.ok(wechatReplyAdminService.update(request));
    }

    @PostMapping("/api/admin/wechat/keywords/reply/status")
    public ApiResponse<Boolean> status(@RequestParam("id") Integer id, @RequestParam("status") Boolean status) {
        return ApiResponse.ok(wechatReplyAdminService.updateStatus(id, status));
    }

    @GetMapping("/api/admin/wechat/keywords/reply/info")
    public ApiResponse<WechatReply> info(@RequestParam("id") Integer id) {
        return ApiResponse.ok(wechatReplyAdminService.info(id));
    }

    @GetMapping("/api/admin/wechat/keywords/reply/info/keywords")
    public ApiResponse<WechatReply> infoByKeywords(@RequestParam("keywords") String keywords) {
        return ApiResponse.ok(wechatReplyAdminService.infoByKeywords(keywords));
    }

    @GetMapping("/api/admin/wechat/keywords/reply/delete")
    public ApiResponse<Boolean> delete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(wechatReplyAdminService.delete(id));
    }
}
