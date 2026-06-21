package com.jsy.crmeb.modern.service.front;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.marketing.entity.StoreSeckill;
import com.jsy.crmeb.modern.service.marketing.entity.StoreSeckillManger;
import com.jsy.crmeb.modern.service.marketing.mapper.StoreSeckillMangerMapper;
import com.jsy.crmeb.modern.service.marketing.mapper.StoreSeckillMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontSeckillService {
    private final StoreSeckillMangerMapper mangerMapper;
    private final StoreSeckillMapper seckillMapper;
    private final FrontUserCenterService userCenterService;
    private final ObjectMapper objectMapper;

    public FrontSeckillService(
            StoreSeckillMangerMapper mangerMapper,
            StoreSeckillMapper seckillMapper,
            FrontUserCenterService userCenterService,
            ObjectMapper objectMapper) {
        this.mangerMapper = mangerMapper;
        this.seckillMapper = seckillMapper;
        this.userCenterService = userCenterService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> header() {
        int currentHour = LocalDateTime.now().getHour();
        List<StoreSeckillManger> managers = mangerMapper.selectList(new QueryWrapper<StoreSeckillManger>()
                .eq("is_del", 0)
                .eq("status", "'1'")
                .gt("end_time", currentHour)
                .orderByAsc("start_time")
                .orderByAsc("id"));
        List<Map<String, Object>> headers = managers.stream()
                .filter(manager -> countVisibleProducts(manager.getId()) > 0)
                .map(manager -> toManagerItem(manager, currentHour))
                .toList();
        return headers.isEmpty() ? fallbackHeader() : headers;
    }

    private List<Map<String, Object>> fallbackHeader() {
        int currentHour = LocalDateTime.now().getHour();
        List<StoreSeckillManger> managers = mangerMapper.selectList(new QueryWrapper<StoreSeckillManger>()
                .eq("is_del", 0)
                .eq("status", "'1'")
                .orderByAsc("start_time")
                .orderByAsc("id"));
        return managers.stream()
                .filter(manager -> countDisplayProducts(manager.getId()) > 0)
                .map(manager -> toManagerItem(manager, currentHour, true))
                .toList();
    }

    public Map<String, Object> index() {
        List<Map<String, Object>> headers = header();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("seckillList", headers);
        response.put("lovely", List.of());
        return response;
    }

    public PageResponse<Map<String, Object>> list(Integer timeId, int page, int limit) {
        if (timeId == null || timeId <= 0) {
            throw new IllegalArgumentException("秒杀时段不能为空");
        }
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        Page<StoreSeckill> result = seckillMapper.selectPage(new Page<>(safePage, safeLimit), visibleSeckillQuery()
                .eq("time_id", timeId)
                .orderByDesc("id"));
        if (result.getTotal() == 0) {
            result = seckillMapper.selectPage(new Page<>(safePage, safeLimit), displaySeckillQuery()
                    .eq("time_id", timeId)
                    .orderByDesc("id"));
        }
        List<Map<String, Object>> rows = result.getRecords().stream().map(this::toSeckillItem).toList();
        return new PageResponse<>(safePage, safeLimit, result.getTotal(), rows);
    }

    public Map<String, Object> detail(Integer id) {
        return detail(id, null);
    }

    public Map<String, Object> detail(Integer id, Integer uid) {
        StoreSeckill seckill = seckillMapper.selectById(id);
        if (seckill == null || (!isVisibleSeckill(seckill) && !isDisplaySeckill(seckill))) {
            throw new IllegalArgumentException("秒杀商品不存在或已下架");
        }
        if (uid != null) {
            userCenterService.recordVisit(uid, 3);
        }
        return toSeckillItem(seckill);
    }

    private long countVisibleProducts(Integer timeId) {
        return seckillMapper.selectCount(visibleSeckillQuery().eq("time_id", timeId));
    }

    private long countDisplayProducts(Integer timeId) {
        return seckillMapper.selectCount(displaySeckillQuery().eq("time_id", timeId));
    }

    private QueryWrapper<StoreSeckill> visibleSeckillQuery() {
        LocalDate today = LocalDate.now();
        return new QueryWrapper<StoreSeckill>()
                .eq("status", 1)
                .eq("is_del", 0)
                .eq("is_show", 1)
                .le("date(start_time)", today)
                .ge("date(stop_time)", today);
    }

    private QueryWrapper<StoreSeckill> displaySeckillQuery() {
        return new QueryWrapper<StoreSeckill>()
                .eq("is_del", 0)
                .eq("is_show", 1)
                .gt("stock", 0)
                .gt("quota", 0);
    }

    private boolean isVisibleSeckill(StoreSeckill item) {
        LocalDate today = LocalDate.now();
        return Integer.valueOf(1).equals(item.getStatus())
                && !Boolean.TRUE.equals(item.getIsDel())
                && Boolean.TRUE.equals(item.getIsShow())
                && item.getStartTime() != null
                && item.getStopTime() != null
                && !item.getStartTime().toLocalDate().isAfter(today)
                && !item.getStopTime().toLocalDate().isBefore(today);
    }

    private boolean isDisplaySeckill(StoreSeckill item) {
        return !Boolean.TRUE.equals(item.getIsDel())
                && Boolean.TRUE.equals(item.getIsShow())
                && item.getStock() != null
                && item.getStock() > 0
                && item.getQuota() != null
                && item.getQuota() > 0;
    }

    private Map<String, Object> toManagerItem(StoreSeckillManger manager, int currentHour) {
        return toManagerItem(manager, currentHour, false);
    }

    private Map<String, Object> toManagerItem(StoreSeckillManger manager, int currentHour, boolean fallback) {
        Map<String, Object> item = new LinkedHashMap<>();
        int start = manager.getStartTime() == null ? 0 : manager.getStartTime();
        int end = manager.getEndTime() == null ? 0 : manager.getEndTime();
        int status = fallback ? 3 : currentHour < start ? 1 : 2;
        item.put("id", manager.getId());
        item.put("name", manager.getName());
        item.put("time", String.format("%02d:00,%02d:00", start, end));
        item.put("startTime", start);
        item.put("endTime", end);
        item.put("status", status);
        item.put("statusName", fallback ? "往期活动" : status == 2 ? "抢购中" : "即将开始");
        item.put("killStatus", status);
        item.put("isCheck", fallback || (start <= currentHour && currentHour < end));
        item.put("fallback", fallback);
        item.put("slide", slideList(manager.getSilderImgs()));
        item.put("silderImgs", manager.getSilderImgs());
        item.put("stop", LocalDate.now() + String.format(" %02d:00:00", end));
        return item;
    }

    private Map<String, Object> toSeckillItem(StoreSeckill item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", item.getId());
        row.put("productId", item.getProductId());
        row.put("image", item.getImage());
        row.put("title", item.getTitle());
        row.put("price", money(item.getPrice()));
        row.put("otPrice", money(item.getOtPrice()));
        row.put("unitName", StringUtils.hasText(item.getUnitName()) ? item.getUnitName() : "件");
        row.put("quota", item.getQuota());
        row.put("quotaShow", item.getQuotaShow());
        row.put("stock", item.getStock());
        row.put("percent", percent(item.getQuotaShow(), item.getQuota()));
        row.put("timeId", item.getTimeId());
        row.put("startTime", item.getStartTime());
        row.put("stopTime", item.getStopTime());
        return row;
    }

    private List<Map<String, Object>> slideList(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private int percent(Integer quotaShow, Integer quota) {
        int total = quotaShow == null ? 0 : quotaShow;
        if (total <= 0) {
            return 0;
        }
        int left = quota == null ? 0 : quota;
        int sold = Math.max(0, total - left);
        return BigDecimal.valueOf(sold)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
