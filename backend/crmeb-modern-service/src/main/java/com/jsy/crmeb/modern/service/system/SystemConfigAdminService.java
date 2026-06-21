package com.jsy.crmeb.modern.service.system;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.system.dto.SystemFormCheckRequest;
import com.jsy.crmeb.modern.service.system.dto.SystemFormItemCheckRequest;
import com.jsy.crmeb.modern.service.system.dto.SystemFormTempRequest;
import com.jsy.crmeb.modern.service.system.entity.SystemConfig;
import com.jsy.crmeb.modern.service.system.entity.SystemFormTemp;
import com.jsy.crmeb.modern.service.system.mapper.SystemConfigMapper;
import com.jsy.crmeb.modern.service.system.mapper.SystemFormTempMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemConfigAdminService {
    private static final String UPLOAD_TYPE_KEY = "upload_type";
    private static final String CHANGE_COLOR_KEY = "change_color_config";

    private final SystemConfigMapper configMapper;
    private final SystemFormTempMapper formTempMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public SystemConfigAdminService(
            SystemConfigMapper configMapper,
            SystemFormTempMapper formTempMapper,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate) {
        this.configMapper = configMapper;
        this.formTempMapper = formTempMapper;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    public SystemFormTemp formTempInfo(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("表单id不能为空");
        }
        SystemFormTemp formTemp = formTempMapper.selectById(id);
        if (formTemp == null) {
            throw new IllegalArgumentException("表单模板不存在");
        }
        return formTemp;
    }

    public PageResponse<SystemFormTemp> formTempList(Integer page, Integer limit, String keywords) {
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 9999);
        String cleanKeywords = StringUtils.hasText(keywords) ? keywords.trim() : null;
        long total = formTempMapper.countAll(cleanKeywords);
        List<SystemFormTemp> list = formTempMapper.selectPage(cleanKeywords, (safePage - 1) * safeLimit, safeLimit);
        return new PageResponse<>(safePage, safeLimit, total, list);
    }

    public Boolean formTempSave(SystemFormTempRequest request) {
        validateFormTemp(request);
        LocalDateTime now = LocalDateTime.now();
        SystemFormTemp formTemp = new SystemFormTemp();
        formTemp.setName(trim(request.getName()));
        formTemp.setInfo(trim(request.getInfo()));
        formTemp.setContent(normalizedFormContent(request.getContent()));
        formTemp.setCreateTime(now);
        formTemp.setUpdateTime(now);
        return formTempMapper.insert(formTemp) > 0;
    }

    public Boolean formTempUpdate(Integer id, SystemFormTempRequest request) {
        formTempInfo(id);
        validateFormTemp(request);
        SystemFormTemp formTemp = new SystemFormTemp();
        formTemp.setId(id);
        formTemp.setName(trim(request.getName()));
        formTemp.setInfo(trim(request.getInfo()));
        formTemp.setContent(normalizedFormContent(request.getContent()));
        formTemp.setUpdateTime(LocalDateTime.now());
        return formTempMapper.updateById(formTemp) > 0;
    }

    public Boolean formTempDelete(Integer id) {
        formTempInfo(id);
        return formTempMapper.deleteById(id) > 0;
    }

    public Map<String, String> info(Integer formId) {
        if (formId == null || formId < 0) {
            throw new IllegalArgumentException("formId 参数不合法");
        }
        QueryWrapper<SystemConfig> query = new QueryWrapper<>();
        query.eq("form_id", formId).orderByAsc("id");
        List<SystemConfig> list = configMapper.selectList(query);
        Map<String, String> map = new HashMap<>();
        for (SystemConfig config : list) {
            map.put(config.getName(), config.getValue());
        }
        if (!list.isEmpty()) {
            map.put("id", String.valueOf(formId));
        }
        return map;
    }

    @Transactional
    public boolean saveForm(SystemFormCheckRequest request) {
        validateFormRequest(request);
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<SystemConfig> oldQuery = new QueryWrapper<>();
        oldQuery.eq("form_id", request.getId());
        configMapper.delete(oldQuery);
        for (SystemFormItemCheckRequest field : request.getFields()) {
            SystemConfig config = new SystemConfig();
            config.setName(trim(field.getName()));
            config.setTitle(StringUtils.hasText(field.getTitle()) ? field.getTitle().trim() : trim(field.getName()));
            config.setFormId(request.getId());
            config.setValue(clearPrefix(field.getValue()));
            config.setStatus(false);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            configMapper.insert(config);
        }
        return true;
    }

    public SystemConfig getUploadType() {
        SystemConfig config = getByName(UPLOAD_TYPE_KEY);
        if (config == null) {
            config = new SystemConfig();
            config.setName(UPLOAD_TYPE_KEY);
            config.setValue("1");
        }
        return config;
    }

    public SystemConfig getChangeColor() {
        SystemConfig config = getByName(CHANGE_COLOR_KEY);
        if (config == null) {
            config = new SystemConfig();
            config.setName(CHANGE_COLOR_KEY);
            config.setTitle(CHANGE_COLOR_KEY);
            config.setFormId(0);
            config.setValue("1");
            config.setStatus(false);
        }
        return config;
    }

    public String getConfigValue(String key, String defaultValue) {
        if (!StringUtils.hasText(key)) {
            return defaultValue;
        }
        SystemConfig config = getByName(key.trim());
        if (config == null || config.getValue() == null) {
            return defaultValue;
        }
        return config.getValue();
    }

    @Transactional
    public boolean saveChangeColor(String value) {
        int themeValue = parseThemeValue(value);
        return saveUnique(CHANGE_COLOR_KEY, String.valueOf(themeValue));
    }

    @Transactional
    public boolean saveUnique(String key, String value) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("配置键不能为空");
        }
        String cleanKey = key.trim();
        String cleanValue = trim(value);
        QueryWrapper<SystemConfig> query = new QueryWrapper<>();
        query.eq("name", cleanKey).orderByDesc("id").last("limit 1");
        SystemConfig config = configMapper.selectOne(query);
        LocalDateTime now = LocalDateTime.now();
        if (config == null) {
            config = new SystemConfig();
            config.setName(cleanKey);
            config.setTitle(cleanKey);
            config.setFormId(0);
            config.setValue(cleanValue);
            config.setStatus(false);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            return configMapper.insert(config) > 0;
        }
        config.setValue(cleanValue);
        config.setUpdateTime(now);
        return configMapper.updateById(config) > 0;
    }

    public int clearCache() {
        Set<String> keys = new LinkedHashSet<>();
        collectKeys(keys, "crmeb-modern:config:*");
        collectKeys(keys, "crmeb:config:*");
        collectKeys(keys, "wechat:public:access_token");
        collectKeys(keys, "wechat:mini:access_token");
        if (keys.isEmpty()) {
            return 0;
        }
        Long deleted = redisTemplate.delete(keys);
        return deleted == null ? 0 : deleted.intValue();
    }

    private void validateFormRequest(SystemFormCheckRequest request) {
        if (request == null || request.getId() == null || request.getId() < 0) {
            throw new IllegalArgumentException("请选择表单");
        }
        formTempInfo(request.getId());
        if (request.getFields() == null || request.getFields().isEmpty()) {
            throw new IllegalArgumentException("fields 至少要有一组数据");
        }
        for (SystemFormItemCheckRequest field : request.getFields()) {
            if (!StringUtils.hasText(field.getName())) {
                throw new IllegalArgumentException("字段名称不能为空");
            }
        }
    }

    private void validateFormTemp(SystemFormTempRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("参数错误");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("填写表单名称");
        }
        if (!StringUtils.hasText(request.getInfo())) {
            throw new IllegalArgumentException("填写表单描述");
        }
        normalizedFormContent(request.getContent());
    }

    private String normalizedFormContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("表单内容不能为空");
        }
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode fields = root.get("fields");
            if (fields == null || !fields.isArray()) {
                throw new IllegalArgumentException("表单内容必须包含 fields 数组");
            }
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("表单内容不是合法 JSON");
        }
    }

    private SystemConfig getByName(String name) {
        QueryWrapper<SystemConfig> query = new QueryWrapper<>();
        query.eq("name", name).eq("status", false).orderByDesc("id").last("limit 1");
        return configMapper.selectOne(query);
    }

    private void collectKeys(Set<String> keys, String pattern) {
        Set<String> matched = redisTemplate.keys(pattern);
        if (matched != null) {
            keys.addAll(matched);
        }
    }

    private int parseThemeValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("请选择主题色");
        }
        try {
            int themeValue = Integer.parseInt(value.trim());
            if (themeValue < 1 || themeValue > 5) {
                throw new IllegalArgumentException("主题色参数不合法");
            }
            return themeValue;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("主题色参数不合法");
        }
    }

    private String clearPrefix(String value) {
        String text = trim(value);
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replaceAll("(https?://[^/]+)?/?crmebimage/", "crmebimage/");
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
