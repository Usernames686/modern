package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.admin.AdminSystemMenuService;
import com.jsy.crmeb.modern.service.admin.dto.SystemMenuRequest;
import com.jsy.crmeb.modern.service.admin.dto.SystemMenuSearchRequest;
import com.jsy.crmeb.modern.service.admin.entity.SystemMenu;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSystemMenuController {
    private final AdminSystemMenuService systemMenuService;

    public AdminSystemMenuController(AdminSystemMenuService systemMenuService) {
        this.systemMenuService = systemMenuService;
    }

    @GetMapping("/api/admin/system/menu/list")
    public ApiResponse<List<SystemMenu>> list(@ModelAttribute SystemMenuSearchRequest request) {
        return ApiResponse.ok(systemMenuService.list(request));
    }

    @PostMapping("/api/admin/system/menu/add")
    public ApiResponse<Boolean> add(@RequestBody SystemMenuRequest request) {
        return ApiResponse.ok(systemMenuService.add(request));
    }

    @PostMapping("/api/admin/system/menu/delete/{id}")
    public ApiResponse<Boolean> delete(@PathVariable("id") Integer id) {
        return ApiResponse.ok(systemMenuService.delete(id));
    }

    @PostMapping("/api/admin/system/menu/update")
    public ApiResponse<Boolean> update(@RequestBody SystemMenuRequest request) {
        return ApiResponse.ok(systemMenuService.update(request));
    }

    @GetMapping("/api/admin/system/menu/info/{id}")
    public ApiResponse<SystemMenu> info(@PathVariable("id") Integer id) {
        return ApiResponse.ok(systemMenuService.info(id));
    }

    @PostMapping("/api/admin/system/menu/updateShowStatus/{id}")
    public ApiResponse<Boolean> updateShowStatus(@PathVariable("id") Integer id) {
        return ApiResponse.ok(systemMenuService.updateShowStatus(id));
    }
}
