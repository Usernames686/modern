package com.jsy.crmeb.modern.service.admin;

import com.jsy.crmeb.modern.service.admin.mapper.LegacyLoginUiMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CopyrightAdminService {
    private final LegacyLoginUiMapper configMapper;

    public CopyrightAdminService(LegacyLoginUiMapper configMapper) {
        this.configMapper = configMapper;
    }

    public Map<String, Object> getInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("siteName", value("site_name"));
        result.put("siteUrl", value("site_url"));
        result.put("apiUrl", value("api_url"));
        result.put("frontApiUrl", value("front_api_url"));
        result.put("localUploadUrl", value("localUploadUrl"));
        result.put("seoTitle", value("seo_title"));
        result.put("siteLogoLeftTop", normalizeAsset(value("site_logo_lefttop")));
        result.put("siteLogoSquare", normalizeAsset(value("site_logo_square")));
        result.put("siteLogoLogin", normalizeAsset(value("site_logo_login")));
        result.put("companyName", value("copyright_company_name"));
        result.put("companyImage", normalizeAsset(value("copyright_company_image")));
        result.put("icpNumber", value("copyright_icp_number"));
        result.put("icpNumberUrl", value("copyright_icp_number_url"));
        result.put("internetRecord", value("copyright_internet_record"));
        result.put("internetRecordUrl", value("copyright_internet_record_url"));
        result.put("appVersion", value("app_version"));
        result.put("authStatus", "local");
        result.put("authMessage", "外部 CRMEB 授权服务未配置，当前展示系统版权/备案配置。");
        result.put("checkedAt", LocalDateTime.now().toString());
        return result;
    }

    private String value(String name) {
        String value = configMapper.selectConfigValue(name);
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String normalizeAsset(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("/")) {
            return trimmed;
        }
        return "/" + trimmed;
    }
}
