package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.front.FrontAuthService;
import com.jsy.crmeb.modern.service.front.FrontSeckillService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontSeckillController {
    private final FrontSeckillService seckillService;
    private final FrontAuthService frontAuthService;

    public FrontSeckillController(FrontSeckillService seckillService, FrontAuthService frontAuthService) {
        this.seckillService = seckillService;
        this.frontAuthService = frontAuthService;
    }

    @GetMapping("/api/front/seckill/index")
    public ApiResponse<Map<String, Object>> index() {
        return ApiResponse.ok(seckillService.index());
    }

    @GetMapping("/api/front/seckill/header")
    public ApiResponse<List<Map<String, Object>>> header() {
        return ApiResponse.ok(seckillService.header());
    }

    @GetMapping("/api/front/seckill/list/{timeId}")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @PathVariable("timeId") Integer timeId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ApiResponse.ok(seckillService.list(timeId, page, limit));
    }

    @GetMapping("/api/front/seckill/detail/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") Integer id, HttpServletRequest request) {
        return ApiResponse.ok(seckillService.detail(id, uidOrNull(request)));
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
}
