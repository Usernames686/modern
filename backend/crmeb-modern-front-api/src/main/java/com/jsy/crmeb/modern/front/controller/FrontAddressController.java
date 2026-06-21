package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.front.FrontAuthService;
import com.jsy.crmeb.modern.service.front.FrontCommerceService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontAddressController {
    private final FrontAuthService frontAuthService;
    private final FrontCommerceService commerceService;

    public FrontAddressController(FrontAuthService frontAuthService, FrontCommerceService commerceService) {
        this.frontAuthService = frontAuthService;
        this.commerceService = commerceService;
    }

    @GetMapping("/api/front/address/list")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(commerceService.addressList(uid(request), page, limit));
    }

    @PostMapping("/api/front/address/edit")
    public ApiResponse<Map<String, Object>> edit(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.saveAddress(uid(request), body));
    }

    @PostMapping("/api/front/address/del")
    public ApiResponse<String> delete(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        commerceService.deleteAddress(uid(request), intValue(body.get("id")));
        return ApiResponse.ok("删除成功");
    }

    @GetMapping("/api/front/address/detail/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") Integer id, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.addressDetail(uid(request), id));
    }

    @GetMapping("/api/front/address/default")
    public ApiResponse<Map<String, Object>> defaultAddress(HttpServletRequest request) {
        return ApiResponse.ok(commerceService.defaultAddress(uid(request)));
    }

    @PostMapping("/api/front/address/default/set")
    public ApiResponse<Map<String, Object>> setDefault(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.setDefaultAddress(uid(request), intValue(body.get("id"))));
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

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? null : Integer.valueOf(String.valueOf(value));
    }
}
