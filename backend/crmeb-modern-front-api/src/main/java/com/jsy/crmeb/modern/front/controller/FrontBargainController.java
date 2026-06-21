package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.front.FrontAuthService;
import com.jsy.crmeb.modern.service.front.FrontBargainService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontBargainController {
    private final FrontBargainService bargainService;
    private final FrontAuthService frontAuthService;

    public FrontBargainController(FrontBargainService bargainService, FrontAuthService frontAuthService) {
        this.bargainService = bargainService;
        this.frontAuthService = frontAuthService;
    }

    @GetMapping("/api/front/bargain/index")
    public ApiResponse<Map<String, Object>> index() {
        return ApiResponse.ok(bargainService.index());
    }

    @GetMapping("/api/front/bargain/header")
    public ApiResponse<Map<String, Object>> header() {
        return ApiResponse.ok(bargainService.header());
    }

    @GetMapping("/api/front/bargain/list")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ApiResponse.ok(bargainService.list(page, limit));
    }

    @GetMapping("/api/front/bargain/detail/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") Integer id, HttpServletRequest request) {
        return ApiResponse.ok(bargainService.detail(id, uidOrNull(request)));
    }

    @GetMapping("/api/front/bargain/user")
    public ApiResponse<Map<String, Object>> user(
            @RequestParam("bargainId") Integer bargainId,
            @RequestParam(value = "bargainUserId", required = false) Integer bargainUserId,
            HttpServletRequest request) {
        return ApiResponse.ok(bargainService.user(uid(request), bargainId, bargainUserId));
    }

    @PostMapping("/api/front/bargain/start")
    public ApiResponse<Map<String, Object>> start(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(bargainService.start(uid(request), intValue(body.get("bargainId"))));
    }

    @PostMapping("/api/front/bargain/help")
    public ApiResponse<Map<String, Object>> help(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(bargainService.help(
                uid(request),
                intValue(body.get("bargainId")),
                intValue(body.get("bargainUserId")),
                intValue(body.get("bargainUserUid"))));
    }

    @GetMapping("/api/front/bargain/record")
    public ApiResponse<PageResponse<Map<String, Object>>> record(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(bargainService.record(uid(request), page, limit));
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
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
