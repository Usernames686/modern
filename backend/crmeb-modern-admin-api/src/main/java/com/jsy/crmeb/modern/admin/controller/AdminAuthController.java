package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.admin.AdminAuthService;
import com.jsy.crmeb.modern.service.admin.AdminLoginPageService;
import com.jsy.crmeb.modern.service.admin.AdminMenuService;
import com.jsy.crmeb.modern.service.admin.CopyrightAdminService;
import com.jsy.crmeb.modern.service.admin.dto.AdminAccountDetectionRequest;
import com.jsy.crmeb.modern.service.admin.dto.AdminInfoResponse;
import com.jsy.crmeb.modern.service.admin.dto.AdminLoginRequest;
import com.jsy.crmeb.modern.service.admin.dto.AdminLoginResponse;
import com.jsy.crmeb.modern.service.admin.dto.MenuResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAuthController {
    private final AdminAuthService adminAuthService;
    private final AdminLoginPageService adminLoginPageService;
    private final AdminMenuService adminMenuService;
    private final CopyrightAdminService copyrightAdminService;

    public AdminAuthController(
            AdminAuthService adminAuthService,
            AdminLoginPageService adminLoginPageService,
            AdminMenuService adminMenuService,
            CopyrightAdminService copyrightAdminService) {
        this.adminAuthService = adminAuthService;
        this.adminLoginPageService = adminLoginPageService;
        this.adminMenuService = adminMenuService;
        this.copyrightAdminService = copyrightAdminService;
    }

    @PostMapping("/api/admin/login")
    public ApiResponse<AdminLoginResponse> login(@RequestBody AdminLoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminAuthService.login(request, clientIp(servletRequest)));
    }

    @GetMapping("/api/admin/getLoginPic")
    public ApiResponse<Map<String, Object>> getLoginPic() {
        return ApiResponse.ok(adminLoginPageService.getLoginPic());
    }

    @GetMapping("/api/admin/copyright/get/info")
    public ApiResponse<Map<String, Object>> copyrightInfo() {
        return ApiResponse.ok(copyrightAdminService.getInfo());
    }

    @PostMapping("/api/admin/login/account/detection")
    public ApiResponse<Integer> accountDetection(@RequestBody AdminAccountDetectionRequest request) {
        return ApiResponse.ok(adminAuthService.accountDetection(request.getAccount()));
    }

    @GetMapping("/api/admin/getAdminInfoByToken")
    public ApiResponse<AdminInfoResponse> getAdminInfoByToken(HttpServletRequest request) {
        return ApiResponse.ok(adminAuthService.getInfoByToken(resolveToken(request)));
    }

    @GetMapping("/api/admin/getMenus")
    public ApiResponse<List<MenuResponse>> getMenus(HttpServletRequest request) {
        return ApiResponse.ok(adminMenuService.getMenus(resolveToken(request)));
    }

    @GetMapping("/api/admin/logout")
    public ApiResponse<Boolean> logout(HttpServletRequest request) {
        adminAuthService.logout(resolveToken(request));
        return ApiResponse.ok(true);
    }

    @PostMapping("/api/admin/logout")
    public ApiResponse<Boolean> logoutByPost(HttpServletRequest request) {
        adminAuthService.logout(resolveToken(request));
        return ApiResponse.ok(true);
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader(AdminAuthService.TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }
        return token;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
