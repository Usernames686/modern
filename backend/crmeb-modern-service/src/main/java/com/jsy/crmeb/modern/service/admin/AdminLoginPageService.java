package com.jsy.crmeb.modern.service.admin;

import com.jsy.crmeb.modern.service.admin.mapper.LegacyLoginUiMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminLoginPageService {
    private static final int ADMIN_LOGIN_BANNER_GID = 72;
    private static final Pattern PIC_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"pic\".*?\"value\"\\s*:\\s*\"([^\"]+)\"");

    private final LegacyLoginUiMapper loginUiMapper;

    public AdminLoginPageService(LegacyLoginUiMapper loginUiMapper) {
        this.loginUiMapper = loginUiMapper;
    }

    public Map<String, Object> getLoginPic() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("backgroundImage", normalizeAsset(loginUiMapper.selectConfigValue("admin_login_bg_pic")));
        result.put("logo", normalizeAsset(loginUiMapper.selectConfigValue("site_logo_lefttop")));
        result.put("loginLogo", normalizeAsset(loginUiMapper.selectConfigValue("site_logo_login")));
        result.put("siteName", loginUiMapper.selectConfigValue("site_name"));
        result.put("banner", getBannerList());
        return result;
    }

    private List<Map<String, Object>> getBannerList() {
        List<Map<String, Object>> bannerList = new ArrayList<>();
        for (String value : loginUiMapper.selectGroupDataValues(ADMIN_LOGIN_BANNER_GID)) {
            String pic = extractPic(value);
            if (StringUtils.hasText(pic)) {
                bannerList.add(Map.of("pic", normalizeAsset(pic)));
            }
        }
        return bannerList;
    }

    private String extractPic(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        Matcher matcher = PIC_PATTERN.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String normalizeAsset(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/")) {
            return value;
        }
        return "/" + value;
    }
}
