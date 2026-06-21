package com.jsy.crmeb.modern.service.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.system.dto.SmsApplyTemplateRequest;
import com.jsy.crmeb.modern.service.system.dto.SystemFormCheckRequest;
import com.jsy.crmeb.modern.service.system.dto.SystemFormItemCheckRequest;
import com.jsy.crmeb.modern.service.system.entity.SmsRecord;
import com.jsy.crmeb.modern.service.system.entity.SmsTemplate;
import com.jsy.crmeb.modern.service.system.entity.SystemConfig;
import com.jsy.crmeb.modern.service.system.mapper.SmsRecordMapper;
import com.jsy.crmeb.modern.service.system.mapper.SmsTemplateMapper;
import com.jsy.crmeb.modern.service.system.mapper.SystemConfigMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SmsAdminService {
    private static final int SMS_CONFIG_FORM_ID = 111;

    private final SmsTemplateMapper smsTemplateMapper;
    private final SmsRecordMapper smsRecordMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final SystemConfigAdminService systemConfigAdminService;

    public SmsAdminService(
            SmsTemplateMapper smsTemplateMapper,
            SmsRecordMapper smsRecordMapper,
            SystemConfigMapper systemConfigMapper,
            SystemConfigAdminService systemConfigAdminService) {
        this.smsTemplateMapper = smsTemplateMapper;
        this.smsRecordMapper = smsRecordMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.systemConfigAdminService = systemConfigAdminService;
    }

    public Map<String, Object> templates(Integer page, Integer limit) {
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 20 : limit;
        QueryWrapper<SmsTemplate> query = new QueryWrapper<>();
        query.orderByDesc("id");
        Page<SmsTemplate> templatePage = smsTemplateMapper.selectPage(new Page<>(safePage, safeLimit), query);
        Map<String, Object> result = new HashMap<>();
        result.put("data", templatePage.getRecords());
        result.put("count", templatePage.getTotal());
        result.put("list", templatePage.getRecords());
        result.put("total", templatePage.getTotal());
        return result;
    }

    @Transactional
    public boolean applyTemplate(SmsApplyTemplateRequest request) {
        SmsTemplate template = new SmsTemplate();
        template.setTempId("LOCAL" + System.currentTimeMillis() % 1000000000000L);
        template.setTempType(request.getType());
        template.setTitle(trim(request.getTitle()));
        template.setType(typeText(request.getType()));
        template.setTempKey("");
        template.setStatus(1);
        template.setContent(trim(request.getContent()));
        template.setCreateTime(LocalDateTime.now());
        return smsTemplateMapper.insert(template) > 0;
    }

    public PageResponse<SmsRecord> records(Integer page, Integer limit, String phone, String template) {
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 20 : limit;
        QueryWrapper<SmsRecord> query = new QueryWrapper<>();
        if (StringUtils.hasText(phone)) {
            query.like("phone", phone.trim());
        }
        if (StringUtils.hasText(template)) {
            query.like("template", template.trim());
        }
        query.orderByDesc("id");
        Page<SmsRecord> recordPage = smsRecordMapper.selectPage(new Page<>(safePage, safeLimit), query);
        return new PageResponse<>(safePage, safeLimit, recordPage.getTotal(), recordPage.getRecords());
    }

    public Map<String, Object> oldRecordResult(Integer page, Integer limit, String phone, String template) {
        PageResponse<SmsRecord> response = records(page, limit, phone, template);
        Map<String, Object> result = new HashMap<>();
        result.put("data", response.getList());
        result.put("count", response.getTotal());
        result.put("list", response.getList());
        result.put("total", response.getTotal());
        return result;
    }

    public Map<String, Object> passInfo() {
        Map<String, String> config = configValues();
        Map<String, Object> result = new HashMap<>();
        result.put("status", true);
        result.put("localMode", true);
        result.put("account", config.getOrDefault("sms_account", ""));
        result.put("message", "短信服务商未配置，不触发真实短信、充值或签名同步。");
        result.put("sms", quota("短信", 0));
        result.put("copy", quota("商品采集", 0));
        result.put("query", quota("物流查询", 0));
        result.put("dump", quota("电子面单打印", 0));
        return result;
    }

    public Map<String, Object> isLogin() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", true);
        result.put("localMode", true);
        result.put("message", "短信设置已保存，服务商配置后可启用发送能力。");
        return result;
    }

    public Map<String, Object> safeExternalResult(String action) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", false);
        result.put("localMode", true);
        result.put("message", action + "属于第三方短信平台能力，当前未触发外部请求。");
        return result;
    }

    @Transactional
    public boolean saveConfig(Map<String, String> params) {
        SystemFormCheckRequest request = new SystemFormCheckRequest();
        request.setId(SMS_CONFIG_FORM_ID);
        request.setSort(0);
        request.setStatus(true);
        List<SystemFormItemCheckRequest> fields = params.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()))
                .map(entry -> {
                    SystemFormItemCheckRequest item = new SystemFormItemCheckRequest();
                    item.setName(entry.getKey());
                    item.setTitle(entry.getKey());
                    item.setValue(entry.getValue());
                    return item;
                })
                .toList();
        request.setFields(fields);
        return systemConfigAdminService.saveForm(request);
    }

    private Map<String, String> configValues() {
        QueryWrapper<SystemConfig> query = new QueryWrapper<>();
        query.in("name", "sms_account", "sms_token");
        List<SystemConfig> list = systemConfigMapper.selectList(query);
        Map<String, String> result = new HashMap<>();
        for (SystemConfig config : list) {
            result.put(config.getName(), config.getValue());
        }
        return result;
    }

    private Map<String, Object> quota(String title, int num) {
        Map<String, Object> result = new HashMap<>();
        result.put("title", title);
        result.put("num", num);
        return result;
    }

    private String typeText(Integer type) {
        return switch (type == null ? 2 : type) {
            case 1 -> "验证码";
            case 3 -> "推广";
            default -> "通知";
        };
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
