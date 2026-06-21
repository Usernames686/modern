package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.distribution.DistributionAdminService;
import com.jsy.crmeb.modern.service.distribution.dto.RetailShopConfigRequest;
import com.jsy.crmeb.modern.service.distribution.dto.SpreadChildUserResponse;
import com.jsy.crmeb.modern.service.distribution.dto.SpreadOrderResponse;
import com.jsy.crmeb.modern.service.distribution.dto.SpreadUserRequest;
import com.jsy.crmeb.modern.service.distribution.dto.SpreadUserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminDistributionController {
    private final DistributionAdminService distributionAdminService;

    public AdminDistributionController(DistributionAdminService distributionAdminService) {
        this.distributionAdminService = distributionAdminService;
    }

    @GetMapping("/api/admin/store/retail/spread/manage/get")
    public ApiResponse<RetailShopConfigRequest> getSpreadInfo() {
        return ApiResponse.ok(distributionAdminService.manageInfo());
    }

    @PostMapping("/api/admin/store/retail/spread/manage/set")
    public ApiResponse<Boolean> setSpreadInfo(@RequestBody RetailShopConfigRequest request) {
        return ApiResponse.ok(distributionAdminService.saveManageInfo(request));
    }

    @GetMapping("/api/admin/store/retail/list")
    public ApiResponse<PageResponse<SpreadUserResponse>> promoterList(
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "dateLimit", required = false) String dateLimit,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return ApiResponse.ok(distributionAdminService.promoterList(keywords, dateLimit, page, limit));
    }

    @PostMapping("/api/admin/store/retail/spread/userlist")
    public ApiResponse<PageResponse<SpreadChildUserResponse>> spreadUserList(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestBody(required = false) SpreadUserRequest request) {
        return ApiResponse.ok(distributionAdminService.spreadUserList(request, page, limit));
    }

    @PostMapping("/api/admin/store/retail/spread/orderlist")
    public ApiResponse<PageResponse<SpreadOrderResponse>> spreadOrderList(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestBody(required = false) SpreadUserRequest request) {
        return ApiResponse.ok(distributionAdminService.spreadOrderList(request, page, limit));
    }
}
