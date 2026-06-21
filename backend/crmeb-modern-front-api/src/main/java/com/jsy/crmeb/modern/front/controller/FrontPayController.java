package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.front.FrontAuthService;
import com.jsy.crmeb.modern.service.front.FrontPayService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontPayController {
    private final FrontAuthService frontAuthService;
    private final FrontPayService frontPayService;

    public FrontPayController(FrontAuthService frontAuthService, FrontPayService frontPayService) {
        this.frontAuthService = frontAuthService;
        this.frontPayService = frontPayService;
    }

    @GetMapping({"/api/front/pay/get/config", "/api/front/order/get/pay/config"})
    public ApiResponse<Map<String, Object>> getPayConfig(HttpServletRequest request) {
        return ApiResponse.ok(frontPayService.payConfig(uid(request)));
    }

    @PostMapping({"/api/front/pay/payment", "/api/front/order/pay/payment"})
    public ApiResponse<Map<String, Object>> payment(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(frontPayService.payment(uid(request), body));
    }

    @GetMapping({
            "/api/front/pay/queryPayResult",
            "/api/front/order/pay/status",
            "/api/front/pay/status"
    })
    public ApiResponse<Map<String, Object>> queryPayResult(
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "orderId", required = false) String orderId,
            HttpServletRequest request) {
        return ApiResponse.ok(frontPayService.queryPayResult(uid(request), firstText(orderNo, orderId)));
    }

    @GetMapping({
            "/api/front/pay/queryPayResult/{orderNo}",
            "/api/front/pay/status/{orderNo}"
    })
    public ApiResponse<Map<String, Object>> queryPayResultPath(@PathVariable("orderNo") String orderNo, HttpServletRequest request) {
        return ApiResponse.ok(frontPayService.queryPayResult(uid(request), orderNo));
    }

    private Integer uid(HttpServletRequest request) {
        return frontAuthService.requireUidByToken(readToken(request));
    }

    private String readToken(HttpServletRequest request) {
        String token = request.getHeader(FrontAuthService.TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }
        return token;
    }

    private String firstText(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
