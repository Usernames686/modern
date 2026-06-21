package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.front.FrontAuthService;
import com.jsy.crmeb.modern.service.front.FrontCombinationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontCombinationController {
    private final FrontCombinationService combinationService;
    private final FrontAuthService frontAuthService;

    public FrontCombinationController(FrontCombinationService combinationService, FrontAuthService frontAuthService) {
        this.combinationService = combinationService;
        this.frontAuthService = frontAuthService;
    }

    @GetMapping("/api/front/combination/index")
    public ApiResponse<Map<String, Object>> index() {
        return ApiResponse.ok(combinationService.index());
    }

    @GetMapping("/api/front/combination/header")
    public ApiResponse<Map<String, Object>> header() {
        return ApiResponse.ok(combinationService.header());
    }

    @GetMapping("/api/front/combination/list")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ApiResponse.ok(combinationService.list(page, limit));
    }

    @GetMapping("/api/front/combination/detail/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") Integer id, HttpServletRequest request) {
        return ApiResponse.ok(combinationService.detail(id, uidOrNull(request)));
    }

    @GetMapping("/api/front/combination/pink/{pinkId}")
    public ApiResponse<Map<String, Object>> pink(@PathVariable("pinkId") Integer pinkId, HttpServletRequest request) {
        return ApiResponse.ok(combinationService.pink(uidOrNull(request), pinkId));
    }

    @GetMapping("/api/front/combination/more")
    public ApiResponse<PageResponse<Map<String, Object>>> more(
            @RequestParam(value = "comId", required = false) Integer comId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ApiResponse.ok(combinationService.more(comId, page, limit));
    }

    @PostMapping("/api/front/combination/remove")
    public ApiResponse<Boolean> remove(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(combinationService.remove(uidOrNull(request), body));
    }

    private Integer uidOrNull(HttpServletRequest request) {
        Object value = request.getAttribute("uid");
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null && !String.valueOf(value).isBlank()) {
            return Integer.valueOf(String.valueOf(value));
        }
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
