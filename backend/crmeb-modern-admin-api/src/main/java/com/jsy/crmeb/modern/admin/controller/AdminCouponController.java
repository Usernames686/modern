package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.coupon.CouponAdminService;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponInfoResponse;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponReceiveRequest;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponSaveRequest;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponSearchRequest;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponUserResponse;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponUserSearchRequest;
import com.jsy.crmeb.modern.service.coupon.entity.StoreCoupon;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminCouponController {
    private final CouponAdminService couponAdminService;

    public AdminCouponController(CouponAdminService couponAdminService) {
        this.couponAdminService = couponAdminService;
    }

    @GetMapping("/api/admin/marketing/coupon/list")
    public ApiResponse<PageResponse<StoreCoupon>> couponList(@ModelAttribute StoreCouponSearchRequest request) {
        return ApiResponse.ok(couponAdminService.list(request));
    }

    @GetMapping("/api/admin/marketing/coupon/send/list")
    public ApiResponse<PageResponse<StoreCoupon>> couponSendList(@ModelAttribute StoreCouponSearchRequest request) {
        return ApiResponse.ok(couponAdminService.sendList(request));
    }

    @GetMapping("/api/admin/marketing/coupon/user/list")
    public ApiResponse<PageResponse<StoreCouponUserResponse>> couponUserList(
            @ModelAttribute StoreCouponUserSearchRequest request) {
        return ApiResponse.ok(couponAdminService.userList(request));
    }

    @PostMapping("/api/admin/marketing/coupon/info")
    public ApiResponse<StoreCouponInfoResponse> couponInfo(@RequestParam("id") Integer id) {
        return ApiResponse.ok(couponAdminService.info(id));
    }

    @PostMapping("/api/admin/marketing/coupon/save")
    public ApiResponse<Boolean> couponSave(@RequestBody StoreCouponSaveRequest request) {
        return ApiResponse.ok(couponAdminService.save(request));
    }

    @PostMapping("/api/admin/marketing/coupon/user/receive")
    public ApiResponse<Boolean> couponReceive(@ModelAttribute StoreCouponReceiveRequest request) {
        return ApiResponse.ok(couponAdminService.receive(request));
    }

    @PostMapping("/api/admin/marketing/coupon/update/status")
    public ApiResponse<Boolean> updateCouponStatus(
            @RequestParam("id") Integer id,
            @RequestParam("status") Boolean status) {
        return ApiResponse.ok(couponAdminService.updateStatus(id, status));
    }

    @PostMapping("/api/admin/marketing/coupon/delete")
    public ApiResponse<Boolean> deleteCoupon(@RequestParam("id") Integer id) {
        return ApiResponse.ok(couponAdminService.delete(id));
    }
}
