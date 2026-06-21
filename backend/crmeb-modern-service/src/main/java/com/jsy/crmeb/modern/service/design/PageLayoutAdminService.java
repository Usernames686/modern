package com.jsy.crmeb.modern.service.design;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsy.crmeb.modern.service.system.SystemConfigAdminService;
import com.jsy.crmeb.modern.service.system.entity.SystemGroupData;
import com.jsy.crmeb.modern.service.system.mapper.SystemGroupDataMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PageLayoutAdminService {
    private static final int GID_INDEX_BANNER = 48;
    private static final int GID_INDEX_TABLE = 70;
    private static final int GID_USER_MENU = 54;
    private static final int GID_USER_BANNER = 65;
    private static final int GID_INDEX_MENU = 67;
    private static final int GID_INDEX_NEWS = 68;
    private static final int GID_BOTTOM_NAVIGATION = 74;
    private static final String BOTTOM_NAVIGATION_IS_CUSTOM = "bottom_navigation_is_custom";
    private static final String CATEGORY_PAGE_CONFIG = "category_page_config";
    private static final String IS_SHOW_CATEGORY = "is_show_category";

    private final SystemGroupDataMapper groupDataMapper;
    private final SystemConfigAdminService configAdminService;
    private final ObjectMapper objectMapper;

    public PageLayoutAdminService(
            SystemGroupDataMapper groupDataMapper,
            SystemConfigAdminService configAdminService,
            ObjectMapper objectMapper) {
        this.groupDataMapper = groupDataMapper;
        this.configAdminService = configAdminService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> index() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("indexBanner", convertData(groupDataMapper.selectByGid(GID_INDEX_BANNER)));
        result.put("indexTable", convertData(groupDataMapper.selectByGid(GID_INDEX_TABLE)));
        result.put("indexMenu", convertData(groupDataMapper.selectByGid(GID_INDEX_MENU)));
        result.put("indexNews", convertData(groupDataMapper.selectByGid(GID_INDEX_NEWS)));
        result.put("userMenu", convertData(groupDataMapper.selectByGid(GID_USER_MENU)));
        result.put("userBanner", convertData(groupDataMapper.selectByGid(GID_USER_BANNER)));
        return result;
    }

    public Map<String, Object> getBottomNavigation() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bottomNavigationList", convertData(groupDataMapper.selectByGid(GID_BOTTOM_NAVIGATION)));
        result.put("isCustom", configAdminService.getConfigValue(BOTTOM_NAVIGATION_IS_CUSTOM, "0"));
        return result;
    }

    public Map<String, Object> getCategoryConfig() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categoryPageConfig", configAdminService.getConfigValue(CATEGORY_PAGE_CONFIG, "1"));
        result.put("isShowCategory", configAdminService.getConfigValue(IS_SHOW_CATEGORY, "1"));
        return result;
    }

    @Transactional
    public boolean saveIndexBanner(Map<String, Object> request) {
        return saveList(GID_INDEX_BANNER, listValue(request, "indexBanner"));
    }

    @Transactional
    public boolean saveIndexTable(Map<String, Object> request) {
        return saveList(GID_INDEX_TABLE, listValue(request, "indexTable"));
    }

    @Transactional
    public boolean saveIndexMenu(Map<String, Object> request) {
        return saveList(GID_INDEX_MENU, listValue(request, "indexMenu"));
    }

    @Transactional
    public boolean saveIndexNews(Map<String, Object> request) {
        return saveList(GID_INDEX_NEWS, listValue(request, "indexNews"));
    }

    @Transactional
    public boolean saveUserMenu(Map<String, Object> request) {
        return saveList(GID_USER_MENU, listValue(request, "userMenu"));
    }

    @Transactional
    public boolean saveUserBanner(Map<String, Object> request) {
        return saveList(GID_USER_BANNER, listValue(request, "userBanner"));
    }

    @Transactional
    public boolean saveBottomNavigation(Map<String, Object> request) {
        List<Map<String, Object>> list = listValue(request, "bottomNavigationList");
        Object isCustom = request == null ? null : request.get("isCustom");
        configAdminService.saveUnique(BOTTOM_NAVIGATION_IS_CUSTOM, isCustom == null ? "0" : String.valueOf(isCustom));
        return saveList(GID_BOTTOM_NAVIGATION, list);
    }

    @Transactional
    public boolean saveCategoryConfig(Map<String, Object> request) {
        String categoryPageConfig = categoryPageConfigValue(request == null ? null : request.get("categoryPageConfig"));
        String isShowCategory = booleanString(request == null ? null : request.get("isShowCategory"));
        configAdminService.saveUnique(CATEGORY_PAGE_CONFIG, categoryPageConfig);
        configAdminService.saveUnique(IS_SHOW_CATEGORY, isShowCategory);
        return true;
    }

    private boolean saveList(int gid, List<Map<String, Object>> list) {
        QueryWrapper<SystemGroupData> query = new QueryWrapper<>();
        query.eq("gid", gid);
        groupDataMapper.delete(query);
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < list.size(); index++) {
            Map<String, Object> item = list.get(index);
            SystemGroupData data = new SystemGroupData();
            data.setGid(gid);
            data.setSort(index);
            data.setStatus(booleanValue(item.get("status")));
            data.setValue(toGroupValue(item, index));
            data.setCreateTime(now);
            data.setUpdateTime(now);
            groupDataMapper.insert(data);
        }
        return true;
    }

    private List<Map<String, Object>> convertData(List<SystemGroupData> dataList) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SystemGroupData data : dataList) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", data.getId());
            item.put("gid", data.getGid());
            item.put("sort", data.getSort());
            item.put("status", Boolean.TRUE.equals(data.getStatus()));
            try {
                JsonNode root = objectMapper.readTree(data.getValue());
                item.put("tempid", root.path("id").isMissingNode() ? null : root.path("id").asInt());
                JsonNode fields = root.path("fields");
                if (fields.isArray()) {
                    for (JsonNode field : fields) {
                        String name = field.path("name").asText("");
                        if (StringUtils.hasText(name)) {
                            item.put(name, normalizeAsset(field.path("value").asText("")));
                        }
                    }
                }
            } catch (JsonProcessingException ignored) {
                item.put("tempid", null);
            }
            result.add(item);
        }
        return result;
    }

    private String toGroupValue(Map<String, Object> item, int sort) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", numberValue(item.get("tempid")));
        root.put("sort", sort);
        root.put("status", booleanValue(item.get("status")));
        List<Map<String, Object>> fields = new ArrayList<>();
        for (Map.Entry<String, Object> entry : item.entrySet()) {
            String key = entry.getKey();
            if (List.of("id", "gid", "sort", "status", "tempid").contains(key)) {
                continue;
            }
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("name", key);
            field.put("title", key);
            field.put("value", clearPrefix(entry.getValue()));
            fields.add(field);
        }
        root.put("fields", fields);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("页面配置数据格式错误");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listValue(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
        if (!(value instanceof List<?> rawList)) {
            throw new IllegalArgumentException("请传入" + key + "配置");
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("页面配置格式错误");
            }
            list.add((Map<String, Object>) map);
        }
        return list;
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = value == null ? "" : String.valueOf(value);
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }

    private String booleanString(Object value) {
        return Boolean.TRUE.equals(booleanValue(value)) ? "1" : "0";
    }

    private String categoryPageConfigValue(Object value) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return List.of("1", "2", "3").contains(text) ? text : "1";
    }

    private Integer numberValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String clearPrefix(Object value) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.replaceAll("(https?://[^/]+)?/?crmebimage/", "crmebimage/");
    }

    private String normalizeAsset(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String text = value.trim();
        if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("/")) {
            return text;
        }
        if (text.startsWith("crmebimage/")) {
            return "/" + text;
        }
        return text;
    }
}
