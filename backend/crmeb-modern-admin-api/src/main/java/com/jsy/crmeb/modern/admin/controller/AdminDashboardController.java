package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.dashboard.DashboardAdminService;
import com.jsy.crmeb.modern.service.dashboard.dto.HomeOperatingDataResponse;
import com.jsy.crmeb.modern.service.dashboard.dto.HomeRateResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminDashboardController {
    private final DashboardAdminService dashboardAdminService;

    public AdminDashboardController(DashboardAdminService dashboardAdminService) {
        this.dashboardAdminService = dashboardAdminService;
    }

    @GetMapping("/api/admin/statistics/home/index")
    public ApiResponse<HomeRateResponse> indexData() {
        return ApiResponse.ok(dashboardAdminService.indexData());
    }

    @GetMapping("/api/admin/statistics/home/operating/data")
    public ApiResponse<HomeOperatingDataResponse> operatingData() {
        return ApiResponse.ok(dashboardAdminService.operatingData());
    }

    @GetMapping("/api/admin/statistics/home/chart/user")
    public ApiResponse<Map<Object, Object>> chartUser() {
        return ApiResponse.ok(dashboardAdminService.chartUser());
    }

    @GetMapping("/api/admin/statistics/home/chart/order")
    public ApiResponse<Map<String, Object>> chartOrder() {
        return ApiResponse.ok(dashboardAdminService.chartOrderLast30());
    }

    @GetMapping("/api/admin/statistics/home/chart/order/week")
    public ApiResponse<Map<String, Object>> chartOrderWeek() {
        return ApiResponse.ok(dashboardAdminService.chartOrderWeek());
    }

    @GetMapping("/api/admin/statistics/home/chart/order/month")
    public ApiResponse<Map<String, Object>> chartOrderMonth() {
        return ApiResponse.ok(dashboardAdminService.chartOrderMonth());
    }

    @GetMapping("/api/admin/statistics/home/chart/order/year")
    public ApiResponse<Map<String, Object>> chartOrderYear() {
        return ApiResponse.ok(dashboardAdminService.chartOrderYear());
    }

    @GetMapping("/api/admin/statistics/product/data")
    public ApiResponse<Map<String, Object>> productData(
            @RequestParam(name = "dateLimit", required = false, defaultValue = "lately30") String dateLimit) {
        return ApiResponse.ok(dashboardAdminService.productData(dateLimit));
    }

    @GetMapping("/api/admin/statistics/product/ranking")
    public ApiResponse<List<Map<String, Object>>> productRanking(
            @RequestParam(name = "dateLimit", required = false, defaultValue = "lately30") String dateLimit,
            @RequestParam(name = "sort", required = false, defaultValue = "payPrice") String sort,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit) {
        return ApiResponse.ok(dashboardAdminService.productRanking(dateLimit, sort, limit));
    }

    @GetMapping("/api/admin/statistics/product/trend")
    public ApiResponse<Map<String, Object>> productTrend(
            @RequestParam(name = "dateLimit", required = false, defaultValue = "lately30") String dateLimit) {
        return ApiResponse.ok(dashboardAdminService.productTrend(dateLimit));
    }

    @GetMapping("/api/admin/statistics/user/total/data")
    public ApiResponse<Map<String, Object>> userTotalData() {
        return ApiResponse.ok(dashboardAdminService.userTotalData());
    }

    @GetMapping("/api/admin/statistics/user/area")
    public ApiResponse<List<Map<String, Object>>> userAreaData() {
        return ApiResponse.ok(dashboardAdminService.userAreaData());
    }

    @GetMapping("/api/admin/statistics/user/channel")
    public ApiResponse<List<Map<String, Object>>> userChannelData() {
        return ApiResponse.ok(dashboardAdminService.userChannelData());
    }

    @GetMapping("/api/admin/statistics/user/overview")
    public ApiResponse<Map<String, Object>> userOverview(
            @RequestParam(name = "dateLimit", required = false, defaultValue = "lately30") String dateLimit) {
        return ApiResponse.ok(dashboardAdminService.userOverview(dateLimit));
    }

    @GetMapping("/api/admin/statistics/user/sex")
    public ApiResponse<List<Map<String, Object>>> userSexData() {
        return ApiResponse.ok(dashboardAdminService.userSexData());
    }

    @GetMapping("/api/admin/statistics/user/overview/list")
    public ApiResponse<List<Map<String, Object>>> userOverviewList(
            @RequestParam(name = "dateLimit", required = false, defaultValue = "lately30") String dateLimit) {
        return ApiResponse.ok(dashboardAdminService.userOverviewList(dateLimit));
    }

    @GetMapping("/api/admin/statistics/trade/data")
    public ApiResponse<Map<String, Object>> tradeData() {
        return ApiResponse.ok(dashboardAdminService.tradeData());
    }

    @GetMapping("/api/admin/statistics/trade/overview")
    public ApiResponse<Map<String, Object>> tradeOverview(
            @RequestParam(name = "dateLimit", required = false, defaultValue = "lately30") String dateLimit) {
        return ApiResponse.ok(dashboardAdminService.tradeOverview(dateLimit));
    }

    @GetMapping("/api/admin/statistics/trade/trend")
    public ApiResponse<Map<String, Object>> tradeTrend(
            @RequestParam(name = "dateLimit", required = false, defaultValue = "lately30") String dateLimit) {
        return ApiResponse.ok(dashboardAdminService.tradeTrend(dateLimit));
    }
}
