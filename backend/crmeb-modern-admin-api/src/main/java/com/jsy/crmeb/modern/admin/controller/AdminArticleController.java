package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.article.ArticleAdminService;
import com.jsy.crmeb.modern.service.article.dto.ArticleRequest;
import com.jsy.crmeb.modern.service.article.dto.ArticleResponse;
import com.jsy.crmeb.modern.service.article.dto.ArticleSearchRequest;
import com.jsy.crmeb.modern.service.article.entity.Article;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminArticleController {
    private final ArticleAdminService articleAdminService;

    public AdminArticleController(ArticleAdminService articleAdminService) {
        this.articleAdminService = articleAdminService;
    }

    @GetMapping("/api/admin/article/list")
    public ApiResponse<PageResponse<ArticleResponse>> list(@ModelAttribute ArticleSearchRequest request) {
        return ApiResponse.ok(articleAdminService.list(request));
    }

    @PostMapping("/api/admin/article/save")
    public ApiResponse<Boolean> save(@Valid @RequestBody ArticleRequest request) {
        return ApiResponse.ok(articleAdminService.create(request));
    }

    @PostMapping("/api/admin/article/update")
    public ApiResponse<Boolean> update(@RequestParam("id") Integer id, @Valid @RequestBody ArticleRequest request) {
        return ApiResponse.ok(articleAdminService.update(id, request));
    }

    @GetMapping("/api/admin/article/delete")
    public ApiResponse<Boolean> delete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(articleAdminService.delete(id));
    }

    @GetMapping("/api/admin/article/info")
    public ApiResponse<Article> info(@RequestParam("id") Integer id) {
        return ApiResponse.ok(articleAdminService.info(id));
    }
}
