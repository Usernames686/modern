package com.jsy.crmeb.modern.service.marketing;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.marketing.dto.ActivityStyleRequest;
import com.jsy.crmeb.modern.service.marketing.dto.ActivityStyleResponse;
import com.jsy.crmeb.modern.service.marketing.entity.ActivityStyle;
import com.jsy.crmeb.modern.service.marketing.mapper.ActivityStyleMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ActivityStyleAdminService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ActivityStyleMapper activityStyleMapper;

    public ActivityStyleAdminService(ActivityStyleMapper activityStyleMapper) {
        this.activityStyleMapper = activityStyleMapper;
    }

    public PageResponse<ActivityStyleResponse> list(
            Integer page,
            Integer limit,
            Boolean type,
            String name,
            Boolean status,
            Integer runningStatus,
            Integer method,
            String starttime,
            String endtime) {
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 200);
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<ActivityStyle> query = new QueryWrapper<>();
        if (type != null) {
            query.eq("type", type);
        }
        if (StringUtils.hasText(name)) {
            query.like("name", name.trim());
        }
        if (status != null) {
            query.eq("status", status);
        }
        if (method != null) {
            query.eq("method", method);
        }
        LocalDateTime start = parseOptionalDateTime(starttime, "开始时间格式错误");
        LocalDateTime end = parseOptionalDateTime(endtime, "结束时间格式错误");
        if (start != null && end != null) {
            query.between("createtime", start, end);
        }
        if (runningStatus != null) {
            switch (runningStatus) {
                case -1 -> query.lt("endtime", now);
                case 0 -> query.gt("starttime", now);
                case 1 -> query.ge("endtime", now).le("starttime", now);
                default -> throw new IllegalArgumentException("活动状态不正确");
            }
        }
        query.orderByDesc("createtime").orderByDesc("id");
        Page<ActivityStyle> result = activityStyleMapper.selectPage(new Page<>(safePage, safeLimit), query);
        List<ActivityStyleResponse> records = result.getRecords().stream()
                .map(item -> ActivityStyleResponse.from(item, now))
                .toList();
        return new PageResponse<>(safePage, safeLimit, result.getTotal(), records);
    }

    public ActivityStyleResponse info(Integer id) {
        return ActivityStyleResponse.from(required(id), LocalDateTime.now());
    }

    @Transactional
    public boolean save(ActivityStyleRequest request) {
        ActivityStyle style = new ActivityStyle();
        copy(style, request, false);
        LocalDateTime now = LocalDateTime.now();
        style.setCreatetime(now);
        style.setUpdatetime(now);
        return activityStyleMapper.insert(style) > 0;
    }

    @Transactional
    public boolean update(ActivityStyleRequest request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new IllegalArgumentException("活动id不能为空");
        }
        required(request.getId());
        ActivityStyle style = new ActivityStyle();
        style.setId(request.getId());
        copy(style, request, true);
        style.setUpdatetime(LocalDateTime.now());
        return activityStyleMapper.updateById(style) > 0;
    }

    @Transactional
    public boolean delete(Integer id) {
        required(id);
        return activityStyleMapper.deleteById(id) > 0;
    }

    @Transactional
    public boolean updateStatus(Integer id, Boolean status) {
        required(id);
        ActivityStyle style = new ActivityStyle();
        style.setId(id);
        style.setStatus(Boolean.TRUE.equals(status));
        style.setUpdatetime(LocalDateTime.now());
        return activityStyleMapper.updateById(style) > 0;
    }

    private void copy(ActivityStyle target, ActivityStyleRequest request, boolean update) {
        if (request == null) {
            throw new IllegalArgumentException("活动参数不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("活动名称不能为空");
        }
        if (!StringUtils.hasText(request.getStyle())) {
            throw new IllegalArgumentException("活动素材不能为空");
        }
        if (request.getMethod() == null || request.getMethod() < 0 || request.getMethod() > 4) {
            throw new IllegalArgumentException("活动参与类型不能为空");
        }
        LocalDateTime start = parseRequiredDateTime(request.getStarttime(), "开始时间不能为空");
        LocalDateTime end = parseRequiredDateTime(request.getEndtime(), "结束时间不能为空");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("结束时间必须大于开始时间");
        }
        target.setName(request.getName().trim());
        target.setType(Boolean.TRUE.equals(request.getType()));
        target.setStarttime(start);
        target.setEndtime(end);
        target.setStyle(clearPrefix(request.getStyle()));
        target.setStatus(Boolean.TRUE.equals(request.getStatus()));
        target.setMethod(request.getMethod());
        target.setProducts(StringUtils.hasText(request.getProducts()) ? request.getProducts().trim() : "");
    }

    private ActivityStyle required(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("活动id不能为空");
        }
        ActivityStyle style = activityStyleMapper.selectById(id);
        if (style == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        return style;
    }

    private LocalDateTime parseRequiredDateTime(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return parseOptionalDateTime(value, "时间格式错误");
    }

    private LocalDateTime parseOptionalDateTime(String value, String message) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().replace("T", " ");
        if (normalized.length() > 19) {
            normalized = normalized.substring(0, 19);
        }
        try {
            return LocalDateTime.parse(normalized, FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(message);
        }
    }

    private String clearPrefix(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String result = value.trim();
        int index = result.indexOf("crmebimage/");
        if (index >= 0) {
            return result.substring(index);
        }
        return result.startsWith("/") ? result.substring(1) : result;
    }
}
