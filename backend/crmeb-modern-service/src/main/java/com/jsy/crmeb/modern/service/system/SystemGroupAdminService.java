package com.jsy.crmeb.modern.service.system;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.system.dto.SystemGroupDataRequest;
import com.jsy.crmeb.modern.service.system.dto.SystemGroupRequest;
import com.jsy.crmeb.modern.service.system.entity.SystemGroup;
import com.jsy.crmeb.modern.service.system.entity.SystemGroupData;
import com.jsy.crmeb.modern.service.system.mapper.SystemGroupDataMapper;
import com.jsy.crmeb.modern.service.system.mapper.SystemGroupMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemGroupAdminService {
    private final SystemGroupMapper groupMapper;
    private final SystemGroupDataMapper groupDataMapper;
    private final SystemConfigAdminService systemConfigAdminService;
    private final ObjectMapper objectMapper;

    public SystemGroupAdminService(
            SystemGroupMapper groupMapper,
            SystemGroupDataMapper groupDataMapper,
            SystemConfigAdminService systemConfigAdminService,
            ObjectMapper objectMapper) {
        this.groupMapper = groupMapper;
        this.groupDataMapper = groupDataMapper;
        this.systemConfigAdminService = systemConfigAdminService;
        this.objectMapper = objectMapper;
    }

    public PageResponse<SystemGroup> groupList(Integer page, Integer limit, String keywords) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit);
        String cleanKeywords = cleanKeywords(keywords);
        long total = groupMapper.countAll(cleanKeywords);
        List<SystemGroup> list = groupMapper.selectPage(cleanKeywords, (safePage - 1) * safeLimit, safeLimit);
        return new PageResponse<>(safePage, safeLimit, total, list);
    }

    public SystemGroup groupInfo(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("数据组id不能为空");
        }
        SystemGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new IllegalArgumentException("数据组不存在");
        }
        return group;
    }

    public Boolean groupSave(SystemGroupRequest request) {
        validateGroup(request);
        SystemGroup group = new SystemGroup();
        group.setName(request.getName().trim());
        group.setInfo(request.getInfo().trim());
        group.setFormId(request.getFormId());
        group.setCreateTime(LocalDateTime.now());
        group.setUpdateTime(LocalDateTime.now());
        return groupMapper.insert(group) > 0;
    }

    public Boolean groupUpdate(SystemGroupRequest request) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("数据组id不能为空");
        }
        groupInfo(request.getId());
        validateGroup(request);
        SystemGroup group = new SystemGroup();
        group.setId(request.getId());
        group.setName(request.getName().trim());
        group.setInfo(request.getInfo().trim());
        group.setFormId(request.getFormId());
        group.setUpdateTime(LocalDateTime.now());
        return groupMapper.updateById(group) > 0;
    }

    @Transactional
    public Boolean groupDelete(Integer id) {
        groupInfo(id);
        return groupMapper.deleteById(id) > 0;
    }

    public PageResponse<SystemGroupData> groupDataList(Integer gid, Integer status, Integer page, Integer limit, String keywords) {
        groupInfo(gid);
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit);
        Boolean statusValue = status == null ? null : status == 1;
        String cleanKeywords = cleanKeywords(keywords);
        long total = groupDataMapper.countAll(gid, statusValue, cleanKeywords);
        List<SystemGroupData> list = groupDataMapper.selectPage(gid, statusValue, cleanKeywords, (safePage - 1) * safeLimit, safeLimit);
        return new PageResponse<>(safePage, safeLimit, total, list);
    }

    public SystemGroupData groupDataInfo(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("组合数据id不能为空");
        }
        SystemGroupData data = groupDataMapper.selectById(id);
        if (data == null) {
            throw new IllegalArgumentException("组合数据不存在");
        }
        return data;
    }

    public Boolean groupDataSave(SystemGroupDataRequest request) {
        validateGroupData(request);
        SystemGroupData data = new SystemGroupData();
        data.setGid(request.getGid());
        applyForm(data, request.getForm());
        data.setCreateTime(LocalDateTime.now());
        data.setUpdateTime(LocalDateTime.now());
        return groupDataMapper.insert(data) > 0;
    }

    public Boolean groupDataUpdate(Integer id, SystemGroupDataRequest request) {
        groupDataInfo(id);
        validateGroupData(request);
        SystemGroupData data = new SystemGroupData();
        data.setId(id);
        data.setGid(request.getGid());
        applyForm(data, request.getForm());
        data.setUpdateTime(LocalDateTime.now());
        return groupDataMapper.updateById(data) > 0;
    }

    public Boolean groupDataDelete(Integer id) {
        groupDataInfo(id);
        return groupDataMapper.deleteById(id) > 0;
    }

    private void validateGroup(SystemGroupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("参数错误");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("填写数据组名称");
        }
        if (!StringUtils.hasText(request.getInfo())) {
            throw new IllegalArgumentException("填写数据简介");
        }
        if (request.getFormId() == null || request.getFormId() <= 0) {
            throw new IllegalArgumentException("请选择表单数据");
        }
        systemConfigAdminService.formTempInfo(request.getFormId());
    }

    @SuppressWarnings("unchecked")
    private void validateGroupData(SystemGroupDataRequest request) {
        if (request == null || request.getGid() == null || request.getGid() <= 0) {
            throw new IllegalArgumentException("数据组id不能为空");
        }
        SystemGroup group = groupInfo(request.getGid());
        Map<String, Object> form = request.getForm();
        if (form == null) {
            throw new IllegalArgumentException("表单数据不能为空");
        }
        Object formId = form.get("id");
        if (numberValue(formId) == null || !numberValue(formId).equals(group.getFormId())) {
            throw new IllegalArgumentException("表单模板不匹配");
        }
        Object fields = form.get("fields");
        if (!(fields instanceof List<?>) || ((List<?>) fields).isEmpty()) {
            throw new IllegalArgumentException("表单字段不能为空");
        }
        for (Object field : (List<?>) fields) {
            if (!(field instanceof Map<?, ?> fieldMap) || !StringUtils.hasText(String.valueOf(fieldMap.get("name")))) {
                throw new IllegalArgumentException("表单字段格式错误");
            }
        }
    }

    private void applyForm(SystemGroupData data, Map<String, Object> form) {
        try {
            String value = objectMapper.writeValueAsString(form);
            data.setValue(clearPrefix(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("表单数据格式错误");
        }
        Integer sort = numberValue(form.get("sort"));
        data.setSort(sort == null ? 0 : sort);
        data.setStatus(booleanValue(form.get("status")));
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

    private String clearPrefix(String value) {
        String text = value == null ? "" : value;
        return text.replaceAll("(https?://[^/]+)?/?crmebimage/", "crmebimage/");
    }

    private int safePage(Integer page) {
        return page == null || page <= 0 ? 1 : page;
    }

    private int safeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 20 : Math.min(limit, 9999);
    }

    private String cleanKeywords(String keywords) {
        return StringUtils.hasText(keywords) ? keywords.trim() : null;
    }
}
