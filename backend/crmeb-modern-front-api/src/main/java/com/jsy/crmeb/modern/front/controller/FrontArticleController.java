package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.front.FrontArticleService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontArticleController {
    private final FrontArticleService articleService;

    public FrontArticleController(FrontArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/api/front/article/category/list")
    public ApiResponse<PageResponse<Map<String, Object>>> categoryList() {
        return ApiResponse.ok(articleService.categories());
    }

    @GetMapping("/api/front/article/list/{cid}")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @PathVariable("cid") String cid,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "8") int limit) {
        return ApiResponse.ok(articleService.list(cid, page, limit));
    }

    @GetMapping("/api/front/article/hot/list")
    public ApiResponse<PageResponse<Map<String, Object>>> hotList() {
        return ApiResponse.ok(articleService.hotList());
    }

    @GetMapping("/api/front/article/banner/list")
    public ApiResponse<PageResponse<Map<String, Object>>> bannerList() {
        return ApiResponse.ok(articleService.bannerList());
    }

    @GetMapping("/api/front/article/info")
    public ApiResponse<Map<String, Object>> info(@RequestParam("id") Integer id) {
        return ApiResponse.ok(articleService.info(id));
    }
}
