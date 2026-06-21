package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.design.PageLayoutAdminService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminPageLayoutController {
    private final PageLayoutAdminService pageLayoutAdminService;

    public AdminPageLayoutController(PageLayoutAdminService pageLayoutAdminService) {
        this.pageLayoutAdminService = pageLayoutAdminService;
    }

    @GetMapping("/api/admin/page/layout/index")
    public ApiResponse<Map<String, Object>> index() {
        return ApiResponse.ok(pageLayoutAdminService.index());
    }

    @GetMapping("/api/admin/page/layout/bottom/navigation/get")
    public ApiResponse<Map<String, Object>> bottomNavigation() {
        return ApiResponse.ok(pageLayoutAdminService.getBottomNavigation());
    }

    @GetMapping("/api/admin/page/layout/category/config")
    public ApiResponse<Map<String, Object>> categoryConfig() {
        return ApiResponse.ok(pageLayoutAdminService.getCategoryConfig());
    }

    @PostMapping("/api/admin/page/layout/index/banner/save")
    public ApiResponse<Void> indexBannerSave(@RequestBody Map<String, Object> request) {
        pageLayoutAdminService.saveIndexBanner(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/page/layout/index/table/save")
    public ApiResponse<Void> indexTableSave(@RequestBody Map<String, Object> request) {
        pageLayoutAdminService.saveIndexTable(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/page/layout/index/menu/save")
    public ApiResponse<Void> indexMenuSave(@RequestBody Map<String, Object> request) {
        pageLayoutAdminService.saveIndexMenu(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/page/layout/index/news/save")
    public ApiResponse<Void> indexNewsSave(@RequestBody Map<String, Object> request) {
        pageLayoutAdminService.saveIndexNews(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/page/layout/user/menu/save")
    public ApiResponse<Void> userMenuSave(@RequestBody Map<String, Object> request) {
        pageLayoutAdminService.saveUserMenu(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/page/layout/user/banner/save")
    public ApiResponse<Void> userBannerSave(@RequestBody Map<String, Object> request) {
        pageLayoutAdminService.saveUserBanner(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/page/layout/bottom/navigation/save")
    public ApiResponse<Void> bottomNavigationSave(@RequestBody Map<String, Object> request) {
        pageLayoutAdminService.saveBottomNavigation(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/page/layout/category/config/save")
    public ApiResponse<Void> categoryConfigSave(@RequestBody Map<String, Object> request) {
        pageLayoutAdminService.saveCategoryConfig(request);
        return ApiResponse.ok(null);
    }
}
