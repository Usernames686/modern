package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.product.ProductAdminService;
import com.jsy.crmeb.modern.service.product.ProductCatalogService;
import com.jsy.crmeb.modern.service.product.ProductReplyAdminService;
import com.jsy.crmeb.modern.service.product.ProductRuleService;
import com.jsy.crmeb.modern.service.product.dto.CategoryRequest;
import com.jsy.crmeb.modern.service.product.dto.CategoryTreeResponse;
import com.jsy.crmeb.modern.service.product.dto.ProductAddStockRequest;
import com.jsy.crmeb.modern.service.product.dto.StoreProductReplyCommentRequest;
import com.jsy.crmeb.modern.service.product.dto.StoreProductReplyRequest;
import com.jsy.crmeb.modern.service.product.dto.StoreProductReplyResponse;
import com.jsy.crmeb.modern.service.product.dto.StoreProductRuleRequest;
import com.jsy.crmeb.modern.service.product.dto.StoreCopyProductRequest;
import com.jsy.crmeb.modern.service.product.dto.StoreProductBasicUpdateRequest;
import com.jsy.crmeb.modern.service.product.dto.StoreProductCreateRequest;
import com.jsy.crmeb.modern.service.product.dto.StoreProductInfoResponse;
import com.jsy.crmeb.modern.service.product.dto.StoreProductResponse;
import com.jsy.crmeb.modern.service.product.dto.StoreProductSearchRequest;
import com.jsy.crmeb.modern.service.product.dto.StoreProductTabsHeader;
import com.jsy.crmeb.modern.service.product.entity.StoreProductReply;
import com.jsy.crmeb.modern.service.product.entity.StoreProductRule;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminProductController {
    private final ProductAdminService productAdminService;
    private final ProductCatalogService productCatalogService;
    private final ProductRuleService productRuleService;
    private final ProductReplyAdminService productReplyAdminService;

    public AdminProductController(
            ProductAdminService productAdminService,
            ProductCatalogService productCatalogService,
            ProductRuleService productRuleService,
            ProductReplyAdminService productReplyAdminService) {
        this.productAdminService = productAdminService;
        this.productCatalogService = productCatalogService;
        this.productRuleService = productRuleService;
        this.productReplyAdminService = productReplyAdminService;
    }

    @GetMapping("/api/admin/store/product/list")
    public ApiResponse<PageResponse<StoreProductResponse>> productList(@ModelAttribute StoreProductSearchRequest request) {
        return ApiResponse.ok(productAdminService.list(request));
    }

    @GetMapping("/api/admin/store/product/tabs/headers")
    public ApiResponse<List<StoreProductTabsHeader>> productTabsHeaders(@ModelAttribute StoreProductSearchRequest request) {
        return ApiResponse.ok(productAdminService.tabsHeaders(request));
    }

    @GetMapping("/api/admin/store/product/info/{id}")
    public ApiResponse<StoreProductInfoResponse> productInfo(@PathVariable(name = "id") Integer id) {
        return ApiResponse.ok(productAdminService.info(id));
    }

    @GetMapping("/api/admin/store/product/listids/{ids}")
    public ApiResponse<List<StoreProductResponse>> productListByIds(@PathVariable("ids") String ids) {
        List<Integer> idList = List.of(ids.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Integer::valueOf)
                .collect(Collectors.toList());
        return ApiResponse.ok(productAdminService.listByIds(idList));
    }

    @GetMapping("/api/admin/store/product/delete/{id}")
    public ApiResponse<Void> deleteProduct(
            @PathVariable(name = "id") Integer id,
            @RequestParam(name = "type", required = false, defaultValue = "recycle") String type) {
        productAdminService.deleteProduct(id, type);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/store/product/restore/{id}")
    public ApiResponse<Void> restoreProduct(@PathVariable(name = "id") Integer id) {
        productAdminService.restoreProduct(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/store/product/putOnShell/{id}")
    public ApiResponse<Void> putOnShell(@PathVariable(name = "id") Integer id) {
        productAdminService.putOnShelf(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/store/product/offShell/{id}")
    public ApiResponse<Void> offShell(@PathVariable(name = "id") Integer id) {
        productAdminService.offShelf(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/store/product/quick/stock/add")
    public ApiResponse<Void> quickAddStock(@Valid @RequestBody ProductAddStockRequest request) {
        productAdminService.quickAddStock(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/store/product/update")
    public ApiResponse<Void> updateProduct(@RequestBody StoreProductBasicUpdateRequest request) {
        productAdminService.updateBasic(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/store/product/save")
    public ApiResponse<Map<String, Integer>> createProduct(@RequestBody StoreProductCreateRequest request) {
        Integer id = productAdminService.create(request);
        return ApiResponse.ok(Map.of("id", id));
    }

    @PostMapping("/api/admin/store/product/importProduct")
    public ApiResponse<StoreProductCreateRequest> importProduct(
            @RequestParam(name = "form") Integer form,
            @RequestParam(name = "url") String url) {
        return ApiResponse.ok(productAdminService.importProductFromUrl(url, form));
    }

    @PostMapping("/api/admin/store/product/copy/config")
    public ApiResponse<Map<String, Object>> copyConfig() {
        return ApiResponse.ok(productAdminService.copyConfig());
    }

    @PostMapping("/api/admin/store/product/copy/product")
    public ApiResponse<Map<String, Object>> copyProduct(@RequestBody StoreCopyProductRequest request) {
        return ApiResponse.ok(productAdminService.copyProduct(request.getUrl()));
    }

    @GetMapping("/api/admin/store/product/rule/list")
    public ApiResponse<PageResponse<StoreProductRule>> productRuleList(
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit,
            @RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(productRuleService.list(page, limit, keywords));
    }

    @PostMapping("/api/admin/store/product/rule/save")
    public ApiResponse<Boolean> productRuleSave(@RequestBody StoreProductRuleRequest request) {
        return ApiResponse.ok(productRuleService.save(request));
    }

    @PostMapping("/api/admin/store/product/rule/update")
    public ApiResponse<Boolean> productRuleUpdate(@RequestBody StoreProductRuleRequest request) {
        return ApiResponse.ok(productRuleService.update(request));
    }

    @GetMapping("/api/admin/store/product/rule/delete/{ids}")
    public ApiResponse<Boolean> productRuleDelete(@PathVariable("ids") String ids) {
        return ApiResponse.ok(productRuleService.delete(ids));
    }

    @GetMapping("/api/admin/store/product/rule/info/{id}")
    public ApiResponse<StoreProductRule> productRuleInfo(@PathVariable("id") Integer id) {
        return ApiResponse.ok(productRuleService.info(id));
    }

    @GetMapping("/api/admin/store/product/reply/list")
    public ApiResponse<PageResponse<StoreProductReplyResponse>> productReplyList(
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit,
            @RequestParam(name = "isReply", required = false) String isReply,
            @RequestParam(name = "productSearch", required = false) String productSearch,
            @RequestParam(name = "nickname", required = false) String nickname,
            @RequestParam(name = "dateLimit", required = false) String dateLimit) {
        return ApiResponse.ok(productReplyAdminService.list(page, limit, isReply, productSearch, nickname, dateLimit));
    }

    @PostMapping("/api/admin/store/product/reply/save")
    public ApiResponse<Boolean> productReplySave(@RequestBody StoreProductReplyRequest request) {
        return ApiResponse.ok(productReplyAdminService.virtualCreate(request));
    }

    @GetMapping("/api/admin/store/product/reply/delete/{id}")
    public ApiResponse<Boolean> productReplyDelete(@PathVariable("id") Integer id) {
        return ApiResponse.ok(productReplyAdminService.delete(id));
    }

    @GetMapping("/api/admin/store/product/reply/info/{id}")
    public ApiResponse<StoreProductReply> productReplyInfo(@PathVariable("id") Integer id) {
        return ApiResponse.ok(productReplyAdminService.info(id));
    }

    @PostMapping("/api/admin/store/product/reply/comment")
    public ApiResponse<Boolean> productReplyComment(@RequestBody StoreProductReplyCommentRequest request) {
        return ApiResponse.ok(productReplyAdminService.comment(request));
    }

    @GetMapping("/api/admin/export/excel/product")
    public ApiResponse<Map<String, String>> exportProduct(@ModelAttribute StoreProductSearchRequest request) {
        return ApiResponse.ok(productAdminService.exportProduct(request));
    }

    @RequestMapping("/api/admin/category/list/tree")
    public ApiResponse<List<CategoryTreeResponse>> categoryTree(
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "name", required = false) String name) {
        return ApiResponse.ok(productCatalogService.getTree(type, status, name));
    }

    @GetMapping("/api/admin/category/list")
    public ApiResponse<List<CategoryTreeResponse>> categoryList(
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pid", required = false) Integer pid,
            @RequestParam(name = "name", required = false) String name) {
        return ApiResponse.ok(productCatalogService.getList(type, status, pid, name));
    }

    @PostMapping("/api/admin/category/save")
    public ApiResponse<Boolean> categorySave(
            @RequestParam Map<String, String> params,
            @RequestBody(required = false) CategoryRequest body) {
        return ApiResponse.ok(productCatalogService.create(mergeCategoryRequest(params, body)));
    }

    @PostMapping("/api/admin/category/update")
    public ApiResponse<Boolean> categoryUpdate(
            @RequestParam Map<String, String> params,
            @RequestBody(required = false) CategoryRequest body) {
        CategoryRequest request = mergeCategoryRequest(params, body);
        Integer id = intValue(params.get("id"));
        if (id == null) {
            id = request.getId();
        }
        return ApiResponse.ok(productCatalogService.update(id, request));
    }

    @GetMapping("/api/admin/category/delete")
    public ApiResponse<Boolean> categoryDelete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(productCatalogService.delete(id));
    }

    @GetMapping("/api/admin/category/info")
    public ApiResponse<CategoryTreeResponse> categoryInfo(@RequestParam("id") Integer id) {
        return ApiResponse.ok(productCatalogService.info(id));
    }

    @GetMapping("/api/admin/category/list/ids")
    public ApiResponse<List<CategoryTreeResponse>> categoryByIds(@RequestParam("ids") String ids) {
        List<Integer> idList = List.of(ids.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Integer::valueOf)
                .collect(Collectors.toList());
        return ApiResponse.ok(productCatalogService.getByIds(idList));
    }

    @GetMapping({"/api/admin/category/updateStatus/{id}", "/api/admin/category/update/status/{id}"})
    public ApiResponse<Boolean> categoryUpdateStatus(@PathVariable("id") Integer id) {
        return ApiResponse.ok(productCatalogService.updateStatus(id));
    }

    private CategoryRequest mergeCategoryRequest(Map<String, String> params, CategoryRequest body) {
        CategoryRequest request = body == null ? new CategoryRequest() : body;
        if (params == null || params.isEmpty()) {
            return request;
        }
        if (request.getId() == null) request.setId(intValue(params.get("id")));
        if (request.getPid() == null) request.setPid(intValue(params.get("pid")));
        if (request.getName() == null) request.setName(params.get("name"));
        if (request.getType() == null) request.setType(intValue(params.get("type")));
        if (request.getUrl() == null) request.setUrl(params.get("url"));
        if (request.getExtra() == null) request.setExtra(params.get("extra"));
        if (request.getStatus() == null) request.setStatus(booleanValue(params.get("status")));
        if (request.getSort() == null) request.setSort(intValue(params.get("sort")));
        return request;
    }

    private Integer intValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Boolean booleanValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        Integer intValue = intValue(value);
        return intValue == null ? null : intValue == 1;
    }
}
