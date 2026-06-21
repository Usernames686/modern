package com.jsy.crmeb.modern.service.dashboard;

import com.jsy.crmeb.modern.service.dashboard.dto.DashboardChartPoint;
import com.jsy.crmeb.modern.service.dashboard.dto.HomeOperatingDataResponse;
import com.jsy.crmeb.modern.service.dashboard.dto.HomeRateResponse;
import com.jsy.crmeb.modern.service.dashboard.mapper.DashboardAdminMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DashboardAdminService {
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] WEEK_LABELS = { "周一", "周二", "周三", "周四", "周五", "周六", "周日" };
    private static final String[] MONTH_LABELS = { "一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月" };
    private final DashboardAdminMapper dashboardMapper;

    public DashboardAdminService(DashboardAdminMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    public HomeRateResponse indexData() {
        String today = LocalDate.now().format(DAY_FORMATTER);
        String yesterday = LocalDate.now().minusDays(1).format(DAY_FORMATTER);
        HomeRateResponse response = new HomeRateResponse();
        response.setSales(nvl(dashboardMapper.sumPaidOrderAmountByDate(today)));
        response.setYesterdaySales(nvl(dashboardMapper.sumPaidOrderAmountByDate(yesterday)));
        response.setPageviews(nvl(dashboardMapper.countPageviewsByDate(today)));
        response.setYesterdayPageviews(nvl(dashboardMapper.countPageviewsByDate(yesterday)));
        response.setOrderNum(nvl(dashboardMapper.countPaidOrderByDate(today)));
        response.setYesterdayOrderNum(nvl(dashboardMapper.countPaidOrderByDate(yesterday)));
        response.setNewUserNum(nvl(dashboardMapper.countNewUserByDate(today)));
        response.setYesterdayNewUserNum(nvl(dashboardMapper.countNewUserByDate(yesterday)));
        return response;
    }

    public HomeOperatingDataResponse operatingData() {
        HomeOperatingDataResponse response = new HomeOperatingDataResponse();
        response.setNotShippingOrderNum(nvl(dashboardMapper.countNotShippingOrders()));
        response.setRefundingOrderNum(nvl(dashboardMapper.countRefundingOrders()));
        response.setNotWriteOffOrderNum(nvl(dashboardMapper.countNotWriteOffOrders()));
        response.setVigilanceInventoryNum(nvl(dashboardMapper.countVigilanceInventory(stockWarningValue())));
        response.setOnSaleProductNum(nvl(dashboardMapper.countOnSaleProducts()));
        response.setNotSaleProductNum(nvl(dashboardMapper.countNotSaleProducts()));
        response.setNotAuditNum(nvl(dashboardMapper.countNotAuditExtracts()));
        response.setTotalRechargeAmount(
                nvl(dashboardMapper.sumRechargeAmount()).add(nvl(dashboardMapper.sumBrokerageToBalanceAmount())));
        return response;
    }

    public Map<Object, Object> chartUser() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(30);
        List<DashboardChartPoint> points = dashboardMapper.selectNewUserTrendByDay(startTime(start), endTime(today));
        Map<String, Integer> countMap = countMap(points);
        Map<Object, Object> result = new LinkedHashMap<>();
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            String key = day.format(DAY_FORMATTER);
            result.put(key.substring(5), countMap.getOrDefault(key, 0));
        }
        return result;
    }

    public Map<String, Object> chartOrderLast30() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(30);
        List<DashboardChartPoint> points = dashboardMapper.selectOrderTrendByDay(startTime(start), endTime(today));
        Map<String, BigDecimal> priceMap = priceMap(points);
        Map<String, Integer> countMap = countMap(points);
        Map<Object, Object> price = new LinkedHashMap<>();
        Map<Object, Object> quality = new LinkedHashMap<>();
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            String key = day.format(DAY_FORMATTER);
            String label = key.substring(5);
            price.put(label, priceMap.getOrDefault(key, BigDecimal.ZERO));
            quality.put(label, countMap.getOrDefault(key, 0));
        }
        return orderChart(price, quality);
    }

    public Map<String, Object> chartOrderWeek() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);
        LocalDate preStart = start.minusWeeks(1);
        LocalDate preEnd = end.minusWeeks(1);
        return rangeOrderChart(start, end, preStart, preEnd, WEEK_LABELS);
    }

    public Map<String, Object> chartOrderMonth() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.withDayOfMonth(1);
        LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
        LocalDate preStart = start.minusMonths(1);
        LocalDate preEnd = preStart.withDayOfMonth(preStart.lengthOfMonth());
        int days = Math.max(end.getDayOfMonth(), preEnd.getDayOfMonth());
        String[] labels = new String[days];
        for (int index = 0; index < days; index++) {
            labels[index] = String.valueOf(index + 1);
        }
        return rangeOrderChart(start, end, preStart, preEnd, labels);
    }

    public Map<String, Object> chartOrderYear() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.withDayOfYear(1);
        LocalDate end = today.withMonth(12).withDayOfMonth(31);
        LocalDate preStart = start.minusYears(1);
        LocalDate preEnd = end.minusYears(1);
        List<DashboardChartPoint> points = dashboardMapper.selectOrderTrendByMonth(startTime(start), endTime(end));
        List<DashboardChartPoint> prePoints = dashboardMapper.selectOrderTrendByMonth(startTime(preStart), endTime(preEnd));
        return yearOrderChart(today.getYear(), today.getYear() - 1, points, prePoints);
    }

    public Map<String, Object> productData(String dateLimit) {
        DateRange range = dateRange(dateLimit);
        Map<String, Object> base = dashboardMapper.selectProductBaseData(toUnix(range.start()), toUnix(range.end()));
        Map<String, Object> pay = dashboardMapper.selectProductPayData(formatDateTime(range.start()), formatDateTime(range.end()));
        BigDecimal payAmount = decimal(pay.get("payAmount"));
        BigDecimal payProductNum = decimal(pay.get("payProductNum"));
        BigDecimal payUserNum = decimal(pay.get("payUserNum"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productCount", number(base.get("productCount")));
        result.put("onSaleProductNum", number(base.get("onSaleProductNum")));
        result.put("offSaleProductNum", number(base.get("offSaleProductNum")));
        result.put("stockNum", number(base.get("stockNum")));
        result.put("salesNum", number(base.get("salesNum")));
        result.put("browseNum", number(base.get("browseNum")));
        result.put("newProductNum", number(base.get("newProductNum")));
        result.put("payProductNum", payProductNum);
        result.put("payAmount", payAmount);
        result.put("payUserNum", payUserNum);
        result.put("payOrderNum", number(pay.get("payOrderNum")));
        result.put("averagePrice", payProductNum.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(2)
                : payAmount.divide(payProductNum, 2, RoundingMode.HALF_UP));
        result.put("customerPrice", payUserNum.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(2)
                : payAmount.divide(payUserNum, 2, RoundingMode.HALF_UP));
        return result;
    }

    public List<Map<String, Object>> productRanking(String dateLimit, String sort, Integer limit) {
        DateRange range = dateRange(dateLimit);
        String safeSort = ("payNum".equals(sort) || "browse".equals(sort) || "payPrice".equals(sort)) ? sort : "payPrice";
        int safeLimit = limit == null || limit <= 0 ? 10 : Math.min(limit, 50);
        return dashboardMapper.selectProductRanking(formatDateTime(range.start()), formatDateTime(range.end()), safeSort, safeLimit)
                .stream()
                .peek(row -> {
                    row.put("image", assetPath(String.valueOf(row.getOrDefault("image", ""))));
                    row.put("payPrice", decimal(row.get("payPrice")));
                    row.put("payNum", number(row.get("payNum")));
                })
                .toList();
    }

    public Map<String, Object> productTrend(String dateLimit) {
        DateRange range = dateRange(dateLimit);
        List<DashboardChartPoint> payPoints = dashboardMapper.selectProductPayTrend(formatDateTime(range.start()), formatDateTime(range.end()));
        List<DashboardChartPoint> visitPoints = dashboardMapper.selectProductVisitTrend(toUnix(range.start()), toUnix(range.end()));
        Map<String, BigDecimal> amountMap = priceMap(payPoints);
        Map<String, Integer> payNumMap = countMap(payPoints);
        Map<String, Integer> visitMap = countMap(visitPoints);
        Map<Object, Object> payAmount = new LinkedHashMap<>();
        Map<Object, Object> payNum = new LinkedHashMap<>();
        Map<Object, Object> visitNum = new LinkedHashMap<>();
        for (String day : dayLabels(range)) {
            String label = day.substring(5);
            payAmount.put(label, amountMap.getOrDefault(day, BigDecimal.ZERO));
            payNum.put(label, payNumMap.getOrDefault(day, 0));
            visitNum.put(label, visitMap.getOrDefault(day, 0));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("payAmount", payAmount);
        result.put("payNum", payNum);
        result.put("visitNum", visitNum);
        return result;
    }

    public Map<String, Object> userTotalData() {
        Map<String, Object> total = dashboardMapper.selectUserTotalData();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("registerNum", number(total.get("registerNum")));
        result.put("enabledUserNum", number(total.get("enabledUserNum")));
        result.put("promoterNum", number(total.get("promoterNum")));
        result.put("userBalance", decimal(total.get("userBalance")));
        result.put("brokerageBalance", decimal(total.get("brokerageBalance")));
        result.put("integralBalance", number(total.get("integralBalance")));
        return result;
    }

    public List<Map<String, Object>> userChannelData() {
        return dashboardMapper.selectUserChannelData().stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    String channel = String.valueOf(row.getOrDefault("channel", ""));
                    item.put("channel", StringUtils.hasText(channel) ? channel : "未知");
                    item.put("num", number(row.get("num")));
                    return item;
                })
                .toList();
    }

    public List<Map<String, Object>> userSexData() {
        return normalizeNameNum(dashboardMapper.selectUserSexData());
    }

    public List<Map<String, Object>> userAreaData() {
        return normalizeNameNum(dashboardMapper.selectUserAreaData());
    }

    public Map<String, Object> userOverview(String dateLimit) {
        DateRange range = dateRange(dateLimit);
        DateRange previous = previousRange(range);
        Map<String, Object> current = userOverviewRange(range);
        Map<String, Object> before = userOverviewRange(previous);
        current.put("registerNumRatio", ratio(number(current.get("registerNum")), number(before.get("registerNum"))));
        current.put("activeUserNumRatio", ratio(number(current.get("activeUserNum")), number(before.get("activeUserNum"))));
        current.put("rechargeUserNumRatio", ratio(number(current.get("rechargeUserNum")), number(before.get("rechargeUserNum"))));
        return current;
    }

    public List<Map<String, Object>> userOverviewList(String dateLimit) {
        DateRange range = dateRange(dateLimit);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate day = range.start(); day.isBefore(range.end()); day = day.plusDays(1)) {
            DateRange itemRange = new DateRange(day, day.plusDays(1));
            Map<String, Object> row = userOverviewRange(itemRange);
            row.put("date", day.format(DAY_FORMATTER));
            rows.add(row);
        }
        return rows;
    }

    public Map<String, Object> tradeData() {
        Map<String, Object> total = dashboardMapper.selectTradeOverview(
                "1970-01-01 00:00:00",
                "2999-12-31 00:00:00");
        return tradeOverviewResult(total);
    }

    public Map<String, Object> tradeOverview(String dateLimit) {
        DateRange range = dateRange(dateLimit);
        return tradeOverviewResult(dashboardMapper.selectTradeOverview(formatDateTime(range.start()), formatDateTime(range.end())));
    }

    public Map<String, Object> tradeTrend(String dateLimit) {
        DateRange range = dateRange(dateLimit);
        List<DashboardChartPoint> points = dashboardMapper.selectPaidTradeTrend(formatDateTime(range.start()), formatDateTime(range.end()));
        Map<String, BigDecimal> amountMap = priceMap(points);
        Map<String, Integer> countMap = countMap(points);
        Map<Object, Object> payOrderAmount = new LinkedHashMap<>();
        Map<Object, Object> payOrderNum = new LinkedHashMap<>();
        for (String day : dayLabels(range)) {
            String label = day.substring(5);
            payOrderAmount.put(label, amountMap.getOrDefault(day, BigDecimal.ZERO));
            payOrderNum.put(label, countMap.getOrDefault(day, 0));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("payOrderAmount", payOrderAmount);
        result.put("payOrderNum", payOrderNum);
        return result;
    }

    private Map<String, Object> rangeOrderChart(
            LocalDate start,
            LocalDate end,
            LocalDate preStart,
            LocalDate preEnd,
            String[] labels) {
        List<DashboardChartPoint> points = dashboardMapper.selectOrderTrendByDay(startTime(start), endTime(end));
        List<DashboardChartPoint> prePoints = dashboardMapper.selectOrderTrendByDay(startTime(preStart), endTime(preEnd));
        return rangeOrderChart(start, preStart, labels, priceMap(points), countMap(points), priceMap(prePoints), countMap(prePoints));
    }

    private Map<String, Object> rangeOrderChart(
            LocalDate start,
            LocalDate preStart,
            String[] labels,
            Map<String, BigDecimal> priceData,
            Map<String, Integer> countData,
            Map<String, BigDecimal> prePriceData,
            Map<String, Integer> preCountData) {
        Map<Object, Object> price = new LinkedHashMap<>();
        Map<Object, Object> quality = new LinkedHashMap<>();
        Map<Object, Object> prePrice = new LinkedHashMap<>();
        Map<Object, Object> preQuality = new LinkedHashMap<>();
        for (int index = 0; index < labels.length; index++) {
            LocalDate day = start.plusDays(index);
            LocalDate preDay = preStart.plusDays(index);
            String label = labels[index];
            price.put(label, priceData.getOrDefault(day.format(DAY_FORMATTER), BigDecimal.ZERO));
            quality.put(label, countData.getOrDefault(day.format(DAY_FORMATTER), 0));
            prePrice.put(label, prePriceData.getOrDefault(preDay.format(DAY_FORMATTER), BigDecimal.ZERO));
            preQuality.put(label, preCountData.getOrDefault(preDay.format(DAY_FORMATTER), 0));
        }
        Map<String, Object> result = orderChart(price, quality);
        result.put("prePrice", prePrice);
        result.put("preQuality", preQuality);
        return result;
    }

    private Map<String, Object> yearOrderChart(int year, int preYear, List<DashboardChartPoint> points, List<DashboardChartPoint> prePoints) {
        Map<String, BigDecimal> priceData = priceMap(points);
        Map<String, Integer> countData = countMap(points);
        Map<String, BigDecimal> prePriceData = priceMap(prePoints);
        Map<String, Integer> preCountData = countMap(prePoints);
        Map<Object, Object> price = new LinkedHashMap<>();
        Map<Object, Object> quality = new LinkedHashMap<>();
        Map<Object, Object> prePrice = new LinkedHashMap<>();
        Map<Object, Object> preQuality = new LinkedHashMap<>();
        for (int index = 0; index < MONTH_LABELS.length; index++) {
            String month = String.format("%02d", index + 1);
            String label = MONTH_LABELS[index];
            price.put(label, priceData.getOrDefault(year + "-" + month, BigDecimal.ZERO));
            quality.put(label, countData.getOrDefault(year + "-" + month, 0));
            prePrice.put(label, prePriceData.getOrDefault(preYear + "-" + month, BigDecimal.ZERO));
            preQuality.put(label, preCountData.getOrDefault(preYear + "-" + month, 0));
        }
        Map<String, Object> result = orderChart(price, quality);
        result.put("prePrice", prePrice);
        result.put("preQuality", preQuality);
        return result;
    }

    private Map<String, Object> orderChart(Map<Object, Object> price, Map<Object, Object> quality) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("price", price);
        result.put("quality", quality);
        return result;
    }

    private int stockWarningValue() {
        String value = dashboardMapper.selectConfigValue("store_stock");
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return Math.max(Integer.parseInt(value.trim()), 0);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private Integer nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Map<String, BigDecimal> priceMap(List<DashboardChartPoint> points) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (DashboardChartPoint point : points) {
            result.put(point.getLabel(), nvl(point.getPrice()));
        }
        return result;
    }

    private Map<String, Integer> countMap(List<DashboardChartPoint> points) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (DashboardChartPoint point : points) {
            result.put(point.getLabel(), nvl(point.getCountValue()));
        }
        return result;
    }

    private String startTime(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MIN).format(DATE_TIME_FORMATTER);
    }

    private String endTime(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MAX).format(DATE_TIME_FORMATTER);
    }

    private Map<String, Object> userOverviewRange(DateRange range) {
        Map<String, Object> raw = dashboardMapper.selectUserOverviewRange(
                range.start().format(DAY_FORMATTER),
                range.end().format(DAY_FORMATTER),
                formatDateTime(range.start()),
                formatDateTime(range.end()));
        BigDecimal payOrderAmount = decimal(raw.get("payOrderAmount"));
        Integer orderPayUserNum = number(raw.get("orderPayUserNum"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("registerNum", number(raw.get("registerNum")));
        result.put("activeUserNum", number(raw.get("activeUserNum")));
        result.put("rechargeUserNum", number(raw.get("rechargeUserNum")));
        result.put("pageviews", number(raw.get("pageviews")));
        result.put("orderUserNum", number(raw.get("orderUserNum")));
        result.put("orderPayUserNum", orderPayUserNum);
        result.put("payOrderAmount", payOrderAmount);
        result.put("customerPrice", orderPayUserNum == 0
                ? BigDecimal.ZERO.setScale(2)
                : payOrderAmount.divide(BigDecimal.valueOf(orderPayUserNum), 2, RoundingMode.HALF_UP));
        return result;
    }

    private Map<String, Object> tradeOverviewResult(Map<String, Object> raw) {
        BigDecimal payOrderAmount = decimal(raw.get("payOrderAmount"));
        Integer payUserNum = number(raw.get("payUserNum"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNum", number(raw.get("orderNum")));
        result.put("payOrderNum", number(raw.get("payOrderNum")));
        result.put("waitPayOrderNum", number(raw.get("waitPayOrderNum")));
        result.put("refundingOrderNum", number(raw.get("refundingOrderNum")));
        result.put("refundedOrderNum", number(raw.get("refundedOrderNum")));
        result.put("payOrderAmount", payOrderAmount);
        result.put("refundAmount", decimal(raw.get("refundAmount")));
        result.put("orderUserNum", number(raw.get("orderUserNum")));
        result.put("payUserNum", payUserNum);
        result.put("customerPrice", payUserNum == 0
                ? BigDecimal.ZERO.setScale(2)
                : payOrderAmount.divide(BigDecimal.valueOf(payUserNum), 2, RoundingMode.HALF_UP));
        return result;
    }

    private List<Map<String, Object>> normalizeNameNum(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", String.valueOf(row.getOrDefault("name", "未知")));
                    item.put("num", number(row.get("num")));
                    return item;
                })
                .toList();
    }

    private DateRange dateRange(String dateLimit) {
        LocalDate today = LocalDate.now();
        if (!StringUtils.hasText(dateLimit)) {
            dateLimit = "lately30";
        }
        String value = dateLimit.trim();
        if (value.contains(",")) {
            String[] parts = value.split(",");
            if (parts.length >= 2) {
                try {
                    LocalDate start = LocalDate.parse(parts[0].trim(), DAY_FORMATTER);
                    LocalDate end = LocalDate.parse(parts[1].trim(), DAY_FORMATTER).plusDays(1);
                    if (start.isBefore(end)) return new DateRange(start, end);
                } catch (RuntimeException ignored) {
                    // Fall through to the default range.
                }
            }
        }
        return switch (value) {
            case "today" -> new DateRange(today, today.plusDays(1));
            case "yesterday" -> new DateRange(today.minusDays(1), today);
            case "lately7" -> new DateRange(today.minusDays(6), today.plusDays(1));
            case "week" -> new DateRange(today.with(DayOfWeek.MONDAY), today.plusDays(1));
            case "month" -> new DateRange(today.withDayOfMonth(1), today.withDayOfMonth(1).plusMonths(1));
            case "year" -> new DateRange(today.withDayOfYear(1), today.withDayOfYear(1).plusYears(1));
            default -> new DateRange(today.minusDays(29), today.plusDays(1));
        };
    }

    private DateRange previousRange(DateRange range) {
        long days = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(range.start(), range.end()));
        return new DateRange(range.start().minusDays(days), range.start());
    }

    private String ratio(Integer current, Integer before) {
        int currentValue = current == null ? 0 : current;
        int beforeValue = before == null ? 0 : before;
        if (currentValue - beforeValue == 0) return "0%";
        if (beforeValue == 0) return "100%";
        BigDecimal value = BigDecimal.valueOf(currentValue)
                .subtract(BigDecimal.valueOf(beforeValue))
                .divide(BigDecimal.valueOf(beforeValue), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return value.stripTrailingZeros().toPlainString() + "%";
    }

    private List<String> dayLabels(DateRange range) {
        List<String> labels = new ArrayList<>();
        for (LocalDate day = range.start(); day.isBefore(range.end()); day = day.plusDays(1)) {
            labels.add(day.format(DAY_FORMATTER));
        }
        return labels;
    }

    private long toUnix(LocalDate date) {
        return Instant.from(date.atStartOfDay(ZoneId.systemDefault())).getEpochSecond();
    }

    private Integer number(Object value) {
        if (value == null) return 0;
        if (value instanceof Number number) return number.intValue();
        try {
            return new BigDecimal(String.valueOf(value)).intValue();
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO.setScale(2);
        try {
            return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO.setScale(2);
        }
    }

    private String formatDateTime(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MIN).format(DATE_TIME_FORMATTER);
    }

    private String assetPath(String value) {
        if (!StringUtils.hasText(value)) return "";
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/")) return value;
        return "/" + value;
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
