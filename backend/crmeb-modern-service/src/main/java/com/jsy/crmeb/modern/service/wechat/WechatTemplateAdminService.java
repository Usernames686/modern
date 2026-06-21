package com.jsy.crmeb.modern.service.wechat;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.service.system.entity.TemplateMessage;
import com.jsy.crmeb.modern.service.system.mapper.TemplateMessageMapper;
import com.jsy.crmeb.modern.service.wechat.dto.WechatProgramMyTempRequest;
import com.jsy.crmeb.modern.service.wechat.dto.WechatTemplateRequest;
import com.jsy.crmeb.modern.service.wechat.entity.WechatProgramMyTemp;
import com.jsy.crmeb.modern.service.wechat.entity.WechatProgramPublicTemp;
import com.jsy.crmeb.modern.service.wechat.mapper.WechatProgramMyTempMapper;
import com.jsy.crmeb.modern.service.wechat.mapper.WechatProgramPublicTempMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WechatTemplateAdminService {
    private final TemplateMessageMapper templateMessageMapper;
    private final WechatProgramPublicTempMapper publicTempMapper;
    private final WechatProgramMyTempMapper myTempMapper;

    public WechatTemplateAdminService(
            TemplateMessageMapper templateMessageMapper,
            WechatProgramPublicTempMapper publicTempMapper,
            WechatProgramMyTempMapper myTempMapper) {
        this.templateMessageMapper = templateMessageMapper;
        this.publicTempMapper = publicTempMapper;
        this.myTempMapper = myTempMapper;
    }

    public Map<String, Object> templateList(Integer page, Integer limit, Integer type, String keywords, Integer status) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit);
        QueryWrapper<TemplateMessage> query = new QueryWrapper<>();
        if (type != null) {
            query.eq("type", type);
        }
        if (status != null) {
            query.eq("status", status);
        }
        if (StringUtils.hasText(keywords)) {
            String value = keywords.trim();
            query.and(wrapper -> wrapper
                    .like("name", value)
                    .or()
                    .like("temp_key", value)
                    .or()
                    .like("temp_id", value));
        }
        query.orderByDesc("update_time").orderByDesc("id");
        Page<TemplateMessage> result = templateMessageMapper.selectPage(new Page<>(safePage, safeLimit), query);
        return oldPage(result.getRecords(), result.getTotal(), safePage, safeLimit);
    }

    public TemplateMessage templateInfo(Integer id) {
        TemplateMessage template = templateMessageMapper.selectById(id);
        if (template == null) {
            throw new IllegalArgumentException("微信模板不存在");
        }
        return template;
    }

    @Transactional
    public boolean templateSave(WechatTemplateRequest request) {
        validateTemplate(request);
        TemplateMessage template = new TemplateMessage();
        copyTemplate(template, request);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        return templateMessageMapper.insert(template) > 0;
    }

    @Transactional
    public boolean templateUpdate(Integer id, WechatTemplateRequest request) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("模板id不能为空");
        }
        templateInfo(id);
        validateTemplate(request);
        TemplateMessage update = new TemplateMessage();
        update.setId(id);
        copyTemplate(update, request);
        update.setUpdateTime(LocalDateTime.now());
        return templateMessageMapper.updateById(update) > 0;
    }

    @Transactional
    public boolean templateStatus(Integer id, Integer status) {
        templateInfo(id);
        TemplateMessage update = new TemplateMessage();
        update.setId(id);
        update.setStatus(status == null ? 0 : status);
        update.setUpdateTime(LocalDateTime.now());
        return templateMessageMapper.updateById(update) > 0;
    }

    @Transactional
    public boolean templateDelete(Integer id) {
        templateInfo(id);
        return templateMessageMapper.deleteById(id) > 0;
    }

    public Map<String, Object> publicTempList(Integer page, Integer limit, Integer tid, String title, Integer type, Integer categoryId) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit);
        QueryWrapper<WechatProgramPublicTemp> query = new QueryWrapper<>();
        if (tid != null) {
            query.eq("tid", tid);
        }
        if (type != null) {
            query.eq("type", type);
        }
        if (categoryId != null) {
            query.eq("category_id", categoryId);
        }
        if (StringUtils.hasText(title)) {
            query.like("title", title.trim());
        }
        query.orderByDesc("update_time").orderByDesc("id");
        Page<WechatProgramPublicTemp> result = publicTempMapper.selectPage(new Page<>(safePage, safeLimit), query);
        return oldPage(result.getRecords(), result.getTotal(), safePage, safeLimit);
    }

    public WechatProgramPublicTemp publicTempInfo(Integer id, Integer tid) {
        QueryWrapper<WechatProgramPublicTemp> query = new QueryWrapper<>();
        if (id != null && id > 0) {
            query.eq("id", id);
        } else if (tid != null && tid > 0) {
            query.eq("tid", tid);
        } else {
            throw new IllegalArgumentException("模板id不能为空");
        }
        query.last("limit 1");
        WechatProgramPublicTemp template = publicTempMapper.selectOne(query);
        if (template == null) {
            throw new IllegalArgumentException("小程序公共模板不存在");
        }
        return template;
    }

    public List<Map<String, Object>> categoryList() {
        QueryWrapper<WechatProgramPublicTemp> query = new QueryWrapper<>();
        query.select("category_id").isNotNull("category_id").groupBy("category_id").orderByAsc("category_id");
        return publicTempMapper.selectObjs(query).stream()
                .map(value -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", value);
                    item.put("categoryId", value);
                    item.put("name", "类目 " + value);
                    return item;
                })
                .toList();
    }

    public Map<String, Object> keywordsByTid(Integer tid) {
        Map<String, Object> result = new HashMap<>();
        result.put("tid", tid);
        result.put("list", List.of());
        result.put("data", List.of());
        result.put("localMode", true);
        result.put("message", "微信平台关键词同步未配置，请在我的模板中维护 kid/extra 字段。");
        return result;
    }

    public Map<String, Object> myTempList(Integer page, Integer limit, Integer id, String title, String tempId, Boolean status, String type) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit);
        QueryWrapper<WechatProgramMyTemp> query = new QueryWrapper<>();
        if (id != null) {
            query.eq("id", id);
        }
        if (StringUtils.hasText(title)) {
            query.like("title", title.trim());
        }
        if (StringUtils.hasText(tempId)) {
            query.eq("temp_id", tempId.trim());
        }
        if (status != null) {
            query.eq("status", status);
        }
        if (StringUtils.hasText(type)) {
            query.eq("type", type.trim());
        }
        query.orderByDesc("update_time").orderByDesc("id");
        Page<WechatProgramMyTemp> result = myTempMapper.selectPage(new Page<>(safePage, safeLimit), query);
        return oldPage(result.getRecords(), result.getTotal(), safePage, safeLimit);
    }

    public WechatProgramMyTemp myTempInfo(Integer id) {
        WechatProgramMyTemp template = myTempMapper.selectById(id);
        if (template == null) {
            throw new IllegalArgumentException("小程序我的模板不存在");
        }
        return template;
    }

    @Transactional
    public boolean myTempSave(WechatProgramMyTempRequest request) {
        validateMyTemp(request);
        WechatProgramMyTemp template = new WechatProgramMyTemp();
        copyMyTemp(template, request);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        return myTempMapper.insert(template) > 0;
    }

    @Transactional
    public boolean myTempUpdate(Integer id, WechatProgramMyTempRequest request) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("模板id不能为空");
        }
        myTempInfo(id);
        validateMyTemp(request);
        WechatProgramMyTemp update = new WechatProgramMyTemp();
        update.setId(id);
        copyMyTemp(update, request);
        update.setUpdateTime(LocalDateTime.now());
        return myTempMapper.updateById(update) > 0;
    }

    @Transactional
    public boolean myTempStatus(Integer id, Boolean status) {
        myTempInfo(id);
        WechatProgramMyTemp update = new WechatProgramMyTemp();
        update.setId(id);
        update.setStatus(Boolean.TRUE.equals(status));
        update.setUpdateTime(LocalDateTime.now());
        return myTempMapper.updateById(update) > 0;
    }

    @Transactional
    public boolean myTempType(Integer id, String type) {
        myTempInfo(id);
        WechatProgramMyTemp update = new WechatProgramMyTemp();
        update.setId(id);
        update.setType(trim(type));
        update.setUpdateTime(LocalDateTime.now());
        return myTempMapper.updateById(update) > 0;
    }

    @Transactional
    public boolean myTempDelete(Integer id) {
        myTempInfo(id);
        return myTempMapper.deleteById(id) > 0;
    }

    public Map<String, Object> safeSync(String title) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", false);
        result.put("localMode", true);
        result.put("message", title + "属于微信平台外部同步能力，当前未触发真实第三方请求。");
        return result;
    }

    private void validateTemplate(WechatTemplateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("模板参数不能为空");
        }
        if (!StringUtils.hasText(request.getTempKey())) {
            throw new IllegalArgumentException("模板编号不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("模板名不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("模板内容不能为空");
        }
    }

    private void copyTemplate(TemplateMessage target, WechatTemplateRequest request) {
        target.setType(Boolean.TRUE.equals(request.getType()));
        target.setTempKey(trim(request.getTempKey()));
        target.setName(trim(request.getName()));
        target.setContent(trim(request.getContent()));
        target.setTempId(trim(request.getTempId()));
        target.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private void validateMyTemp(WechatProgramMyTempRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("模板参数不能为空");
        }
        if (request.getTid() == null || request.getTid() <= 0) {
            throw new IllegalArgumentException("微信模板tid不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("模板标题不能为空");
        }
    }

    private void copyMyTemp(WechatProgramMyTemp target, WechatProgramMyTempRequest request) {
        target.setTid(request.getTid());
        target.setTitle(trim(request.getTitle()));
        target.setKid(trim(request.getKid()));
        target.setSceneDesc(trim(request.getSceneDesc()));
        target.setTempId(trim(request.getTempId()));
        target.setExtra(trim(request.getExtra()));
        target.setStatus(request.getStatus() == null || request.getStatus());
        target.setType(trim(request.getType()));
    }

    private Map<String, Object> oldPage(List<?> records, long total, int page, int limit) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", records);
        result.put("data", records);
        result.put("total", total);
        result.put("count", total);
        result.put("page", page);
        result.put("limit", limit);
        result.put("totalPage", limit <= 0 ? 0 : (int) Math.ceil((double) total / limit));
        return result;
    }

    private int safePage(Integer page) {
        return page == null || page <= 0 ? 1 : page;
    }

    private int safeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 20 : Math.min(limit, 200);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
