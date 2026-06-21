package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.marketing.CombinationAdminService;
import com.jsy.crmeb.modern.service.marketing.dto.CombinationProductRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminCombinationController {
    private final CombinationAdminService combinationAdminService;

    public AdminCombinationController(CombinationAdminService combinationAdminService) {
        this.combinationAdminService = combinationAdminService;
    }

    @GetMapping("/api/admin/store/combination/list")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "isShow", required = false) Integer isShow,
            @RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(combinationAdminService.list(page, limit, isShow, keywords));
    }

    @GetMapping("/api/admin/store/combination/info")
    public ApiResponse<Map<String, Object>> info(@RequestParam("id") Integer id) {
        return ApiResponse.ok(combinationAdminService.info(id));
    }

    @GetMapping("/api/admin/store/combination/product/detail")
    public ApiResponse<Map<String, Object>> productDraft(@RequestParam("id") Integer id) {
        return ApiResponse.ok(combinationAdminService.productDraft(id));
    }

    @PostMapping("/api/admin/store/combination/save")
    public ApiResponse<Boolean> save(@RequestBody CombinationProductRequest request) {
        return ApiResponse.ok(combinationAdminService.save(request));
    }

    @PostMapping("/api/admin/store/combination/update")
    public ApiResponse<Boolean> update(
            @RequestParam("id") Integer id,
            @RequestBody CombinationProductRequest request) {
        return ApiResponse.ok(combinationAdminService.update(id, request));
    }

    @PostMapping("/api/admin/store/combination/update/status")
    public ApiResponse<Boolean> updateStatus(
            @RequestParam("id") Integer id,
            @RequestParam(name = "isShow", required = false) String isShow) {
        return ApiResponse.ok(combinationAdminService.updateStatus(id, isShow));
    }

    @GetMapping("/api/admin/store/combination/delete")
    public ApiResponse<Boolean> delete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(combinationAdminService.delete(id));
    }

    @GetMapping("/api/admin/store/combination/statistics")
    public ApiResponse<Map<String, Object>> statistics() {
        return ApiResponse.ok(combinationAdminService.statistics());
    }

    @GetMapping("/api/admin/store/combination/combine/list")
    public ApiResponse<PageResponse<Map<String, Object>>> combineList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "dateLimit", required = false) String dateLimit) {
        return ApiResponse.ok(combinationAdminService.pinkList(page, limit, status, dateLimit));
    }

    @GetMapping("/api/admin/store/combination/order_pink/{id}")
    public ApiResponse<List<Map<String, Object>>> pinkOrders(@PathVariable("id") Integer id) {
        return ApiResponse.ok(combinationAdminService.pinkOrders(id));
    }

    @GetMapping("/api/admin/export/excel/combiantion/product")
    public ApiResponse<Map<String, String>> export(
            @RequestParam(name = "isShow", required = false) Integer isShow,
            @RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(combinationAdminService.export(isShow, keywords));
    }
}
