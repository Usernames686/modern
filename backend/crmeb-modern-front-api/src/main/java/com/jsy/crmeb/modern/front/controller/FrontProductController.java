package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.design.PageDiyAdminService;
import com.jsy.crmeb.modern.service.design.dto.PageDiyResponse;
import com.jsy.crmeb.modern.service.front.FrontAuthService;
import com.jsy.crmeb.modern.service.front.FrontProductService;
import com.jsy.crmeb.modern.service.product.dto.CategoryTreeResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontProductController {
    private final FrontProductService frontProductService;
    private final FrontAuthService frontAuthService;
    private final PageDiyAdminService pageDiyService;

    public FrontProductController(
            FrontProductService frontProductService,
            FrontAuthService frontAuthService,
            PageDiyAdminService pageDiyService) {
        this.frontProductService = frontProductService;
        this.frontAuthService = frontAuthService;
        this.pageDiyService = pageDiyService;
    }

    @GetMapping("/api/front/index")
    public ApiResponse<Map<String, Object>> index() {
        return ApiResponse.ok(frontProductService.index());
    }

    @GetMapping("/api/front/customer/service/config")
    public ApiResponse<Map<String, Object>> customerServiceConfig() {
        return ApiResponse.ok(frontProductService.customerServiceConfig());
    }

    @GetMapping("/api/front/page/diy/default")
    public ApiResponse<PageDiyResponse> pageDiyDefault() {
        return ApiResponse.ok(pageDiyService.info(0));
    }

    @GetMapping("/api/front/page/diy/{id}")
    public ApiResponse<PageDiyResponse> pageDiyInfo(@PathVariable("id") Integer id) {
        return ApiResponse.ok(pageDiyService.info(id));
    }

    @GetMapping("/api/front/index/product/{type}")
    public ApiResponse<PageResponse<Map<String, Object>>> indexProduct(
            @PathVariable("type") Integer type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ApiResponse.ok(frontProductService.indexProduct(type, page, limit));
    }

    @GetMapping("/api/front/category")
    public ApiResponse<List<CategoryTreeResponse>> category() {
        return ApiResponse.ok(frontProductService.category());
    }

    @GetMapping("/api/front/products")
    public ApiResponse<PageResponse<Map<String, Object>>> products(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "cid", required = false) String cid,
            @RequestParam(value = "cateId", required = false) String cateId,
            @RequestParam(value = "priceOrder", required = false) String priceOrder,
            @RequestParam(value = "salesOrder", required = false) String salesOrder,
            @RequestParam(value = "news", required = false) Boolean news) {
        return ApiResponse.ok(frontProductService.products(page, limit, keyword, cid, cateId, priceOrder, salesOrder, news));
    }

    @GetMapping("/api/front/product/hot")
    public ApiResponse<PageResponse<Map<String, Object>>> hotProduct(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ApiResponse.ok(frontProductService.indexProduct(2, page, limit));
    }

    @GetMapping("/api/front/product/good")
    public ApiResponse<PageResponse<Map<String, Object>>> goodProduct() {
        return ApiResponse.ok(frontProductService.indexProduct(1, 1, 20));
    }

    @GetMapping("/api/front/product/detail/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") Integer id, HttpServletRequest request) {
        String token = readToken(request);
        Integer uid = frontAuthService.tokenIsExist(token) ? frontAuthService.requireUidByToken(token) : null;
        return ApiResponse.ok(frontProductService.detail(id, uid));
    }

    @GetMapping("/api/front/product/sku/detail/{id}")
    public ApiResponse<Map<String, Object>> skuDetail(@PathVariable("id") Integer id) {
        return ApiResponse.ok(frontProductService.detail(id));
    }

    @GetMapping("/api/front/reply/list/{id}")
    public ApiResponse<PageResponse<Map<String, Object>>> replyList(
            @PathVariable("id") Integer id,
            @RequestParam(value = "type", defaultValue = "0") Integer type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ApiResponse.ok(frontProductService.replyList(id, type, page, limit));
    }

    @GetMapping("/api/front/reply/config/{id}")
    public ApiResponse<Map<String, Object>> replyConfig(@PathVariable("id") Integer id) {
        return ApiResponse.ok(frontProductService.replyConfig(id));
    }

    @GetMapping("/api/front/search/keyword")
    public ApiResponse<List<Map<String, Object>>> hotKeywords() {
        return ApiResponse.ok(frontProductService.hotKeywords());
    }

    @GetMapping("/api/front/image/domain")
    public ApiResponse<String> imageDomain() {
        return ApiResponse.ok(frontProductService.imageDomain());
    }

    private String readToken(HttpServletRequest request) {
        String token = request.getHeader(FrontAuthService.TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }
        return token;
    }
}
