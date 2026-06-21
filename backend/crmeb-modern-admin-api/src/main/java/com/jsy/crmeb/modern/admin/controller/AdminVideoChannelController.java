package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.marketing.VideoChannelAdminService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminVideoChannelController {
    private final VideoChannelAdminService videoChannelAdminService;

    public AdminVideoChannelController(VideoChannelAdminService videoChannelAdminService) {
        this.videoChannelAdminService = videoChannelAdminService;
    }

    @GetMapping("/api/admin/marketing/videoChannel/list")
    public ApiResponse<PageResponse<Map<String, Object>>> productList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(videoChannelAdminService.productList(page, limit, keywords));
    }

    @GetMapping("/api/admin/marketing/videoChannel/draft/list")
    public ApiResponse<PageResponse<Map<String, Object>>> draftList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(videoChannelAdminService.draftList(page, limit, status, keywords));
    }
}
