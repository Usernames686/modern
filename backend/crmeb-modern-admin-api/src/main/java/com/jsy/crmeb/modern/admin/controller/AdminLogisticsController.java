package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.logistics.ShippingTemplateService;
import com.jsy.crmeb.modern.service.logistics.dto.ExpressUpdateRequest;
import com.jsy.crmeb.modern.service.logistics.dto.ShippingTemplateFreeResponse;
import com.jsy.crmeb.modern.service.logistics.dto.ShippingTemplateInfoResponse;
import com.jsy.crmeb.modern.service.logistics.dto.ShippingTemplateRegionResponse;
import com.jsy.crmeb.modern.service.logistics.dto.ShippingTemplateRequest;
import com.jsy.crmeb.modern.service.logistics.dto.SystemCityTreeResponse;
import com.jsy.crmeb.modern.service.logistics.entity.ShippingTemplate;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminLogisticsController {
    private final ShippingTemplateService shippingTemplateService;

    public AdminLogisticsController(ShippingTemplateService shippingTemplateService) {
        this.shippingTemplateService = shippingTemplateService;
    }

    @GetMapping("/api/admin/express/shipping/templates/list")
    public ApiResponse<PageResponse<ShippingTemplate>> shippingTemplatesList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(shippingTemplateService.list(page, limit, keywords));
    }

    @GetMapping("/api/admin/express/shipping/templates/info")
    public ApiResponse<ShippingTemplateInfoResponse> shippingTemplatesInfo(@RequestParam("id") Integer id) {
        return ApiResponse.ok(shippingTemplateService.info(id));
    }

    @PostMapping("/api/admin/express/shipping/templates/save")
    public ApiResponse<Boolean> shippingTemplatesSave(@RequestBody ShippingTemplateRequest request) {
        return ApiResponse.ok(shippingTemplateService.save(request));
    }

    @PostMapping("/api/admin/express/shipping/templates/update")
    public ApiResponse<Boolean> shippingTemplatesUpdate(
            @RequestParam("id") Integer id,
            @RequestBody ShippingTemplateRequest request) {
        return ApiResponse.ok(shippingTemplateService.update(id, request));
    }

    @GetMapping("/api/admin/express/shipping/templates/delete")
    public ApiResponse<Boolean> shippingTemplatesDelete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(shippingTemplateService.delete(id));
    }

    @GetMapping("/api/admin/express/shipping/region/list")
    public ApiResponse<List<ShippingTemplateRegionResponse>> shippingRegionList(@RequestParam("tempId") Integer tempId) {
        return ApiResponse.ok(shippingTemplateService.regionGroup(tempId));
    }

    @GetMapping("/api/admin/express/shipping/free/list")
    public ApiResponse<List<ShippingTemplateFreeResponse>> shippingFreeList(@RequestParam("tempId") Integer tempId) {
        return ApiResponse.ok(shippingTemplateService.freeGroup(tempId));
    }

    @GetMapping("/api/admin/system/city/list/tree")
    public ApiResponse<List<SystemCityTreeResponse>> cityTree() {
        return ApiResponse.ok(shippingTemplateService.cityTree());
    }

    @GetMapping("/api/admin/system/city/list")
    public ApiResponse<List<SystemCityTreeResponse>> cityList(
            @RequestParam(name = "parentId", required = false) Integer parentId) {
        return ApiResponse.ok(shippingTemplateService.cityList(parentId));
    }

    @GetMapping("/api/admin/system/city/info")
    public ApiResponse<SystemCityTreeResponse> cityInfo(@RequestParam("id") Integer id) {
        return ApiResponse.ok(shippingTemplateService.cityInfo(id));
    }

    @PostMapping("/api/admin/system/city/update")
    public ApiResponse<Boolean> cityUpdate(
            @RequestParam("id") Integer id,
            @RequestParam(name = "name") String name) {
        return ApiResponse.ok(shippingTemplateService.updateCity(id, name));
    }

    @GetMapping("/api/admin/express/list")
    public ApiResponse<PageResponse<Map<String, Object>>> expressList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "keywords", required = false) String keywords,
            @RequestParam(name = "isShow", required = false) Integer isShow) {
        return ApiResponse.ok(shippingTemplateService.expressList(page, limit, keywords, isShow));
    }

    @GetMapping("/api/admin/express/info")
    public ApiResponse<Map<String, Object>> expressInfo(@RequestParam("id") Integer id) {
        return ApiResponse.ok(shippingTemplateService.expressInfo(id));
    }

    @PostMapping("/api/admin/express/update")
    public ApiResponse<Boolean> expressUpdate(@RequestBody ExpressUpdateRequest request) {
        return ApiResponse.ok(shippingTemplateService.updateExpress(request));
    }

    @PostMapping("/api/admin/express/update/show")
    public ApiResponse<Boolean> expressUpdateShow(@RequestBody ExpressUpdateRequest request) {
        return ApiResponse.ok(shippingTemplateService.updateExpressShow(request));
    }

    @PostMapping("/api/admin/express/sync/express")
    public ApiResponse<Map<String, Object>> expressSync() {
        return ApiResponse.ok(shippingTemplateService.syncExpress());
    }
}
