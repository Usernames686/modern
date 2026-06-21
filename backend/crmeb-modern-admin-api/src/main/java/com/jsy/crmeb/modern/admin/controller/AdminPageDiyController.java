package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.design.PageDiyAdminService;
import com.jsy.crmeb.modern.service.design.dto.PageDiyResponse;
import com.jsy.crmeb.modern.service.design.dto.PageDiySaveRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminPageDiyController {
    private final PageDiyAdminService pageDiyAdminService;

    public AdminPageDiyController(PageDiyAdminService pageDiyAdminService) {
        this.pageDiyAdminService = pageDiyAdminService;
    }

    @GetMapping("/api/admin/pagediy/list")
    public ApiResponse<PageResponse<PageDiyResponse>> list(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return ApiResponse.ok(pageDiyAdminService.list(name, page, limit));
    }

    @GetMapping("/api/admin/pagediy/info/{id}")
    public ApiResponse<PageDiyResponse> info(@PathVariable("id") Integer id) {
        return ApiResponse.ok(pageDiyAdminService.info(id));
    }

    @GetMapping("/api/admin/pagediy/getdefault")
    public ApiResponse<Integer> getDefault() {
        return ApiResponse.ok(pageDiyAdminService.defaultId());
    }

    @GetMapping("/api/admin/pagediy/setdefault/{id}")
    public ApiResponse<Void> setDefault(@PathVariable("id") Integer id) {
        pageDiyAdminService.setDefault(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/pagediy/delete")
    public ApiResponse<Void> delete(@RequestParam("id") Integer id) {
        pageDiyAdminService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/pagediy/copy/{id}")
    public ApiResponse<PageDiyResponse> copy(@PathVariable("id") Integer id) {
        return ApiResponse.ok(pageDiyAdminService.copy(id));
    }

    @PostMapping("/api/admin/pagediy/save/base")
    public ApiResponse<PageDiyResponse> saveBase(@RequestBody PageDiySaveRequest request) {
        return ApiResponse.ok(pageDiyAdminService.saveBase(request));
    }

    @PostMapping("/api/admin/pagediy/save")
    public ApiResponse<PageDiyResponse> save(@RequestBody PageDiySaveRequest request) {
        request.setId(null);
        return ApiResponse.ok(pageDiyAdminService.saveBase(request));
    }

    @PostMapping("/api/admin/pagediy/update")
    public ApiResponse<PageDiyResponse> update(@RequestBody PageDiySaveRequest request) {
        return ApiResponse.ok(pageDiyAdminService.saveBase(request));
    }
}
