package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.front.FrontAuthService;
import com.jsy.crmeb.modern.service.front.FrontCommerceService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontCartController {
    private final FrontAuthService frontAuthService;
    private final FrontCommerceService commerceService;

    public FrontCartController(FrontAuthService frontAuthService, FrontCommerceService commerceService) {
        this.frontAuthService = frontAuthService;
        this.commerceService = commerceService;
    }

    @GetMapping("/api/front/cart/list")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(value = "isValid", defaultValue = "true") Boolean isValid,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(commerceService.cartList(uid(request), Boolean.TRUE.equals(isValid), page, limit));
    }

    @PostMapping("/api/front/cart/save")
    public ApiResponse<Map<String, String>> save(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.addCart(uid(request), body));
    }

    @GetMapping("/api/front/cart/count")
    public ApiResponse<Map<String, Integer>> count(
            @RequestParam(value = "numType", required = false) Boolean numType,
            @RequestParam(value = "type", required = false) String type,
            HttpServletRequest request) {
        return ApiResponse.ok(commerceService.cartCount(uid(request), numType, type));
    }

    @PostMapping("/api/front/cart/num")
    public ApiResponse<String> updateNum(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        commerceService.updateCartNum(uid(request), intValue(body.get("id")), intValue(body.get("number")));
        return ApiResponse.ok("修改成功");
    }

    @PostMapping("/api/front/cart/delete")
    public ApiResponse<String> delete(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        commerceService.deleteCart(uid(request), ids(body.get("ids")));
        return ApiResponse.ok("删除成功");
    }

    @PostMapping("/api/front/cart/resetcart")
    public ApiResponse<Map<String, Object>> resetCart(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(commerceService.resetCart(uid(request), body));
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

    private List<Integer> ids(Object value) {
        List<Integer> ids = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                ids.add(intValue(item));
            }
            return ids;
        }
        if (value != null) {
            for (String item : String.valueOf(value).split(",")) {
                if (!item.isBlank()) {
                    ids.add(Integer.valueOf(item.trim()));
                }
            }
        }
        return ids;
    }
}
