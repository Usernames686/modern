package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.admin.AdminSystemRoleService;
import com.jsy.crmeb.modern.service.admin.dto.MenuCheckResponse;
import com.jsy.crmeb.modern.service.admin.dto.SystemRoleInfoResponse;
import com.jsy.crmeb.modern.service.admin.dto.SystemRoleRequest;
import com.jsy.crmeb.modern.service.admin.dto.SystemRoleSearchRequest;
import com.jsy.crmeb.modern.service.admin.entity.SystemRole;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSystemRoleController {
    private final AdminSystemRoleService roleService;

    public AdminSystemRoleController(AdminSystemRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/api/admin/system/role/list")
    public ApiResponse<PageResponse<SystemRole>> list(@ModelAttribute SystemRoleSearchRequest request) {
        return ApiResponse.ok(roleService.list(request));
    }

    @PostMapping("/api/admin/system/role/save")
    public ApiResponse<Boolean> save(@RequestBody SystemRoleRequest request) {
        return ApiResponse.ok(roleService.save(request));
    }

    @GetMapping("/api/admin/system/role/delete")
    public ApiResponse<Boolean> delete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(roleService.delete(id));
    }

    @PostMapping("/api/admin/system/role/update")
    public ApiResponse<Boolean> update(@RequestBody SystemRoleRequest request) {
        return ApiResponse.ok(roleService.update(request));
    }

    @GetMapping("/api/admin/system/role/info/{id}")
    public ApiResponse<SystemRoleInfoResponse> info(@PathVariable("id") Integer id) {
        return ApiResponse.ok(roleService.info(id));
    }

    @GetMapping("/api/admin/system/role/updateStatus")
    public ApiResponse<Boolean> updateStatus(@RequestParam("id") Integer id, @RequestParam("status") Boolean status) {
        return ApiResponse.ok(roleService.updateStatus(id, status));
    }

    @GetMapping("/api/admin/system/menu/cache/tree")
    public ApiResponse<List<MenuCheckResponse>> menuCacheTree() {
        return ApiResponse.ok(roleService.menuCacheTree());
    }
}
