package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.admin.AdminAuthService;
import com.jsy.crmeb.modern.service.order.OrderAdminService;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderCountItemResponse;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderDetailResponse;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderSearchRequest;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderStatisticsRequest;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderTimeResponse;
import com.jsy.crmeb.modern.service.order.dto.StoreStaffDetailResponse;
import com.jsy.crmeb.modern.service.order.dto.StoreStaffTopDetailResponse;
import com.jsy.crmeb.modern.service.order.dto.SystemWriteOffOrderResponse;
import com.jsy.crmeb.modern.service.order.dto.SystemWriteOffOrderSearchRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminOrderController {
    private final OrderAdminService orderAdminService;
    private final AdminAuthService adminAuthService;

    public AdminOrderController(OrderAdminService orderAdminService, AdminAuthService adminAuthService) {
        this.orderAdminService = orderAdminService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/api/admin/store/order/list")
    public ApiResponse<PageResponse<StoreOrderDetailResponse>> orderList(@ModelAttribute StoreOrderSearchRequest request) {
        return ApiResponse.ok(orderAdminService.list(request));
    }

    @GetMapping("/api/admin/store/order/info")
    public ApiResponse<StoreOrderDetailResponse> info(@RequestParam("orderNo") String orderNo) {
        return ApiResponse.ok(orderAdminService.info(orderNo));
    }

    @GetMapping("/api/admin/store/order/status/list")
    public ApiResponse<PageResponse<Map<String, Object>>> statusList(
            @RequestParam("orderNo") String orderNo,
            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit) {
        return ApiResponse.ok(orderAdminService.statusList(orderNo, page, limit));
    }

    @PostMapping("/api/admin/store/order/mark")
    public ApiResponse<Boolean> mark(
            @RequestParam("orderNo") String orderNo,
            @RequestParam("mark") String mark) {
        return ApiResponse.ok(orderAdminService.mark(orderNo, mark));
    }

    @PostMapping("/api/admin/store/order/update/price")
    public ApiResponse<Boolean> updatePrice(@RequestBody Map<String, Object> body) {
        String orderNo = String.valueOf(body.getOrDefault("orderNo", ""));
        Object value = body.get("payPrice");
        BigDecimal payPrice = value == null ? null : new BigDecimal(String.valueOf(value));
        return ApiResponse.ok(orderAdminService.updatePrice(orderNo, payPrice));
    }

    @PostMapping("/api/admin/store/order/send")
    public ApiResponse<Boolean> send(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(orderAdminService.send(body));
    }

    @GetMapping("/api/admin/store/order/delete")
    public ApiResponse<Boolean> delete(@RequestParam("orderNo") String orderNo) {
        return ApiResponse.ok(orderAdminService.delete(orderNo));
    }

    @GetMapping("/api/admin/yly/print/{id}")
    public ApiResponse<Boolean> print(@PathVariable("id") String orderNo) {
        return ApiResponse.ok(orderAdminService.print(orderNo));
    }

    @GetMapping("/api/admin/store/order/status/num")
    public ApiResponse<StoreOrderCountItemResponse> orderStatusNum(
            @RequestParam(name = "dateLimit", required = false) String dateLimit,
            @RequestParam(name = "type", required = false, defaultValue = "2") Integer type,
            @RequestParam(name = "orderNo", required = false) String orderNo) {
        return ApiResponse.ok(orderAdminService.statusNum(dateLimit, type, orderNo));
    }

    @GetMapping("/api/admin/export/excel/order")
    public ApiResponse<Map<String, String>> exportOrder(@ModelAttribute StoreOrderSearchRequest request) {
        return ApiResponse.ok(orderAdminService.exportOrder(request));
    }

    @GetMapping("/api/admin/store/order/refund")
    public ApiResponse<Boolean> refund(
            @RequestParam("orderNo") String orderNo,
            @RequestParam("amount") BigDecimal amount) {
        return ApiResponse.ok(orderAdminService.refund(orderNo, amount));
    }

    @GetMapping("/api/admin/store/order/refund/refuse")
    public ApiResponse<Boolean> refundRefuse(
            @RequestParam("orderNo") String orderNo,
            @RequestParam("reason") String reason) {
        return ApiResponse.ok(orderAdminService.refundRefuse(orderNo, reason));
    }

    @GetMapping("/api/admin/store/order/writeUpdate/{vCode}")
    public ApiResponse<Boolean> writeUpdate(@PathVariable("vCode") String verifyCode, HttpServletRequest request) {
        return ApiResponse.ok(orderAdminService.writeOff(verifyCode, adminId(request)));
    }

    @GetMapping("/api/admin/store/order/writeConfirm/{vCode}")
    public ApiResponse<StoreOrderDetailResponse> writeConfirm(@PathVariable("vCode") String verifyCode) {
        return ApiResponse.ok(orderAdminService.writeConfirm(verifyCode));
    }

    @PostMapping("/api/admin/system/store/order/list")
    public ApiResponse<SystemWriteOffOrderResponse> writeOffList(@ModelAttribute SystemWriteOffOrderSearchRequest request) {
        return ApiResponse.ok(orderAdminService.writeOffList(request));
    }

    @GetMapping("/api/admin/store/order/statistics")
    public ApiResponse<StoreStaffTopDetailResponse> statistics() {
        return ApiResponse.ok(orderAdminService.statistics());
    }

    @GetMapping("/api/admin/store/order/statisticsData")
    public ApiResponse<List<StoreStaffDetailResponse>> statisticsData(@ModelAttribute StoreOrderStatisticsRequest request) {
        return ApiResponse.ok(orderAdminService.statisticsData(request));
    }

    @GetMapping("/api/admin/store/order/time")
    public ApiResponse<StoreOrderTimeResponse> orderTime(@ModelAttribute StoreOrderStatisticsRequest request) {
        return ApiResponse.ok(orderAdminService.orderTime(request));
    }

    private Integer adminId(HttpServletRequest request) {
        String token = request.getHeader(AdminAuthService.TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }
        return adminAuthService.requireAdminIdByToken(token);
    }
}
