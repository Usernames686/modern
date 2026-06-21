package com.jsy.crmeb.modern.service.front;

import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.front.mapper.FrontBargainMapper;
import com.jsy.crmeb.modern.service.product.dto.ProductAttrValueResponse;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductAttrValueMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductDescriptionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FrontBargainService {
    private static final int PRODUCT_TYPE_BARGAIN = 2;
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FrontBargainMapper bargainMapper;
    private final StoreProductAttrValueMapper attrValueMapper;
    private final StoreProductDescriptionMapper descriptionMapper;
    private final FrontUserCenterService userCenterService;

    public FrontBargainService(
            FrontBargainMapper bargainMapper,
            StoreProductAttrValueMapper attrValueMapper,
            StoreProductDescriptionMapper descriptionMapper,
            FrontUserCenterService userCenterService) {
        this.bargainMapper = bargainMapper;
        this.attrValueMapper = attrValueMapper;
        this.descriptionMapper = descriptionMapper;
        this.userCenterService = userCenterService;
    }

    public Map<String, Object> index() {
        List<Map<String, Object>> products = bargainMapper.selectVisible(System.currentTimeMillis(), 6, 0).stream()
                .map(this::toListItem)
                .toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("productList", products);
        return response;
    }

    public Map<String, Object> header() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bargainTotal", bargainMapper.countHelps());
        response.put("bargainSuccessList", bargainMapper.selectSuccessList().stream()
                .map(this::successRow)
                .toList());
        return response;
    }

    public PageResponse<Map<String, Object>> list(int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        long now = System.currentTimeMillis();
        long total = bargainMapper.countVisible(now);
        List<Map<String, Object>> rows = bargainMapper.selectVisible(now, safeLimit, (safePage - 1) * safeLimit).stream()
                .map(this::toListItem)
                .toList();
        return new PageResponse<>(safePage, safeLimit, total, rows);
    }

    public Map<String, Object> detail(Integer id) {
        return detail(id, null);
    }

    public Map<String, Object> detail(Integer id, Integer uid) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("砍价商品id不能为空");
        }
        Map<String, Object> row = validBargain(id);
        bargainMapper.increaseLook(id);
        if (uid != null) {
            userCenterService.recordVisit(uid, 3);
        }
        Map<String, Object> detail = toListItem(row);
        Integer productId = intValue(row.get("productId"));
        detail.put("masterStatus", masterStatus(productId));
        List<ProductAttrValueResponse> attrValues = attrValueMapper.selectByProductIdAndType(id, PRODUCT_TYPE_BARGAIN);
        if (!attrValues.isEmpty()) {
            ProductAttrValueResponse attrValue = attrValues.get(0);
            detail.put("attrValueId", attrValue.getId());
            detail.put("sku", attrValue.getSuk());
        } else {
            detail.put("attrValueId", null);
            detail.put("sku", "");
        }
        String content = descriptionMapper.selectDescription(id, PRODUCT_TYPE_BARGAIN);
        detail.put("content", content == null ? "" : content);
        return detail;
    }

    public Map<String, Object> user(Integer uid, Integer bargainId, Integer bargainUserId) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        Map<String, Object> bargain = validBargain(bargainId);
        if (intValue(bargain.get("stock")) <= 0 || intValue(bargain.get("quota")) <= 0) {
            throw new IllegalArgumentException("砍价商品已售罄");
        }
        long now = System.currentTimeMillis();
        if (longValue(bargain.get("stopTime")) < now) {
            throw new IllegalArgumentException("活动已结束");
        }
        if (bargainUserId != null && bargainUserId > 0) {
            return otherBargainActivity(uid, bargain, bargainUserId);
        }
        return ownBargainActivity(uid, bargain);
    }

    public Map<String, Object> start(Integer uid, Integer bargainId) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        Map<String, Object> bargain = validBargain(bargainId);
        if (intValue(bargain.get("quota")) <= 0 || intValue(bargain.get("stock")) <= 0) {
            throw new IllegalArgumentException("砍价商品已售罄");
        }
        if (System.currentTimeMillis() > longValue(bargain.get("stopTime"))) {
            throw new IllegalArgumentException("砍价活动已结束");
        }
        Map<String, Object> current = bargainMapper.selectLastUserBargain(bargainId, uid);
        if (current != null && !truthy(current.get("isDel")) && intValue(current.get("status")) == 1) {
            throw new IllegalArgumentException("请先完成当前砍价活动");
        }
        if (bargainMapper.countActiveBargainUserTimes(bargainId, uid) >= intValue(bargain.get("num"))) {
            throw new IllegalArgumentException("您已达到当前砍价活动上限");
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("uid", uid);
        row.put("bargainId", bargainId);
        row.put("bargainPriceMin", decimal(bargain.get("minPrice")));
        row.put("bargainPrice", decimal(bargain.get("price")));
        row.put("price", BigDecimal.ZERO);
        row.put("status", 1);
        row.put("addTime", System.currentTimeMillis());
        row.put("isDel", 0);
        if (bargainMapper.insertBargainUser(row) <= 0) {
            throw new IllegalArgumentException("参与砍价失败");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("storeBargainUserId", intValue(row.get("id")));
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> help(Integer uid, Integer bargainId, Integer bargainUserId, Integer bargainUserUid) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        if (bargainUserId == null || bargainUserId <= 0) {
            throw new IllegalArgumentException("砍价活动id不能为空");
        }
        Map<String, Object> bargain = validBargain(bargainId);
        if (intValue(bargain.get("quota")) <= 0 || intValue(bargain.get("stock")) <= 0) {
            throw new IllegalArgumentException("砍价商品已售罄");
        }
        long now = System.currentTimeMillis();
        if (now < longValue(bargain.get("startTime"))) {
            throw new IllegalArgumentException("砍价活动未开始");
        }
        if (now > longValue(bargain.get("stopTime"))) {
            throw new IllegalArgumentException("砍价活动已结束");
        }
        Map<String, Object> bargainUser = bargainMapper.selectUserBargainById(bargainUserId);
        if (bargainUser == null || intValue(bargainUser.get("bargainId")) != bargainId) {
            throw new IllegalArgumentException("砍价商品用户信息不存在");
        }
        BigDecimal minPrice = decimal(bargainUser.get("bargainPriceMin"));
        BigDecimal bargainPrice = decimal(bargainUser.get("bargainPrice"));
        BigDecimal oldCutPrice = decimal(bargainUser.get("price"));
        BigDecimal remainingTarget = bargainPrice.subtract(oldCutPrice);
        if (intValue(bargainUser.get("status")) == 3 || minPrice.compareTo(remainingTarget) >= 0) {
            throw new IllegalArgumentException("商品已完成砍价");
        }

        if (intValue(bargainUser.get("uid")) == uid) {
            if (bargainMapper.countUserHelp(bargainUserId, uid) > 0) {
                throw new IllegalArgumentException("您已经砍过了");
            }
        } else if (bargainMapper.countOtherHelpByUser(bargainId, uid) >= intValue(bargain.get("bargainNum"))) {
            throw new IllegalArgumentException("您的帮砍次数已达上限");
        }

        long helpCount = bargainMapper.countHelpByBargainUser(bargainId, bargainUserId);
        BigDecimal cutPrice = helpBargain(bargain, bargainUser, helpCount);
        boolean success = intValue(bargain.get("peopleNum")) == helpCount + 1;
        BigDecimal newCutPrice = oldCutPrice.add(cutPrice);
        int newStatus = success ? 3 : intValue(bargainUser.get("status"));
        if (bargainMapper.updateBargainUserPrice(bargainUserId, oldCutPrice, newCutPrice, newStatus) <= 0) {
            throw new IllegalArgumentException("砍价失败");
        }
        bargainMapper.insertBargainUserHelp(uid, bargainId, bargainUserId, cutPrice, System.currentTimeMillis());
        if (bargainUserUid == null || !uid.equals(bargainUserUid)) {
            bargainMapper.increaseShare(bargainId);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bargainPrice", money(cutPrice));
        response.put("storeBargainUserId", bargainUserId);
        response.put("success", success);
        return response;
    }

    public PageResponse<Map<String, Object>> record(Integer uid, int page, int limit) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        long total = bargainMapper.countUserRecords(uid);
        List<Map<String, Object>> rows = bargainMapper.selectUserRecords(uid, safeLimit, (safePage - 1) * safeLimit).stream()
                .map(this::recordRow)
                .toList();
        return new PageResponse<>(safePage, safeLimit, total, rows);
    }

    private BigDecimal helpBargain(Map<String, Object> bargain, Map<String, Object> bargainUser, long helpCount) {
        BigDecimal minPrice = decimal(bargainUser.get("bargainPriceMin"));
        BigDecimal bargainPrice = decimal(bargainUser.get("bargainPrice"));
        BigDecimal userPrice = decimal(bargainUser.get("price"));
        int peopleNum = intValue(bargain.get("peopleNum"));
        BigDecimal totalCuttable = bargainPrice.subtract(minPrice);
        if (totalCuttable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal cutPrice;
        if (helpCount == 0) {
            BigDecimal retainPrice = BigDecimal.valueOf(Math.max(0, peopleNum - 1)).multiply(new BigDecimal("0.01"));
            BigDecimal canCut = totalCuttable.subtract(retainPrice);
            if (canCut.compareTo(new BigDecimal("0.01")) > 0) {
                cutPrice = randomMoney(totalCuttable.multiply(new BigDecimal("0.2")), totalCuttable.multiply(new BigDecimal("0.8")));
                return minOneCent(cutPrice);
            }
            return new BigDecimal("0.01");
        }
        if (peopleNum - (int) helpCount == 1) {
            return totalCuttable.subtract(userPrice).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal retainPrice = BigDecimal.valueOf(Math.max(0, peopleNum - (int) helpCount)).multiply(new BigDecimal("0.01"));
        BigDecimal remaining = totalCuttable.subtract(userPrice).subtract(retainPrice);
        if (remaining.compareTo(new BigDecimal("0.01")) > 0) {
            cutPrice = randomMoney(remaining.multiply(new BigDecimal("0.2")), remaining.multiply(new BigDecimal("0.8")));
            return minOneCent(cutPrice);
        }
        return new BigDecimal("0.01");
    }

    private BigDecimal randomMoney(BigDecimal min, BigDecimal max) {
        if (max.compareTo(min) <= 0) {
            return min.setScale(2, RoundingMode.HALF_UP);
        }
        double value = ThreadLocalRandom.current().nextDouble(min.doubleValue(), max.doubleValue());
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal minOneCent(BigDecimal value) {
        BigDecimal result = value.compareTo(new BigDecimal("0.01")) < 0 ? new BigDecimal("0.01") : value;
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> validBargain(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("砍价商品id不能为空");
        }
        Map<String, Object> row = bargainMapper.selectById(id);
        if (row == null || truthy(row.get("isDel"))) {
            throw new IllegalArgumentException("未找到对应砍价商品信息");
        }
        if (!truthy(row.get("status"))) {
            throw new IllegalArgumentException("砍价商品已下架");
        }
        return row;
    }

    private Map<String, Object> ownBargainActivity(Integer uid, Map<String, Object> bargain) {
        Integer bargainId = intValue(bargain.get("id"));
        Map<String, Object> bargainUser = bargainMapper.selectLastUserBargain(bargainId, uid);
        if (bargainUser == null) {
            return userInfo(1, BigDecimal.ZERO, decimal(bargain.get("price")).subtract(decimal(bargain.get("minPrice"))), 0, null, null);
        }
        if (intValue(bargainUser.get("status")) == 2) {
            throw new IllegalArgumentException("未在活动期内完成砍价");
        }
        if (truthy(bargainUser.get("isDel"))) {
            int bargainCount = bargainMapper.countBargainUserTimes(bargainId, uid);
            int status = intValue(bargain.get("num")) >= bargainCount ? 2 : 1;
            return userInfo(status, BigDecimal.ZERO, decimal(bargain.get("price")).subtract(decimal(bargain.get("minPrice"))), 0, null, null);
        }
        if (intValue(bargainUser.get("status")) == 3) {
            return completedUserInfo(bargainUser, true, null);
        }
        return activeUserInfo(bargain, bargainUser, 3, null);
    }

    private Map<String, Object> otherBargainActivity(Integer uid, Map<String, Object> bargain, Integer bargainUserId) {
        Map<String, Object> bargainUser = bargainMapper.selectUserBargainById(bargainUserId);
        if (bargainUser == null) {
            throw new IllegalArgumentException("用户砍价活动未找到");
        }
        if (truthy(bargainUser.get("isDel"))) {
            throw new IllegalArgumentException("用户砍价活动已取消");
        }
        if (intValue(bargainUser.get("status")) == 2) {
            throw new IllegalArgumentException("砍价活动已过期");
        }
        Map<String, Object> owner = ownerInfo(bargainUser);
        if (intValue(bargainUser.get("uid")) == uid) {
            if (intValue(bargainUser.get("status")) == 3) {
                return completedUserInfo(bargainUser, true, null);
            }
            return activeUserInfo(bargain, bargainUser, 3, null);
        }
        if (intValue(bargainUser.get("status")) == 3) {
            return completedUserInfo(bargainUser, false, owner);
        }
        if (bargainMapper.countUserHelp(bargainUserId, uid) > 0) {
            return activeUserInfo(bargain, bargainUser, 6, owner);
        }
        if (bargainMapper.countOtherHelpByUser(intValue(bargain.get("id")), uid) >= intValue(bargain.get("bargainNum"))) {
            return activeUserInfo(bargain, bargainUser, 7, owner);
        }
        return activeUserInfo(bargain, bargainUser, 5, owner);
    }

    private Map<String, Object> completedUserInfo(Map<String, Object> bargainUser, boolean own, Map<String, Object> owner) {
        int status = own ? 4 : 4;
        Map<String, Object> order = bargainMapper.selectBargainOrder(intValue(bargainUser.get("bargainId")), intValue(bargainUser.get("id")));
        if (own && order != null) {
            status = truthy(order.get("paid")) ? 9 : 8;
            if (!truthy(order.get("paid")) && (truthy(order.get("isDel")) || truthy(order.get("isSystemDel")))) {
                status = 10;
            }
        }
        return userInfo(status, decimal(bargainUser.get("price")), BigDecimal.ZERO, 100, bargainUser, owner);
    }

    private Map<String, Object> activeUserInfo(Map<String, Object> bargain, Map<String, Object> bargainUser, int status, Map<String, Object> owner) {
        BigDecimal already = decimal(bargainUser.get("price"));
        BigDecimal surplus = decimal(bargainUser.get("bargainPrice")).subtract(decimal(bargain.get("minPrice"))).subtract(already);
        if (surplus.compareTo(BigDecimal.ZERO) < 0) {
            surplus = BigDecimal.ZERO;
        }
        int percent = percent(already, already.add(surplus));
        return userInfo(status, already, surplus, percent, bargainUser, owner);
    }

    private Map<String, Object> ownerInfo(Map<String, Object> bargainUser) {
        Map<String, Object> user = bargainMapper.selectUserBrief(intValue(bargainUser.get("uid")));
        Map<String, Object> owner = new LinkedHashMap<>();
        owner.put("storeBargainUserName", user == null ? "" : user.getOrDefault("nickname", ""));
        owner.put("storeBargainUserAvatar", user == null ? "" : user.getOrDefault("avatar", ""));
        return owner;
    }

    private Map<String, Object> userInfo(
            int bargainStatus,
            BigDecimal alreadyPrice,
            BigDecimal surplusPrice,
            int bargainPercent,
            Map<String, Object> bargainUser,
            Map<String, Object> owner) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bargainStatus", bargainStatus);
        response.put("alreadyPrice", money(alreadyPrice));
        response.put("surplusPrice", money(surplusPrice));
        response.put("bargainPercent", bargainPercent);
        Integer bargainUserId = bargainUser == null ? null : intValue(bargainUser.get("id"));
        response.put("storeBargainUserId", bargainUserId);
        response.put("storeBargainUserUid", bargainUser == null ? null : intValue(bargainUser.get("uid")));
        response.put("userHelpList", bargainUserId == null ? List.of() : bargainMapper.selectHelpList(bargainUserId).stream()
                .map(this::helpRow)
                .toList());
        if (owner != null) {
            response.putAll(owner);
        } else {
            response.put("storeBargainUserName", "");
            response.put("storeBargainUserAvatar", "");
        }
        return response;
    }

    private Map<String, Object> helpRow(Map<String, Object> source) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", intValue(source.get("id")));
        row.put("uid", intValue(source.get("uid")));
        row.put("bargainId", intValue(source.get("bargainId")));
        row.put("bargainUserId", intValue(source.get("bargainUserId")));
        row.put("price", money(source.get("price")));
        row.put("addTime", dateTimeText(source.get("addTime")));
        row.put("addTimeStr", dateTimeText(source.get("addTime")));
        row.put("avatar", source.getOrDefault("avatar", ""));
        row.put("nickname", StringUtils.hasText(text(source.get("nickname"))) ? source.get("nickname") : "用户");
        return row;
    }

    private Map<String, Object> toListItem(Map<String, Object> source) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", intValue(source.get("id")));
        row.put("productId", intValue(source.get("productId")));
        row.put("title", source.getOrDefault("title", ""));
        row.put("image", source.getOrDefault("image", ""));
        row.put("unitName", StringUtils.hasText(text(source.get("unitName"))) ? source.get("unitName") : "件");
        row.put("startTime", longValue(source.get("startTime")));
        row.put("stopTime", longValue(source.get("stopTime")));
        row.put("startTimeText", dateTimeText(source.get("startTime")));
        row.put("stopTimeText", dateTimeText(source.get("stopTime")));
        row.put("price", money(source.get("price")));
        row.put("minPrice", money(source.get("minPrice")));
        row.put("stock", intValue(source.get("stock")));
        row.put("sales", intValue(source.get("sales")));
        row.put("quota", intValue(source.get("quota")));
        row.put("quotaShow", intValue(source.get("quotaShow")));
        row.put("num", intValue(source.get("num")));
        row.put("peopleNum", intValue(source.get("peopleNum")));
        row.put("isSoldOut", intValue(source.get("quota")) <= 0 || intValue(source.get("stock")) <= 0);
        row.put("percent", percent(source.get("quotaShow"), source.get("quota")));
        return row;
    }

    private Map<String, Object> successRow(Map<String, Object> source) {
        Map<String, Object> row = new LinkedHashMap<>(source == null ? Map.of() : source);
        row.putIfAbsent("nickName", "用户");
        row.putIfAbsent("avatar", "");
        row.put("price", money(row.get("price")));
        return row;
    }

    private Map<String, Object> recordRow(Map<String, Object> source) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", intValue(source.get("id")));
        row.put("productId", intValue(source.get("productId")));
        row.put("title", source.getOrDefault("title", ""));
        row.put("image", source.getOrDefault("image", ""));
        row.put("stopTime", longValue(source.get("stopTime")));
        row.put("stopTimeText", dateTimeText(source.get("stopTime")));
        row.put("addTime", dateTimeText(source.get("addTime")));
        row.put("bargainUserId", intValue(source.get("bargainUserId")));
        row.put("status", intValue(source.get("status")));
        row.put("isDel", truthy(source.get("isDel")) || truthy(source.get("orderIsDel")) || truthy(source.get("orderIsSystemDel")));
        row.put("isOrder", StringUtils.hasText(text(source.get("orderNo"))));
        row.put("isPay", truthy(source.get("paid")));
        row.put("orderNo", text(source.get("orderNo")));
        row.put("surplusPrice", surplusPrice(source));
        row.put("bargainPrice", money(source.get("bargainPrice")));
        row.put("cutPrice", money(source.get("cutPrice")));
        row.put("minPrice", money(source.get("minPrice")));
        row.put("price", money(source.get("price")));
        row.put("quota", intValue(source.get("quota")));
        row.put("unitName", StringUtils.hasText(text(source.get("unitName"))) ? source.get("unitName") : "件");
        row.put("statusText", recordStatusText(row));
        return row;
    }

    private String surplusPrice(Map<String, Object> source) {
        BigDecimal surplus;
        if (intValue(source.get("status")) == 3) {
            surplus = decimal(source.get("bargainPriceMin"));
        } else {
            surplus = decimal(source.get("bargainPrice")).subtract(decimal(source.get("cutPrice")));
        }
        if (surplus.compareTo(BigDecimal.ZERO) < 0) {
            surplus = BigDecimal.ZERO;
        }
        return money(surplus);
    }

    private String recordStatusText(Map<String, Object> row) {
        if (truthy(row.get("isDel"))) return "已取消";
        int status = intValue(row.get("status"));
        if (status == 1) return "砍价中";
        if (status == 2) return "砍价失败";
        if (status == 3 && truthy(row.get("isPay"))) return "已支付";
        if (status == 3 && truthy(row.get("isOrder"))) return "待支付";
        if (status == 3) return "砍价成功";
        return "未知";
    }

    private String masterStatus(Integer productId) {
        Map<String, Object> product = productId == null ? null : bargainMapper.selectMasterProduct(productId);
        if (product == null || truthy(product.get("isDel"))) {
            return "delete";
        }
        if (!truthy(product.get("isShow"))) {
            return "soldOut";
        }
        if (intValue(product.get("stock")) <= 0) {
            return "sellOut";
        }
        return "normal";
    }

    private int percent(Object quotaShow, Object quota) {
        int total = intValue(quotaShow);
        if (total <= 0) {
            return 0;
        }
        int left = intValue(quota);
        int sold = Math.max(0, total - left);
        return BigDecimal.valueOf(sold)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private String dateTimeText(Object value) {
        long millis = longValue(value);
        if (millis <= 0) {
            return "";
        }
        return Instant.ofEpochMilli(millis).atZone(ZONE).toLocalDateTime().format(DATE_TIME);
    }

    private String money(Object value) {
        BigDecimal decimal = decimal(value);
        return decimal.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        } else if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private boolean truthy(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.intValue() != 0;
        if (value == null) return false;
        String text = String.valueOf(value);
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "'1'".equals(text);
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return 0;
        return Integer.parseInt(String.valueOf(value));
    }

    private long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return 0L;
        return Long.parseLong(String.valueOf(value));
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
