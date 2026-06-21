package com.jsy.crmeb.modern.service;

import com.jsy.crmeb.modern.common.config.CrmebRuntimeProperties;
import java.util.LinkedHashMap;
import java.util.Map;

public class RuntimeInfoService {
    private final CrmebRuntimeProperties properties;

    public RuntimeInfoService(CrmebRuntimeProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> describe(String application) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("application", application);
        result.put("stack", "Spring Boot 3 / Java 17");
        result.put("imagePath", properties.getImagePath());
        result.put("siteUrl", properties.getSiteUrl());
        return result;
    }
}
