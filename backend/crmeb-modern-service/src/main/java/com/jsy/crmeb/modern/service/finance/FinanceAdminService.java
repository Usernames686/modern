package com.jsy.crmeb.modern.service.finance;

import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.finance.dto.BrokerageRecordRequest;
import com.jsy.crmeb.modern.service.finance.dto.BrokerageRecordResponse;
import com.jsy.crmeb.modern.service.finance.dto.ExtractBalanceResponse;
import com.jsy.crmeb.modern.service.finance.dto.FundsMonitorRequest;
import com.jsy.crmeb.modern.service.finance.dto.MonitorResponse;
import com.jsy.crmeb.modern.service.finance.dto.UserExtractResponse;
import com.jsy.crmeb.modern.service.finance.dto.UserExtractSearchRequest;
import com.jsy.crmeb.modern.service.finance.dto.UserExtractUpdateRequest;
import com.jsy.crmeb.modern.service.finance.dto.UserRechargeResponse;
import com.jsy.crmeb.modern.service.finance.dto.UserRechargeSearchRequest;
import com.jsy.crmeb.modern.service.finance.mapper.UserBillAdminMapper;
import com.jsy.crmeb.modern.service.finance.mapper.UserBrokerageAdminMapper;
import com.jsy.crmeb.modern.service.finance.mapper.UserExtractAdminMapper;
import com.jsy.crmeb.modern.service.finance.mapper.UserRechargeAdminMapper;
import com.jsy.crmeb.modern.service.user.entity.UserBrokerageRecord;
import com.jsy.crmeb.modern.service.user.mapper.UserBrokerageRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FinanceAdminService {
    private final UserRechargeAdminMapper rechargeMapper;
    private final UserBillAdminMapper billMapper;
    private final UserBrokerageAdminMapper brokerageMapper;
    private final UserExtractAdminMapper extractMapper;
    private final UserBrokerageRecordMapper brokerageRecordMapper;

    public FinanceAdminService(
            UserRechargeAdminMapper rechargeMapper,
            UserBillAdminMapper billMapper,
            UserBrokerageAdminMapper brokerageMapper,
            UserExtractAdminMapper extractMapper,
            UserBrokerageRecordMapper brokerageRecordMapper) {
        this.rechargeMapper = rechargeMapper;
        this.billMapper = billMapper;
        this.brokerageMapper = brokerageMapper;
        this.extractMapper = extractMapper;
        this.brokerageRecordMapper = brokerageRecordMapper;
    }

    public PageResponse<UserRechargeResponse> rechargeList(UserRechargeSearchRequest request) {
        UserRechargeSearchRequest safeRequest = request == null ? new UserRechargeSearchRequest() : request;
        int page = safeRequest.getPage() == null || safeRequest.getPage() <= 0 ? 1 : safeRequest.getPage();
        int limit = safeRequest.getLimit() == null || safeRequest.getLimit() <= 0 ? 20 : Math.min(safeRequest.getLimit(), 100);
        String keywords = trimToNull(safeRequest.getKeywords());
        String[] dateRange = parseDateLimit(safeRequest.getDateLimit());
        long total = rechargeMapper.countRecharge(safeRequest.getUid(), keywords, dateRange[0], dateRange[1]);
        List<UserRechargeResponse> list = total <= 0
                ? List.of()
                : rechargeMapper.selectRecharge(
                        safeRequest.getUid(),
                        keywords,
                        dateRange[0],
                        dateRange[1],
                        (page - 1) * limit,
                        limit);
        list.forEach(item -> item.setAvatar(normalizeAsset(item.getAvatar())));
        return new PageResponse<>(page, limit, total, list);
    }

    public PageResponse<BrokerageRecordResponse> brokerageRecords(BrokerageRecordRequest request) {
        BrokerageRecordRequest safeRequest = request == null ? new BrokerageRecordRequest() : request;
        int page = safeRequest.getPage() == null || safeRequest.getPage() <= 0 ? 1 : safeRequest.getPage();
        int limit = safeRequest.getLimit() == null || safeRequest.getLimit() <= 0 ? 20 : Math.min(safeRequest.getLimit(), 100);
        Integer type = brokerageFilterType(safeRequest.getType());
        long total = brokerageMapper.countBrokerage(type);
        List<BrokerageRecordResponse> list = total <= 0
                ? List.of()
                : brokerageMapper.selectBrokerage(type, (page - 1) * limit, limit);
        return new PageResponse<>(page, limit, total, list);
    }

    public PageResponse<UserExtractResponse> extractList(UserExtractSearchRequest request) {
        UserExtractSearchRequest safeRequest = request == null ? new UserExtractSearchRequest() : request;
        int page = safeRequest.getPage() == null || safeRequest.getPage() <= 0 ? 1 : safeRequest.getPage();
        int limit = safeRequest.getLimit() == null || safeRequest.getLimit() <= 0 ? 20 : Math.min(safeRequest.getLimit(), 100);
        String keywords = trimToNull(safeRequest.getKeywords());
        String extractType = extractType(safeRequest.getExtractType());
        Integer status = extractStatus(safeRequest.getStatus());
        String[] dateRange = parseDateLimit(safeRequest.getDateLimit());
        long total = extractMapper.countExtracts(keywords, extractType, status, dateRange[0], dateRange[1]);
        List<UserExtractResponse> list = total <= 0
                ? List.of()
                : extractMapper.selectExtracts(keywords, extractType, status, dateRange[0], dateRange[1], (page - 1) * limit, limit);
        list.forEach(item -> item.setQrcodeUrl(normalizeAsset(item.getQrcodeUrl())));
        return new PageResponse<>(page, limit, total, list);
    }

    public ExtractBalanceResponse extractBalance(String dateLimit) {
        String[] dateRange = parseDateLimit(dateLimit);
        BigDecimal withdrawn = nvl(extractMapper.sumExtractByStatus(1, dateRange[0], dateRange[1]));
        BigDecimal toBeWithdrawn = nvl(extractMapper.sumExtractByStatus(0, dateRange[0], dateRange[1]));
        BigDecimal commissionTotal = nvl(extractMapper.sumCompletedOrderBrokerage(dateRange[0], dateRange[1]));
        BigDecimal subWithdraw = nvl(extractMapper.sumCompletedSubBrokerage(dateRange[0], dateRange[1]));
        return new ExtractBalanceResponse(withdrawn, commissionTotal.subtract(subWithdraw), commissionTotal, toBeWithdrawn);
    }

    @Transactional
    public void updateExtract(UserExtractUpdateRequest request) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("提现记录不存在");
        }
        if (!StringUtils.hasText(request.getExtractType())) {
            throw new IllegalArgumentException("请选择提现方式");
        }
        extractType(request.getExtractType());
        if (request.getExtractPrice() == null || request.getExtractPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("提现金额必须大于0");
        }
        if (extractMapper.updateExtract(request) != 1) {
            throw new IllegalArgumentException("提现记录不存在");
        }
    }

    @Transactional
    public void applyExtract(Integer id, Integer status, String backMessage) {
        if (id == null) {
            throw new IllegalArgumentException("提现记录不存在");
        }
        if (status == null || (status != -1 && status != 1)) {
            throw new IllegalArgumentException("请选择正确的审核状态");
        }
        if (status == -1 && !StringUtils.hasText(backMessage)) {
            throw new IllegalArgumentException("驳回时请填写驳回原因");
        }
        UserExtractResponse extract = extractMapper.selectExtractForUpdate(id);
        if (extract == null) {
            throw new IllegalArgumentException("提现申请记录不存在");
        }
        if (extract.getStatus() == null || extract.getStatus() != 0) {
            throw new IllegalArgumentException("提现申请已处理过");
        }
        if (status == -1) {
            rejectExtract(extract, backMessage.trim());
        } else {
            passExtract(extract);
        }
    }

    public Map<String, BigDecimal> rechargeBalance() {
        BigDecimal routine = nvl(rechargeMapper.sumByType("routine"));
        BigDecimal weChat = nvl(rechargeMapper.sumByType("public"));
        BigDecimal total = nvl(rechargeMapper.sumByType(""));
        BigDecimal refund = nvl(rechargeMapper.sumRefund());
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        result.put("routine", routine);
        result.put("weChat", weChat);
        result.put("total", total);
        result.put("refund", refund);
        result.put("other", total.subtract(routine).subtract(weChat));
        return result;
    }

    public PageResponse<MonitorResponse> fundMonitorList(FundsMonitorRequest request) {
        FundsMonitorRequest safeRequest = request == null ? new FundsMonitorRequest() : request;
        int page = safeRequest.getPage() == null || safeRequest.getPage() <= 0 ? 1 : safeRequest.getPage();
        int limit = safeRequest.getLimit() == null || safeRequest.getLimit() <= 0 ? 20 : Math.min(safeRequest.getLimit(), 100);
        String keywords = trimToNull(safeRequest.getKeywords());
        String title = fundMonitorTitle(safeRequest.getTitle());
        String[] dateRange = parseDateLimit(safeRequest.getDateLimit());
        long total = billMapper.countFundMonitor(keywords, title, dateRange[0], dateRange[1]);
        List<MonitorResponse> list = total <= 0
                ? List.of()
                : billMapper.selectFundMonitor(keywords, title, dateRange[0], dateRange[1], (page - 1) * limit, limit);
        return new PageResponse<>(page, limit, total, list);
    }

    private void rejectExtract(UserExtractResponse extract, String backMessage) {
        BigDecimal currentBrokerage = nvl(brokerageRecordMapper.selectUserBrokeragePriceForUpdate(extract.getUid()));
        BigDecimal balance = currentBrokerage.add(nvl(extract.getExtractPrice()));
        if (brokerageRecordMapper.updateUserBrokeragePrice(extract.getUid(), balance) != 1) {
            throw new IllegalArgumentException("提现用户数据异常");
        }
        if (extractMapper.rejectExtract(extract.getId(), backMessage) != 1) {
            throw new IllegalArgumentException("提现申请已处理过");
        }
        UserBrokerageRecord record = new UserBrokerageRecord();
        record.setUid(extract.getUid());
        record.setLinkId(String.valueOf(extract.getId()));
        record.setLinkType("withdraw");
        record.setType(1);
        record.setTitle("提现失败");
        record.setPrice(nvl(extract.getExtractPrice()));
        record.setBalance(balance);
        record.setMark("提现申请拒绝返还佣金" + moneyPlain(extract.getExtractPrice()));
        record.setStatus(3);
        record.setFrozenTime(0);
        record.setThawTime(0L);
        record.setCreateTime(LocalDateTime.now());
        brokerageRecordMapper.insert(record);
    }

    private void passExtract(UserExtractResponse extract) {
        Integer brokerageId = extractMapper.selectWithdrawBrokerageIdForUpdate(String.valueOf(extract.getId()));
        if (brokerageId == null) {
            throw new IllegalArgumentException("对应的佣金记录不存在");
        }
        if (extractMapper.passExtract(extract.getId()) != 1) {
            throw new IllegalArgumentException("提现申请已处理过");
        }
        if (extractMapper.markBrokerageComplete(brokerageId) != 1) {
            throw new IllegalArgumentException("佣金记录更新失败");
        }
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

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String fundMonitorTitle(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return switch (value.trim()) {
            case "recharge" -> "充值支付";
            case "admin" -> "后台操作";
            case "productRefund" -> "商品退款";
            case "payProduct" -> "购买商品";
            default -> throw new IllegalArgumentException("请选择正确的明细类型");
        };
    }

    private String extractType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return switch (value.trim()) {
            case "bank", "alipay", "weixin" -> value.trim();
            default -> throw new IllegalArgumentException("请选择正确的提现方式");
        };
    }

    private Integer extractStatus(Integer value) {
        if (value == null) {
            return null;
        }
        if (value == -1 || value == 0 || value == 1) {
            return value;
        }
        throw new IllegalArgumentException("请选择正确的提现状态");
    }

    private Integer brokerageFilterType(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("未知的类型");
        }
        return value;
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

    private String moneyPlain(BigDecimal value) {
        return nvl(value).stripTrailingZeros().toPlainString();
    }
}
