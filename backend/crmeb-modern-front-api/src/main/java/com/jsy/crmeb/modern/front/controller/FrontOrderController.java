package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponUserResponse;
import com.jsy.crmeb.modern.service.front.FrontAuthService;
import com.jsy.crmeb.modern.service.front.FrontCommerceService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontOrderController {
    private final FrontAuthService frontAuthService;
    private final FrontCommerceService commerceService;

    public FrontOrderController(FrontAuthService frontAuthService, FrontCommerceService commerceService) {
        this.frontAuthService = frontAuthService;
        this.commerceService = commerceService;
    }

    @GetMapping("/api/front/order/list")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(commerceService.orderList(uid(request), keywords, type, page, limit));
    }

    @GetMapping("/api/front/order/detail/{orderId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("orderId") String orderId, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.orderDetail(uid(request), orderId));
    }

    @GetMapping({
            "/api/front/order/apply/refund/{orderId}",
            "/api/front/order/refund/apply/{orderId}"
    })
    public ApiResponse<Map<String, Object>> refundApplyOrder(@PathVariable("orderId") String orderId, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.refundApplyOrderInfo(uid(request), orderId));
    }

    @GetMapping({"/api/front/order/refund/list", "/api/front/order/refund/list/{type}"})
    public ApiResponse<PageResponse<Map<String, Object>>> refundList(
            @PathVariable(value = "type", required = false) Integer pathType,
            @RequestParam(value = "type", required = false) Integer queryType,
            @RequestParam(value = "refundStatus", required = false) Integer refundStatus,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest request) {
        Integer type = pathType == null ? queryType : pathType;
        return ApiResponse.ok(commerceService.refundOrderList(uid(request), refundStatus, type, page, limit));
    }

    @PostMapping("/api/front/order/product")
    public ApiResponse<Map<String, Object>> orderProduct(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.orderProduct(uid(request), body));
    }

    @PostMapping("/api/front/order/comment")
    public ApiResponse<Boolean> comment(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.commentOrder(uid(request), body));
    }

    @PostMapping("/api/front/order/take")
    public ApiResponse<Boolean> take(
            @RequestParam(value = "id", required = false) Integer queryId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Integer id = queryId == null ? intValue(body == null ? null : body.get("id")) : queryId;
        return ApiResponse.ok(commerceService.takeOrder(uid(request), id));
    }

    @GetMapping("/api/front/order/express/{orderId}")
    public ApiResponse<Map<String, Object>> express(@PathVariable("orderId") String orderId, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.expressOrder(uid(request), orderId));
    }

    @PostMapping({"/api/front/order/refund", "/api/front/order/refund/apply"})
    public ApiResponse<Boolean> refundApply(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.refundApply(uid(request), body));
    }

    @GetMapping("/api/front/order/refund/reason")
    public ApiResponse<List<String>> refundReason() {
        return ApiResponse.ok(commerceService.refundReasons());
    }

    @GetMapping("/api/front/order/data")
    public ApiResponse<Map<String, Integer>> data(HttpServletRequest request) {
        return ApiResponse.ok(commerceService.orderData(uid(request)));
    }

    @PostMapping("/api/front/order/confirm")
    public ApiResponse<Map<String, Object>> confirm(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.orderConfirm(uid(request), body));
    }

    @PostMapping("/api/front/order/pre/order")
    public ApiResponse<Map<String, Object>> preOrder(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.preOrder(uid(request), body));
    }

    @GetMapping("/api/front/order/load/pre/{preOrderNo}")
    public ApiResponse<Map<String, Object>> loadPreOrder(@PathVariable("preOrderNo") String preOrderNo, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.loadPreOrder(uid(request), preOrderNo));
    }

    @GetMapping("/api/front/coupons/order/{preOrderNo}")
    public ApiResponse<List<Map<String, Object>>> orderCoupons(@PathVariable("preOrderNo") String preOrderNo, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.orderCoupons(uid(request), preOrderNo));
    }

    @GetMapping("/api/front/coupons")
    public ApiResponse<PageResponse<Map<String, Object>>> receiveCoupons(
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(commerceService.receiveCoupons(uidOrNull(request), type, page, limit));
    }

    @PostMapping("/api/front/coupon/receive")
    public ApiResponse<Boolean> receiveCoupon(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.receiveCoupon(uid(request), intValue(body == null ? null : body.get("couponId"))));
    }

    @GetMapping({"/api/front/coupon/list", "/api/front/coupons/user"})
    public ApiResponse<PageResponse<StoreCouponUserResponse>> userCoupons(
            @RequestParam(value = "type", defaultValue = "usable") String type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(commerceService.userCoupons(uid(request), type, page, limit));
    }

    @PostMapping("/api/front/order/computed/price")
    public ApiResponse<Map<String, Object>> computedPrice(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.computedPrice(uid(request), body));
    }

    @PostMapping("/api/front/order/create")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.createOrder(uid(request), body));
    }

    @PostMapping("/api/front/order/cancel")
    public ApiResponse<Boolean> cancel(
            @RequestParam(value = "id", required = false) Integer queryId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Integer id = queryId == null ? intValue(body == null ? null : body.get("id")) : queryId;
        return ApiResponse.ok(commerceService.cancelOrder(uid(request), id));
    }

    @PostMapping("/api/front/order/del")
    public ApiResponse<Boolean> delete(
            @RequestParam(value = "id", required = false) Integer queryId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        return ApiResponse.ok(commerceService.deleteOrder(uid(request), queryId, body));
    }

    private Integer uid(HttpServletRequest request) {
        return frontAuthService.requireUidByToken(readToken(request));
    }

    private Integer uidOrNull(HttpServletRequest request) {
        String token = readToken(request);
        return frontAuthService.tokenIsExist(token) ? frontAuthService.requireUidByToken(token) : null;
    }

    private String readToken(HttpServletRequest request) {
        String token = request.getHeader(FrontAuthService.TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }
        return token;
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? null : Integer.valueOf(String.valueOf(value));
    }
}
