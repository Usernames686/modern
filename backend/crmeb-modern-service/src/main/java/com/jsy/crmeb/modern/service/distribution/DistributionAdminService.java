package com.jsy.crmeb.modern.service.distribution;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.distribution.dto.RetailShopConfigRequest;
import com.jsy.crmeb.modern.service.distribution.dto.SpreadChildUserResponse;
import com.jsy.crmeb.modern.service.distribution.dto.SpreadOrderResponse;
import com.jsy.crmeb.modern.service.distribution.dto.SpreadUserRequest;
import com.jsy.crmeb.modern.service.distribution.dto.SpreadUserResponse;
import com.jsy.crmeb.modern.service.distribution.mapper.DistributionAdminMapper;
import com.jsy.crmeb.modern.service.system.entity.SystemConfig;
import com.jsy.crmeb.modern.service.system.mapper.SystemConfigMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DistributionAdminService {
    private final SystemConfigMapper configMapper;
    private final DistributionAdminMapper distributionMapper;

    public DistributionAdminService(SystemConfigMapper configMapper, DistributionAdminMapper distributionMapper) {
        this.configMapper = configMapper;
        this.distributionMapper = distributionMapper;
    }

    public RetailShopConfigRequest manageInfo() {
        RetailShopConfigRequest response = new RetailShopConfigRequest();
        response.setBrokerageFuncStatus(intConfig("brokerage_func_status", 0));
        response.setStoreBrokerageRatio(intConfig("store_brokerage_ratio", 0));
        response.setStoreBrokerageTwo(intConfig("store_brokerage_two", 0));
        response.setUserExtractMinPrice(decimalConfig("user_extract_min_price", BigDecimal.ZERO));
        response.setUserExtractBank(stringConfig("user_extract_bank", "").replace("\\n", "\n"));
        response.setExtractTime(intConfig("extract_time", 0));
        response.setStoreBrokerageQuota(intConfig("store_brokerage_quota", -1));
        response.setStoreBrokerageIsBubble(intConfig("store_brokerage_is_bubble", 0));
        response.setBrokerageBindind(intConfig("brokerage_bindind", 0));
        return response;
    }

    public PageResponse<SpreadUserResponse> promoterList(String keywords, String dateLimit, Integer page, Integer limit) {
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        String cleanKeywords = trimToNull(keywords);
        String[] range = parseDateLimit(dateLimit);
        long total = distributionMapper.countPromoters(cleanKeywords, range[0], range[1]);
        List<SpreadUserResponse> list = total <= 0
                ? List.of()
                : distributionMapper.selectPromoters(cleanKeywords, range[0], range[1], (safePage - 1) * safeLimit, safeLimit);
        list.forEach(item -> item.setAvatar(normalizeAsset(item.getAvatar())));
        return new PageResponse<>(safePage, safeLimit, total, list);
    }

    public PageResponse<SpreadChildUserResponse> spreadUserList(SpreadUserRequest request, Integer page, Integer limit) {
        SpreadUserRequest safeRequest = request == null ? new SpreadUserRequest() : request;
        validateSpreadRequest(safeRequest);
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 10 : Math.min(limit, 100);
        String keywords = trimToNull(safeRequest.getNickName());
        long total = distributionMapper.countSpreadUsers(safeRequest.getUid(), safeRequest.getType(), keywords);
        List<SpreadChildUserResponse> list = total <= 0
                ? List.of()
                : distributionMapper.selectSpreadUsers(safeRequest.getUid(), safeRequest.getType(), keywords, (safePage - 1) * safeLimit, safeLimit);
        list.forEach(item -> item.setAvatar(normalizeAsset(item.getAvatar())));
        return new PageResponse<>(safePage, safeLimit, total, list);
    }

    public PageResponse<SpreadOrderResponse> spreadOrderList(SpreadUserRequest request, Integer page, Integer limit) {
        SpreadUserRequest safeRequest = request == null ? new SpreadUserRequest() : request;
        validateSpreadRequest(safeRequest);
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 10 : Math.min(limit, 100);
        String keywords = trimToNull(safeRequest.getNickName());
        String[] range = parseDateLimit(safeRequest.getDateLimit());
        long total = distributionMapper.countSpreadOrders(safeRequest.getUid(), safeRequest.getType(), keywords, range[0], range[1]);
        List<SpreadOrderResponse> list = total <= 0
                ? List.of()
                : distributionMapper.selectSpreadOrders(safeRequest.getUid(), safeRequest.getType(), keywords, range[0], range[1], (safePage - 1) * safeLimit, safeLimit);
        return new PageResponse<>(safePage, safeLimit, total, list);
    }

    @Transactional
    public boolean clearSpread(Integer uid) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("用户id不能为空");
        }
        Integer spreadUid = distributionMapper.selectSpreadUid(uid);
        if (spreadUid == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        distributionMapper.clearSpread(uid);
        if (spreadUid > 0) {
            distributionMapper.decreaseSpreadCount(spreadUid);
        }
        return true;
    }

    @Transactional
    public boolean saveManageInfo(RetailShopConfigRequest request) {
        validate(request);
        upsert("brokerage_func_status", String.valueOf(request.getBrokerageFuncStatus()));
        upsert("store_brokerage_ratio", String.valueOf(request.getStoreBrokerageRatio()));
        upsert("store_brokerage_two", String.valueOf(request.getStoreBrokerageTwo()));
        upsert("user_extract_min_price", request.getUserExtractMinPrice().stripTrailingZeros().toPlainString());
        upsert("user_extract_bank", trim(request.getUserExtractBank()));
        upsert("extract_time", String.valueOf(request.getExtractTime()));
        upsert("brokerage_bindind", String.valueOf(request.getBrokerageBindind()));
        upsert("store_brokerage_quota", String.valueOf(request.getStoreBrokerageQuota()));
        upsert("store_brokerage_is_bubble", String.valueOf(request.getStoreBrokerageIsBubble()));
        return true;
    }

    private void validate(RetailShopConfigRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        requireSwitch(request.getBrokerageFuncStatus(), "请选择是否启用分销");
        requireSwitch(request.getBrokerageBindind(), "请选择分销关系绑定");
        requireSwitch(request.getStoreBrokerageIsBubble(), "请选择是否展示分销气泡");
        if (request.getStoreBrokerageQuota() == null || request.getStoreBrokerageQuota() < -1) {
            throw new IllegalArgumentException("满额分销最低金额不能小于-1");
        }
        if (request.getStoreBrokerageRatio() == null || request.getStoreBrokerageRatio() < 0 || request.getStoreBrokerageRatio() > 100) {
            throw new IllegalArgumentException("一级返佣比例请在0-100中选择");
        }
        if (request.getStoreBrokerageTwo() == null || request.getStoreBrokerageTwo() < 0 || request.getStoreBrokerageTwo() > 100) {
            throw new IllegalArgumentException("二级返佣比例请在0-100中选择");
        }
        if (request.getStoreBrokerageRatio() + request.getStoreBrokerageTwo() > 100) {
            throw new IllegalArgumentException("返佣比例加起来不能超过100%");
        }
        if (request.getUserExtractMinPrice() == null || request.getUserExtractMinPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("用户提现最低金额最小为0");
        }
        if (!StringUtils.hasText(request.getUserExtractBank())) {
            throw new IllegalArgumentException("提现银行卡不能为空");
        }
        if (request.getExtractTime() == null || request.getExtractTime() < 0) {
            throw new IllegalArgumentException("冻结时间最少为0天");
        }
    }

    private void validateSpreadRequest(SpreadUserRequest request) {
        if (request.getUid() == null || request.getUid() <= 0) {
            throw new IllegalArgumentException("用户id不能为空");
        }
        if (request.getType() == null) {
            request.setType(0);
        }
        if (request.getType() < 0 || request.getType() > 2) {
            throw new IllegalArgumentException("请选择正确的用户类型");
        }
    }

    private void requireSwitch(Integer value, String message) {
        if (value == null || (value != 0 && value != 1)) {
            throw new IllegalArgumentException(message);
        }
    }

    private String stringConfig(String name, String fallback) {
        SystemConfig config = configByName(name);
        return config == null || config.getValue() == null ? fallback : config.getValue();
    }

    private String[] parseDateLimit(String dateLimit) {
        if (!StringUtils.hasText(dateLimit)) {
            return new String[] { null, null };
        }
        LocalDate today = LocalDate.now();
        return switch (dateLimit.trim()) {
            case "today" -> range(today, today);
            case "yesterday" -> range(today.minusDays(1), today.minusDays(1));
            case "lately7" -> range(today.minusDays(6), today);
            case "lately30" -> range(today.minusDays(29), today);
            case "month" -> range(today.withDayOfMonth(1), today);
            case "year" -> range(today.withDayOfYear(1), today);
            default -> parseCustomDateLimit(dateLimit);
        };
    }

    private String[] parseCustomDateLimit(String dateLimit) {
        String[] parts = dateLimit.trim().split("\\s*,\\s*|\\s+-\\s+");
        if (parts.length < 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            return new String[] { null, null };
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.parse(parts[0].trim().substring(0, 10), formatter);
        LocalDate endDate = LocalDate.parse(parts[1].trim().substring(0, 10), formatter);
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("开始时间不能大于结束时间！");
        }
        return range(startDate, endDate);
    }

    private String[] range(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);
        return new String[] { start.toString().replace('T', ' '), end.toString().replace('T', ' ') };
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

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private int intConfig(String name, int fallback) {
        String value = stringConfig(name, String.valueOf(fallback));
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private BigDecimal decimalConfig(String name, BigDecimal fallback) {
        String value = stringConfig(name, fallback.toPlainString());
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private void upsert(String name, String value) {
        SystemConfig config = configByName(name);
        LocalDateTime now = LocalDateTime.now();
        if (config == null) {
            config = new SystemConfig();
            config.setName(name);
            config.setTitle(name);
            config.setFormId(0);
            config.setStatus(false);
            config.setCreateTime(now);
        }
        config.setValue(value);
        config.setUpdateTime(now);
        if (config.getId() == null) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
    }

    private SystemConfig configByName(String name) {
        QueryWrapper<SystemConfig> query = new QueryWrapper<>();
        query.eq("name", name).orderByDesc("id").last("limit 1");
        return configMapper.selectOne(query);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
