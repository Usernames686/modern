package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.marketing.BargainAdminService;
import com.jsy.crmeb.modern.service.marketing.dto.BargainProductRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminBargainController {
    private final BargainAdminService bargainAdminService;

    public AdminBargainController(BargainAdminService bargainAdminService) {
        this.bargainAdminService = bargainAdminService;
    }

    @GetMapping("/api/admin/store/bargain/list")
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(bargainAdminService.list(page, limit, status, keywords));
    }

    @GetMapping("/api/admin/store/bargain/info")
    public ApiResponse<Map<String, Object>> info(@RequestParam("id") Integer id) {
        return ApiResponse.ok(bargainAdminService.info(id));
    }

    @PostMapping("/api/admin/store/bargain/save")
    public ApiResponse<Boolean> save(@RequestBody BargainProductRequest request) {
        return ApiResponse.ok(bargainAdminService.save(request));
    }

    @PostMapping("/api/admin/store/bargain/update")
    public ApiResponse<Boolean> update(
            @RequestParam("id") Integer id,
            @RequestBody BargainProductRequest request) {
        return ApiResponse.ok(bargainAdminService.update(id, request));
    }

    @PostMapping("/api/admin/store/bargain/update/status")
    public ApiResponse<Boolean> updateStatus(
            @RequestParam("id") Integer id,
            @RequestParam(name = "status", required = false) String status) {
        return ApiResponse.ok(bargainAdminService.updateStatus(id, status));
    }

    @GetMapping("/api/admin/store/bargain/delete")
    public ApiResponse<Boolean> delete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(bargainAdminService.delete(id));
    }

    @GetMapping("/api/admin/store/bargain/bargain_list")
    public ApiResponse<PageResponse<Map<String, Object>>> bargainUserList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "dateLimit", required = false) String dateLimit) {
        return ApiResponse.ok(bargainAdminService.userList(page, limit, status, dateLimit));
    }

    @GetMapping("/api/admin/store/bargain/bargain_list/{id}")
    public ApiResponse<List<Map<String, Object>>> bargainUserHelp(@PathVariable("id") Integer id) {
        return ApiResponse.ok(bargainAdminService.userHelpList(id));
    }

    @GetMapping("/api/admin/export/excel/bargain/product")
    public ApiResponse<Map<String, String>> export(
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "keywords", required = false) String keywords) {
        return ApiResponse.ok(bargainAdminService.export(status, keywords));
    }
}
