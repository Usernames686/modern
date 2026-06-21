package com.jsy.crmeb.modern.admin.config;

import com.jsy.crmeb.modern.service.admin.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import com.jsy.crmeb.modern.common.config.CrmebRuntimeProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminWebConfiguration implements WebMvcConfigurer {
    private final AdminAuthService adminAuthService;
    private final CrmebRuntimeProperties runtimeProperties;

    public AdminWebConfiguration(AdminAuthService adminAuthService, CrmebRuntimeProperties runtimeProperties) {
        this.adminAuthService = adminAuthService;
        this.runtimeProperties = runtimeProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminAuthInterceptor())
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns(
                        "/api/admin/login",
                        "/api/admin/getLoginPic",
                        "/api/admin/login/account/detection",
                        "/api/admin/health",
                        "/api/admin/logout",
                        "/api/public/**",
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (runtimeProperties.getImagePath() != null && !runtimeProperties.getImagePath().isBlank()) {
            String uploadLocation = Path.of(runtimeProperties.getImagePath()).toAbsolutePath().normalize().toUri().toString();
            registry.addResourceHandler("/crmebimage/**").addResourceLocations(uploadLocation + "/crmebimage/");
            registry.addResourceHandler("/public/**").addResourceLocations(uploadLocation + "/public/");
        }
    }

    private class AdminAuthInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
            String token = request.getHeader(AdminAuthService.TOKEN_HEADER);
            if (token == null || token.isBlank()) {
                token = request.getParameter("token");
            }
            try {
                if (adminAuthService.isTokenValid(token)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // Broken or stale local tokens must never surface as a blank 500 on the login page.
            }
            writeError(response, 401, AdminAuthService.UNAUTHORIZED_MESSAGE);
            return false;
        }
    }

    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        if (!response.isCommitted()) {
            response.resetBuffer();
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + escapeJson(message) + "\",\"data\":null}");
        response.getWriter().flush();
    }

    private String escapeJson(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i += 1) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
