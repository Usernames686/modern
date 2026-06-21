package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.front.FrontAuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontAuthController {
    private final FrontAuthService frontAuthService;

    public FrontAuthController(FrontAuthService frontAuthService) {
        this.frontAuthService = frontAuthService;
    }

    @PostMapping("/api/front/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(frontAuthService.login(body, request.getRemoteAddr()));
    }

    @PostMapping("/api/front/login/mobile")
    public ApiResponse<Map<String, Object>> mobileLogin(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(frontAuthService.login(body, request.getRemoteAddr()));
    }

    @GetMapping("/api/front/logout")
    public ApiResponse<String> logout(HttpServletRequest request) {
        frontAuthService.logout(readToken(request));
        return ApiResponse.ok("退出成功");
    }

    @PostMapping("/api/front/token/is/exist")
    public ApiResponse<Boolean> tokenIsExist(HttpServletRequest request) {
        return ApiResponse.ok(frontAuthService.tokenIsExist(readToken(request)));
    }

    @GetMapping("/api/front/user")
    public ApiResponse<Map<String, Object>> user(HttpServletRequest request) {
        return ApiResponse.ok(frontAuthService.userInfo(readToken(request)));
    }

    @PostMapping("/api/front/user/edit")
    public ApiResponse<Map<String, Object>> userEdit(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(frontAuthService.updateProfile(readToken(request), body));
    }

    @PostMapping("/api/front/update/binding/verify")
    public ApiResponse<String> bindingVerify(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        frontAuthService.verifyCurrentPhone(readToken(request), body);
        return ApiResponse.ok("验证成功");
    }

    @PostMapping("/api/front/update/binding")
    public ApiResponse<Map<String, Object>> bindingPhone(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.ok(frontAuthService.updatePhone(readToken(request), body));
    }

    @PostMapping("/api/front/user/password")
    public ApiResponse<String> updatePassword(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        frontAuthService.updatePassword(readToken(request), body);
        return ApiResponse.ok("修改成功");
    }

    @PostMapping("/api/front/register/reset")
    public ApiResponse<String> resetPassword(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        frontAuthService.updatePassword(readToken(request), body);
        return ApiResponse.ok("修改成功");
    }

    @GetMapping("/api/front/login/config")
    public ApiResponse<Map<String, Object>> loginConfig() {
        return ApiResponse.ok(Map.of(
                "loginLogo", "",
                "wechatLogin", false,
                "phoneLogin", true,
                "accountLogin", true));
    }

    @PostMapping("/api/front/sendCode")
    public ApiResponse<String> sendCode() {
        return ApiResponse.ok("当前未配置短信服务商，已启用安全校验");
    }

    private String readToken(HttpServletRequest request) {
        String token = request.getHeader(FrontAuthService.TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }
        return token;
    }
}
