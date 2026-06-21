package com.jsy.crmeb.modern.service.front;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.finance.entity.UserExtract;
import com.jsy.crmeb.modern.service.front.dto.FrontRechargeRequest;
import com.jsy.crmeb.modern.service.front.dto.FrontUserExtractRequest;
import com.jsy.crmeb.modern.service.front.mapper.FrontUserCenterMapper;
import com.jsy.crmeb.modern.service.user.entity.User;
import com.jsy.crmeb.modern.service.user.entity.UserBrokerageRecord;
import com.jsy.crmeb.modern.service.user.mapper.UserBrokerageRecordMapper;
import com.jsy.crmeb.modern.service.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrontUserCenterService {
    private static final int GROUP_DATA_ID_USER_CENTER_MENU = 54;
    private static final int GROUP_DATA_ID_USER_CENTER_BANNER = 65;
    private static final int GROUP_DATA_ID_SIGN = 55;
    private static final int GROUP_DATA_ID_SPREAD_BANNER = 60;
    private static final int GROUP_DATA_ID_RECHARGE_LIST = 62;

    private final FrontUserCenterMapper userCenterMapper;
    private final UserMapper userMapper;
    private final UserBrokerageRecordMapper userBrokerageRecordMapper;
    private final ObjectMapper objectMapper;

    public FrontUserCenterService(
            FrontUserCenterMapper userCenterMapper,
            UserMapper userMapper,
            UserBrokerageRecordMapper userBrokerageRecordMapper,
            ObjectMapper objectMapper) {
        this.userCenterMapper = userCenterMapper;
        this.userMapper = userMapper;
        this.userBrokerageRecordMapper = userBrokerageRecordMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> balance(Integer uid) {
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new IllegalArgumentException("用户数据异常");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("nowMoney", money(user.getNowMoney()));
        response.put("recharge", money(userCenterMapper.sumRecharge(uid)));
        response.put("orderStatusSum", money(userCenterMapper.sumPaidOrders(uid)));
        return response;
    }

    public PageResponse<Map<String, Object>> billRecord(Integer uid, String type, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 12 : Math.min(limit, 100);
        Integer pm = billPm(type);
        int offset = (safePage - 1) * safeLimit;
        List<Map<String, Object>> rows = userCenterMapper.selectBalanceBills(uid, pm, offset, safeLimit);
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String month = String.valueOf(row.getOrDefault("billMonth", ""));
            grouped.computeIfAbsent(month, key -> new ArrayList<>()).add(row);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("date", entry.getKey());
            group.put("list", entry.getValue());
            list.add(group);
        }
        return new PageResponse<>(safePage, safeLimit, userCenterMapper.countBalanceBills(uid, pm), list);
    }

    public Map<String, Object> rechargeConfig() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("rechargeQuota", groupDataItems(GROUP_DATA_ID_RECHARGE_LIST).stream()
                .map(this::normalizeRechargeItem)
                .filter(item -> money(item.get("price")).compareTo(BigDecimal.ZERO) > 0)
                .toList());
        String attention = config("recharge_attention", "");
        response.put("rechargeAttention", attention.replace("\r\n", "\n").replace("\\n", "\n").lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList());
        response.put("rechargeSwitch", Boolean.parseBoolean(config("recharge_switch", "true")));
        response.put("minRecharge", decimalConfig("store_user_min_recharge", BigDecimal.ONE));
        response.put("localMode", true);
        response.put("message", "当前未配置微信支付，提交充值时只生成待支付充值单。");
        return response;
    }

    @Transactional
    public Map<String, Object> rechargeDryRun(Integer uid, FrontRechargeRequest request, String defaultFromType) {
        activeUser(uid);
        if (!Boolean.parseBoolean(config("recharge_switch", "true"))) {
            throw new IllegalArgumentException("充值功能已关闭");
        }
        if (request == null) {
            throw new IllegalArgumentException("充值参数不能为空");
        }

        BigDecimal price = money(request.getPrice()).setScale(2, RoundingMode.DOWN);
        BigDecimal givePrice = BigDecimal.ZERO;
        if (request.getGroupDataId() != null && request.getGroupDataId() > 0) {
            Map<String, Object> row = userCenterMapper.selectGroupDataById(GROUP_DATA_ID_RECHARGE_LIST, request.getGroupDataId());
            if (row == null || row.isEmpty()) {
                throw new IllegalArgumentException("您选择的充值方式已下架");
            }
            Map<String, Object> item = normalizeRechargeItem(parseGroupDataValue(String.valueOf(row.getOrDefault("value", ""))));
            price = money(item.get("price")).setScale(2, RoundingMode.DOWN);
            givePrice = money(item.get("giveMoney")).setScale(2, RoundingMode.DOWN);
        }
        BigDecimal minRecharge = decimalConfig("store_user_min_recharge", BigDecimal.ONE).setScale(2, RoundingMode.DOWN);
        if (price.compareTo(minRecharge) < 0) {
            throw new IllegalArgumentException("充值金额不能低于" + minRecharge.stripTrailingZeros().toPlainString());
        }
        String rechargeType = trim(request.getFromType());
        if (rechargeType.isBlank()) {
            rechargeType = defaultFromType;
        }
        String orderId = "recharge" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()) + uid;
        userCenterMapper.insertRechargeDryRun(uid, orderId, price, givePrice, rechargeType);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", false);
        result.put("localMode", true);
        result.put("orderId", orderId);
        result.put("payType", trim(request.getPayType()).isBlank() ? "weixin" : trim(request.getPayType()));
        result.put("rechargeType", rechargeType);
        result.put("price", price);
        result.put("givePrice", givePrice);
        result.put("message", "当前未配置微信支付，已生成待支付充值单，未增加用户余额。");
        result.put("jsConfig", Map.of());
        return result;
    }

    public Map<String, Object> integralUser(Integer uid) {
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new IllegalArgumentException("用户数据异常");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("integral", user.getIntegral() == null ? 0 : user.getIntegral());
        response.put("sumIntegral", nvl(userCenterMapper.sumIntegral(uid, 1, null)));
        response.put("deductionIntegral", nvl(userCenterMapper.sumIntegral(uid, 2, "order,system")));
        response.put("frozenIntegral", nvl(userCenterMapper.frozenIntegral(uid)));
        return response;
    }

    public PageResponse<Map<String, Object>> integralList(Integer uid, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        return new PageResponse<>(
                safePage,
                safeLimit,
                userCenterMapper.countIntegralRecords(uid),
                userCenterMapper.selectIntegralRecords(uid, offset, safeLimit));
    }

    public Map<String, Object> menuUser() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("routine_my_menus", groupDataItems(GROUP_DATA_ID_USER_CENTER_MENU));
        response.put("routine_my_banner", groupDataItems(GROUP_DATA_ID_USER_CENTER_BANNER));
        return response;
    }

    public List<Map<String, Object>> signConfig() {
        return groupDataItems(GROUP_DATA_ID_SIGN).stream()
                .map(this::normalizeSignConfig)
                .sorted(Comparator.comparingInt(item -> intValue(item.get("day"))))
                .toList();
    }

    public PageResponse<Map<String, Object>> signList(Integer uid, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        return new PageResponse<>(safePage, safeLimit, userCenterMapper.countSigns(uid), userCenterMapper.selectSignList(uid, offset, safeLimit));
    }

    public PageResponse<Map<String, Object>> signMonthList(Integer uid, int page, int limit) {
        PageResponse<Map<String, Object>> flatPage = signList(uid, page, limit);
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : flatPage.getList()) {
            String createDay = String.valueOf(row.getOrDefault("createDay", ""));
            String month = createDay.length() >= 7 ? createDay.substring(0, 7) : createDay;
            grouped.computeIfAbsent(month, key -> new ArrayList<>()).add(row);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", entry.getKey());
            item.put("list", entry.getValue());
            list.add(item);
        }
        return new PageResponse<>(flatPage.getPage(), flatPage.getLimit(), flatPage.getTotal(), list);
    }

    public Map<String, Object> signInfo(Integer uid) {
        User user = activeUser(uid);
        boolean todaySign = userCenterMapper.countSignByDay(uid, LocalDate.now()) > 0;
        boolean yesterdaySign = userCenterMapper.countSignByDay(uid, LocalDate.now().minusDays(1)) > 0;
        int signNum = user.getSignNum() == null ? 0 : user.getSignNum();
        if (!yesterdaySign && !todaySign) {
            signNum = 0;
        }
        int cycleDays = signConfig().size();
        if (cycleDays > 0 && signNum >= cycleDays) {
            signNum = 0;
            userCenterMapper.resetSignNum(uid);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("uid", user.getUid());
        response.put("nickname", user.getNickname());
        response.put("avatar", user.getAvatar());
        response.put("phone", user.getPhone());
        response.put("integral", user.getIntegral() == null ? 0 : user.getIntegral());
        response.put("experience", user.getExperience() == null ? 0 : user.getExperience());
        response.put("signNum", signNum);
        response.put("sumSignDay", userCenterMapper.countSigns(uid));
        response.put("isDaySign", todaySign);
        response.put("isYesterdaySign", yesterdaySign);
        return response;
    }

    public Map<String, Object> signToday(Integer uid) {
        Map<String, Object> response = new LinkedHashMap<>();
        User user = activeUser(uid);
        response.put("integral", user.getIntegral() == null ? 0 : user.getIntegral());
        response.put("count", userCenterMapper.countSigns(uid));
        response.put("today", userCenterMapper.countSignByDay(uid, LocalDate.now()) > 0);
        return response;
    }

    @Transactional
    public Map<String, Object> sign(Integer uid) {
        User user = activeUser(uid);
        LocalDate today = LocalDate.now();
        LocalDate lastDay = userCenterMapper.selectLastSignDay(uid);
        int signNum = user.getSignNum() == null ? 0 : user.getSignNum();
        if (lastDay == null) {
            signNum = 0;
        } else if (lastDay.equals(today)) {
            throw new IllegalArgumentException("今日已签到。不可重复签到");
        } else if (!lastDay.plusDays(1).equals(today)) {
            signNum = 0;
        }

        List<Map<String, Object>> configs = signConfig();
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("签到配置不存在，请在管理端配置签到数据");
        }
        if (signNum >= configs.size()) {
            signNum = 0;
        }
        int nextSignNum = signNum + 1;
        Map<String, Object> config = configs.stream()
                .filter(item -> intValue(item.get("day")) == nextSignNum)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("请先配置签到天数！"));

        int integral = intValue(config.get("integral"));
        int experience = intValue(config.get("experience"));
        int nextIntegral = (user.getIntegral() == null ? 0 : user.getIntegral()) + integral;
        int nextExperience = (user.getExperience() == null ? 0 : user.getExperience()) + experience;
        userCenterMapper.insertSign(uid, integral, nextIntegral);
        userCenterMapper.insertSignIntegralRecord(uid, integral, nextIntegral, "签到积分奖励增加了" + integral + "积分");
        userCenterMapper.insertSignExperienceRecord(uid, experience, nextExperience, "签到经验奖励增加了" + experience + "经验");
        userCenterMapper.updateSignReward(uid, nextSignNum, nextIntegral, nextExperience);

        Map<String, Object> response = new LinkedHashMap<>(config);
        response.put("signNum", nextSignNum);
        response.put("balance", nextIntegral);
        return response;
    }

    public List<Map<String, Object>> userLevelGrade() {
        return userCenterMapper.selectVisibleUserLevels().stream()
                .map(level -> {
                    Map<String, Object> item = new LinkedHashMap<>(level);
                    if (item.get("icon") != null) {
                        item.put("icon", normalizeAsset(String.valueOf(item.get("icon"))));
                    }
                    return item;
                })
                .toList();
    }

    public PageResponse<Map<String, Object>> experienceList(Integer uid, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        return new PageResponse<>(
                safePage,
                safeLimit,
                userCenterMapper.countExperienceRecords(uid),
                userCenterMapper.selectExperienceRecords(uid, offset, safeLimit));
    }

    public Map<String, Object> commission(Integer uid) {
        User user = activeUser(uid);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("lastDayCount", money(userCenterMapper.sumYesterdayBrokerage(uid)));
        response.put("extractCount", money(userCenterMapper.sumUserExtracted(uid)));
        response.put("commissionCount", money(user.getBrokeragePrice()));
        return response;
    }

    public boolean recordVisit(Integer uid, Integer visitType) {
        int safeType = visitType == null ? 0 : visitType;
        if (safeType < 1 || safeType > 4) {
            return false;
        }
        int safeUid = uid == null || uid < 0 ? 0 : uid;
        userCenterMapper.insertVisitRecord(LocalDate.now().toString(), safeUid, safeType);
        return true;
    }

    public Map<String, BigDecimal> spreadCount(Integer uid, int type) {
        User user = activeUser(uid);
        Map<String, BigDecimal> response = new LinkedHashMap<>();
        if (type == 3) {
            response.put("count", money(user.getBrokeragePrice()));
        } else if (type == 4) {
            response.put("count", money(userCenterMapper.sumUserExtracted(uid)));
        } else {
            response.put("count", BigDecimal.ZERO);
        }
        return response;
    }

    public PageResponse<Map<String, Object>> brokerageRecords(Integer uid, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map<String, Object> row : userCenterMapper.selectBrokerageRecordMonths(uid, offset, safeLimit)) {
            String month = String.valueOf(row.getOrDefault("date", ""));
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("date", month);
            group.put("list", userCenterMapper.selectBrokerageRecordsByMonth(uid, month));
            groups.add(group);
        }
        return new PageResponse<>(safePage, safeLimit, userCenterMapper.countBrokerageRecordMonths(uid), groups);
    }

    public PageResponse<Map<String, Object>> extractRecords(Integer uid, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map<String, Object> row : userCenterMapper.selectExtractRecordMonths(uid, offset, safeLimit)) {
            String month = String.valueOf(row.getOrDefault("date", ""));
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("date", month);
            group.put("list", userCenterMapper.selectExtractRecordsByMonth(uid, month));
            groups.add(group);
        }
        return new PageResponse<>(safePage, safeLimit, userCenterMapper.countExtractRecordMonths(uid), groups);
    }

    public Map<String, Object> extractUser(Integer uid) {
        User user = activeUser(uid);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("minPrice", config("user_extract_min_price", "0"));
        response.put("commissionCount", money(user.getBrokeragePrice()));
        response.put("brokenCommission", money(userCenterMapper.sumFrozenBrokerage(uid)));
        response.put("brokenDay", config("extract_time", "0"));
        return response;
    }

    public List<String> extractBank() {
        String value = config("user_extract_bank", "");
        String normalized = value.replace("\r\n", "\n").replace("\\n", "\n");
        return normalized.lines()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    @Transactional
    public boolean extractCash(Integer uid, FrontUserExtractRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("提现参数不能为空");
        }
        User user = activeUser(uid);
        String extractType = trim(request.getExtractType());
        BigDecimal extractPrice = money(request.getExtractPrice()).setScale(2, RoundingMode.DOWN);
        if (extractPrice.compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("请输入提现金额");
        }
        BigDecimal minPrice = decimalConfig("user_extract_min_price", BigDecimal.ZERO);
        if (extractPrice.compareTo(minPrice) < 0) {
            throw new IllegalArgumentException("最低提现金额" + minPrice.stripTrailingZeros().toPlainString() + "元");
        }
        BigDecimal currentBrokerage = money(userBrokerageRecordMapper.selectUserBrokeragePriceForUpdate(uid));
        if (currentBrokerage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("您当前没有金额可以提现");
        }
        if (currentBrokerage.compareTo(extractPrice) < 0) {
            throw new IllegalArgumentException("你当前最多可提现 " + currentBrokerage.stripTrailingZeros().toPlainString() + "元");
        }

        UserExtract extract = buildExtract(uid, request, extractType, extractPrice, currentBrokerage.subtract(extractPrice));
        userCenterMapper.insertUserExtract(extract);
        if (extract.getId() == null) {
            throw new IllegalStateException("提现申请保存失败");
        }
        BigDecimal balance = currentBrokerage.subtract(extractPrice);
        if (userBrokerageRecordMapper.updateUserBrokeragePrice(uid, balance) != 1) {
            throw new IllegalArgumentException("提现用户数据异常");
        }

        UserBrokerageRecord record = new UserBrokerageRecord();
        record.setUid(uid);
        record.setLinkId(String.valueOf(extract.getId()));
        record.setLinkType("withdraw");
        record.setType(2);
        record.setTitle("提现申请");
        record.setPrice(extractPrice);
        record.setBalance(balance);
        record.setMark("提现申请扣除佣金" + extractPrice.stripTrailingZeros().toPlainString());
        record.setStatus(5);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        userBrokerageRecordMapper.insert(record);
        return true;
    }

    public Map<String, Object> spreadOrders(Integer uid, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", userCenterMapper.countSpreadOrders(uid));
        List<Map<String, Object>> rows = userCenterMapper.selectSpreadOrders(uid, offset, safeLimit);
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String month = String.valueOf(row.getOrDefault("timeMonth", ""));
            Map<String, Object> group = grouped.computeIfAbsent(month, key -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("time", key);
                value.put("count", userCenterMapper.countSpreadOrdersByMonth(uid, key));
                value.put("child", new ArrayList<Map<String, Object>>());
                return value;
            });
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) group.get("child");
            Map<String, Object> child = new LinkedHashMap<>();
            child.put("orderId", row.get("orderId"));
            child.put("time", row.get("time"));
            child.put("number", money((BigDecimal) row.get("number")));
            child.put("avatar", normalizeAsset(String.valueOf(row.getOrDefault("avatar", ""))));
            child.put("nickname", row.get("nickname"));
            child.put("type", "返佣");
            children.add(child);
        }
        response.put("list", new ArrayList<>(grouped.values()));
        return response;
    }

    public List<Map<String, Object>> spreadRank(String type, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        DateRange range = dateRange(type);
        return userCenterMapper.selectSpreadRank(range.start(), range.end(), offset, safeLimit).stream()
                .map(item -> withNormalizedAvatar(item, "avatar"))
                .toList();
    }

    public List<Map<String, Object>> brokerageRank(String type, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        DateRange range = dateRange(type);
        return userCenterMapper.selectBrokerageRank(range.start(), range.end(), offset, safeLimit).stream()
                .map(item -> withNormalizedAvatar(item, "avatar"))
                .toList();
    }

    public Integer brokerageRankNumber(Integer uid, String type) {
        DateRange range = dateRange(type);
        Integer number = userCenterMapper.selectBrokerageRankNumber(uid, range.start(), range.end());
        return number == null ? 0 : number;
    }

    public List<Map<String, Object>> spreadBanner() {
        return groupDataItems(GROUP_DATA_ID_SPREAD_BANNER).stream()
                .map(item -> {
                    Map<String, Object> banner = new LinkedHashMap<>(item);
                    if (banner.get("pic") == null && banner.get("image") != null) {
                        banner.put("pic", normalizeAsset(String.valueOf(banner.get("image"))));
                    }
                    return banner;
                })
                .toList();
    }

    @Transactional
    public boolean bindSpread(Integer uid, Integer spreadPid) {
        if (spreadPid == null || spreadPid <= 0) {
            return false;
        }
        User user = activeUser(uid);
        if (uid.equals(spreadPid) || (user.getSpreadUid() != null && user.getSpreadUid() > 0)) {
            return false;
        }
        if (!"1".equals(config("brokerage_func_status", "0"))) {
            return false;
        }

        User spreadUser = userMapper.selectById(spreadPid);
        if (spreadUser == null
                || Integer.valueOf(1).equals(spreadUser.getIsLogoff())
                || Integer.valueOf(0).equals(spreadUser.getStatus())) {
            return false;
        }
        if (requiresPromoterForSpread() && !Integer.valueOf(1).equals(spreadUser.getIsPromoter())) {
            return false;
        }
        if (wouldCreateSpreadLoop(uid, spreadPid)) {
            return false;
        }

        int updated = userMapper.updateSpread(uid, spreadPid, LocalDateTime.now());
        if (updated == 1) {
            userMapper.updateSpreadCount(spreadPid, 1);
            return true;
        }
        return false;
    }

    public Map<String, Object> spreadPeopleCount(Integer uid) {
        long firstCount = userCenterMapper.countFirstSpreadPeople(uid);
        long secondCount = userCenterMapper.countSecondSpreadPeople(uid);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", firstCount);
        response.put("totalLevel", secondCount);
        response.put("count", firstCount + secondCount);
        response.put("spreadPeopleList", List.of());
        return response;
    }

    public PageResponse<Map<String, Object>> spreadPeople(
            Integer uid, int grade, String keyword, String sortKey, String isAsc, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        List<Integer> ids = userCenterMapper.selectSpreadPeopleIds(uid);
        if (grade == 1 && !ids.isEmpty()) {
            ids = userCenterMapper.selectSpreadPeopleIdsByParents(ids);
        }
        ids = ids.stream().distinct().toList();
        if (ids.isEmpty()) {
            return new PageResponse<>(safePage, safeLimit, 0, List.of());
        }
        String sortSql = spreadSortSql(sortKey, isAsc);
        String safeKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        long total = userCenterMapper.countSpreadPeopleList(ids, safeKeyword);
        List<Map<String, Object>> list = userCenterMapper.selectSpreadPeopleList(ids, safeKeyword, sortSql, offset, safeLimit);
        return new PageResponse<>(safePage, safeLimit, total, list);
    }

    private Integer billPm(String type) {
        if ("expenditure".equals(type)) {
            return 0;
        }
        if ("income".equals(type)) {
            return 1;
        }
        return null;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private Map<String, Object> withNormalizedAvatar(Map<String, Object> source, String key) {
        Map<String, Object> item = new LinkedHashMap<>(source);
        item.put(key, normalizeAsset(String.valueOf(item.getOrDefault(key, ""))));
        return item;
    }

    private DateRange dateRange(String type) {
        LocalDate today = LocalDate.now();
        LocalDate start = null;
        LocalDate end = today;
        if ("month".equalsIgnoreCase(type)) {
            start = today.withDayOfMonth(1);
        } else if ("week".equalsIgnoreCase(type)) {
            start = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        }
        if (start == null) {
            return new DateRange(null, null);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return new DateRange(
                LocalDateTime.of(start, LocalTime.MIN).format(formatter),
                LocalDateTime.of(end, LocalTime.MAX).format(formatter));
    }

    private String spreadSortSql(String sortKey, String isAsc) {
        String direction = "ASC".equalsIgnoreCase(isAsc) ? "asc" : "desc";
        String column = switch (sortKey == null ? "" : sortKey) {
            case "childCount" -> "childCount";
            case "numberCount" -> "numberCount";
            case "orderCount" -> "orderCount";
            default -> "u.create_time";
        };
        return column + " " + direction + ", u.uid desc";
    }

    private Integer nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private UserExtract buildExtract(
            Integer uid,
            FrontUserExtractRequest request,
            String extractType,
            BigDecimal extractPrice,
            BigDecimal balance) {
        String realName = trim(request.getRealName());
        if (realName.isBlank()) {
            throw new IllegalArgumentException("提现用户名称必须填写");
        }
        if (realName.length() > 64) {
            throw new IllegalArgumentException("提现用户名称不能超过64个字符");
        }
        UserExtract extract = new UserExtract();
        extract.setUid(uid);
        extract.setRealName(realName);
        extract.setExtractType(extractType);
        extract.setExtractPrice(extractPrice);
        extract.setBalance(balance);
        extract.setMark(trim(request.getMark()));
        extract.setStatus(0);
        extract.setCreateTime(LocalDateTime.now());
        extract.setUpdateTime(LocalDateTime.now());

        switch (extractType) {
            case "bank" -> {
                String bankName = trim(request.getBankName());
                String bankCode = trim(request.getBankCode());
                if (bankName.isBlank()) {
                    throw new IllegalArgumentException("请填写银行名称！");
                }
                if (bankCode.isBlank()) {
                    throw new IllegalArgumentException("请填写银行卡号！");
                }
                extract.setBankName(bankName);
                extract.setBankCode(bankCode);
            }
            case "weixin" -> {
                String wechat = trim(request.getWechat());
                if (wechat.isBlank()) {
                    throw new IllegalArgumentException("请填写微信号！");
                }
                extract.setWechat(wechat);
                extract.setQrcodeUrl(clearPrefix(request.getQrcodeUrl()));
            }
            case "alipay" -> {
                String alipayCode = trim(request.getAlipayCode());
                if (alipayCode.isBlank()) {
                    throw new IllegalArgumentException("请填写支付宝账号！");
                }
                extract.setAlipayCode(alipayCode);
            }
            default -> throw new IllegalArgumentException("请选择支付方式");
        }
        return extract;
    }

    private BigDecimal decimalConfig(String name, BigDecimal fallback) {
        try {
            String value = config(name, "");
            return value.isBlank() ? fallback : new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String config(String name, String fallback) {
        String value = userCenterMapper.configValue(name);
        return value == null ? fallback : value;
    }

    private boolean requiresPromoterForSpread() {
        return "1".equals(config("store_brokerage_status", "1"));
    }

    private boolean wouldCreateSpreadLoop(Integer uid, Integer spreadPid) {
        Integer cursor = spreadPid;
        for (int depth = 0; cursor != null && cursor > 0 && depth < 20; depth++) {
            if (uid.equals(cursor)) {
                return true;
            }
            cursor = userCenterMapper.selectUserSpreadUid(cursor);
        }
        return false;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private List<Map<String, Object>> groupDataItems(int gid) {
        List<Map<String, Object>> rows = userCenterMapper.selectGroupDataByGid(gid);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String value = String.valueOf(row.getOrDefault("value", ""));
            Map<String, Object> item = parseGroupDataValue(value);
            if (item.isEmpty()) {
                continue;
            }
            item.put("id", row.get("id"));
            item.put("sort", row.get("sort"));
            if (item.get("pic") != null) {
                item.put("pic", normalizeAsset(String.valueOf(item.get("pic"))));
            }
            items.add(item);
        }
        return items;
    }

    private Map<String, Object> parseGroupDataValue(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(value);
            JsonNode fields = root.get("fields");
            if (fields == null || !fields.isArray()) {
                return Map.of();
            }
            Map<String, Object> item = new LinkedHashMap<>();
            for (JsonNode field : fields) {
                String name = field.path("name").asText("");
                if (name.isBlank()) {
                    continue;
                }
                JsonNode fieldValue = field.get("value");
                Object convertedValue = fieldValue == null || fieldValue.isNull()
                        ? ""
                        : objectMapper.convertValue(fieldValue, new TypeReference<Object>() {});
                item.put(name, convertedValue);
            }
            return item;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private record DateRange(String start, String end) {
    }

    private String normalizeAsset(String value) {
        if (value == null || value.isBlank() || value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/")) {
            return value;
        }
        return "/" + value;
    }

    private String clearPrefix(String value) {
        String text = trim(value);
        if (text.isBlank()) {
            return "";
        }
        int index = text.indexOf("crmebimage/");
        if (index >= 0) {
            return text.substring(index);
        }
        if (text.startsWith("/")) {
            return text.substring(1);
        }
        return text;
    }

    private Map<String, Object> normalizeSignConfig(Map<String, Object> item) {
        Map<String, Object> normalized = new LinkedHashMap<>(item);
        normalized.put("day", intValue(item.get("day")));
        normalized.put("integral", intValue(item.get("integral")));
        normalized.put("experience", intValue(item.get("experience")));
        return normalized;
    }

    private Map<String, Object> normalizeRechargeItem(Map<String, Object> item) {
        Map<String, Object> normalized = new LinkedHashMap<>(item);
        Object giveMoney = normalized.containsKey("giveMoney") ? normalized.get("giveMoney") : normalized.get("give_money");
        normalized.put("price", money(normalized.get("price")).setScale(2, RoundingMode.DOWN));
        normalized.put("giveMoney", money(giveMoney).setScale(2, RoundingMode.DOWN));
        normalized.remove("give_money");
        return normalized;
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private User activeUser(Integer uid) {
        User user = userMapper.selectById(uid);
        if (user == null || Integer.valueOf(1).equals(user.getIsLogoff()) || Integer.valueOf(0).equals(user.getStatus())) {
            throw new IllegalArgumentException("用户数据异常");
        }
        return user;
    }
}
