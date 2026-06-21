package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.marketing.dto.SeckillManagerRequest;
import com.jsy.crmeb.modern.service.marketing.dto.SeckillProductRequest;
import com.jsy.crmeb.modern.service.marketing.SeckillAdminService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSeckillController {
    private final SeckillAdminService seckillAdminService;

    public AdminSeckillController(SeckillAdminService seckillAdminService) {
        this.seckillAdminService = seckillAdminService;
    }

    @GetMapping("/api/admin/store/seckill/list")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "timeId", required = false) Integer timeId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(seckillAdminService.list(page, limit, timeId, status, keywords));
    }

    @GetMapping("/api/admin/store/seckill/info")
    public ApiResponse<Map<String, Object>> info(@RequestParam("id") Integer id) {
        return ApiResponse.ok(seckillAdminService.info(id));
    }

    @GetMapping("/api/admin/store/seckill/product/detail")
    public ApiResponse<Map<String, Object>> productDraft(@RequestParam("id") Integer id) {
        return ApiResponse.ok(seckillAdminService.productDraft(id));
    }

    @PostMapping("/api/admin/store/seckill/save")
    public ApiResponse<Boolean> save(@RequestBody SeckillProductRequest request) {
        return ApiResponse.ok(seckillAdminService.save(request));
    }

    @PostMapping("/api/admin/store/seckill/update")
    public ApiResponse<Boolean> update(
            @RequestParam("id") Integer id,
            @RequestBody SeckillProductRequest request) {
        return ApiResponse.ok(seckillAdminService.update(id, request));
    }

    @PostMapping("/api/admin/store/seckill/update/status")
    public ApiResponse<Boolean> updateStatus(
            @RequestParam("id") Integer id,
            @RequestParam(name = "status", required = false) Integer status) {
        return ApiResponse.ok(seckillAdminService.updateStatus(id, status));
    }

    @GetMapping("/api/admin/store/seckill/delete")
    public ApiResponse<Boolean> delete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(seckillAdminService.delete(id));
    }

    @GetMapping("/api/admin/store/seckill/manger/list")
    public ApiResponse<PageResponse<Map<String, Object>>> managerList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "status", required = false) String status) {
        return ApiResponse.ok(seckillAdminService.managerList(page, limit, name, status));
    }

    @GetMapping("/api/admin/store/seckill/manger/info")
    public ApiResponse<Map<String, Object>> managerInfo(@RequestParam("id") Integer id) {
        return ApiResponse.ok(seckillAdminService.managerInfo(id));
    }

    @PostMapping("/api/admin/store/seckill/manger/save")
    public ApiResponse<Boolean> managerSave(@RequestBody SeckillManagerRequest request) {
        return ApiResponse.ok(seckillAdminService.managerSave(request));
    }

    @PostMapping("/api/admin/store/seckill/manger/update")
    public ApiResponse<Boolean> managerUpdate(
            @RequestParam("id") Integer id,
            @RequestBody SeckillManagerRequest request) {
        return ApiResponse.ok(seckillAdminService.managerUpdate(id, request));
    }

    @GetMapping("/api/admin/store/seckill/manger/delete")
    public ApiResponse<Boolean> managerDelete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(seckillAdminService.managerDelete(id));
    }

    @PostMapping("/api/admin/store/seckill/manger/update/status/{id}")
    public ApiResponse<Boolean> managerUpdateStatus(
            @PathVariable("id") Integer id,
            @RequestParam("status") String status) {
        return ApiResponse.ok(seckillAdminService.managerUpdateStatus(id, status));
    }

    @GetMapping("/api/admin/export/excel/seckill/product")
    public ApiResponse<Map<String, String>> export(
            @RequestParam(name = "timeId", required = false) Integer timeId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(seckillAdminService.export(timeId, status, keywords));
    }
}
