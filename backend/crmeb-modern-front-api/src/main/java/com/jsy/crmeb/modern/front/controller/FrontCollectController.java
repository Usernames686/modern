package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.front.FrontAuthService;
import com.jsy.crmeb.modern.service.front.FrontProductService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontCollectController {
    private final FrontAuthService frontAuthService;
    private final FrontProductService frontProductService;

    public FrontCollectController(FrontAuthService frontAuthService, FrontProductService frontProductService) {
        this.frontAuthService = frontAuthService;
        this.frontProductService = frontProductService;
    }

    @GetMapping("/api/front/collect/user")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(frontProductService.collectList(uid(request), page, limit));
    }

    @PostMapping("/api/front/collect/add")
    public ApiResponse<String> add(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        frontProductService.addCollect(uid(request), body);
        return ApiResponse.ok("收藏成功");
    }

    @PostMapping("/api/front/collect/all")
    public ApiResponse<String> all(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        frontProductService.addCollectAll(uid(request), body);
        return ApiResponse.ok("收藏成功");
    }

    @PostMapping("/api/front/collect/delete")
    public ApiResponse<String> delete(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        frontProductService.deleteCollect(uid(request), body);
        return ApiResponse.ok("取消收藏成功");
    }

    @PostMapping("/api/front/collect/cancel/{proId}")
    public ApiResponse<String> cancel(@PathVariable("proId") Integer proId, HttpServletRequest request) {
        frontProductService.cancelCollectByProduct(uid(request), proId);
        return ApiResponse.ok("取消收藏成功");
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
}
