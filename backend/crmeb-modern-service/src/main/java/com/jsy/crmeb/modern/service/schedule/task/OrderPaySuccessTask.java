package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.order.mapper.OrderPaySuccessTaskMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Component("OrderPaySuccessTask")
public class OrderPaySuccessTask {
    private static final Logger logger = LoggerFactory.getLogger(OrderPaySuccessTask.class);
    public static final String ORDER_TASK_PAY_SUCCESS_AFTER = "orderPaySuccessTask";

    private final StringRedisTemplate redisTemplate;
    private final OrderPaySuccessTaskMapper orderPaySuccessTaskMapper;
    private final TransactionTemplate transactionTemplate;

    public OrderPaySuccessTask(
            StringRedisTemplate redisTemplate,
            OrderPaySuccessTaskMapper orderPaySuccessTaskMapper,
            TransactionTemplate transactionTemplate) {
        this.redisTemplate = redisTemplate;
        this.orderPaySuccessTaskMapper = orderPaySuccessTaskMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public void orderPayAfter() {
        Long size = redisTemplate.opsForList().size(ORDER_TASK_PAY_SUCCESS_AFTER);
        logger.info("OrderPaySuccessTask.orderPayAfter queue size: {}", size);
        if (size == null || size < 1) {
            return;
        }
        for (int i = 0; i < size; i++) {
            String value = redisTemplate.opsForList().rightPop(ORDER_TASK_PAY_SUCCESS_AFTER);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            try {
                Boolean success = transactionTemplate.execute(status -> processOrder(value));
                if (!Boolean.TRUE.equals(success)) {
                    redisTemplate.opsForList().leftPush(ORDER_TASK_PAY_SUCCESS_AFTER, value);
                }
            } catch (Exception exception) {
                redisTemplate.opsForList().leftPush(ORDER_TASK_PAY_SUCCESS_AFTER, value);
                logger.error("订单支付成功后置任务失败，已重新入队，orderNo = {}", value, exception);
            }
        }
    }

    private Boolean processOrder(String orderNo) {
        Map<String, Object> order = orderPaySuccessTaskMapper.selectOrderByOrderNo(orderNo);
        if (order == null) {
            throw new IllegalStateException("订单支付成功task处理，未找到订单，orderNo=" + orderNo);
        }
        Integer paid = intValue(order.get("paid"));
        if (paid == null || paid < 1) {
            throw new IllegalStateException("订单支付成功task处理，订单未支付，orderNo=" + orderNo);
        }
        Integer orderId = intValue(order.get("id"));
        if (orderPaySuccessTaskMapper.countPaySuccessLog(orderId) > 0) {
            return Boolean.TRUE;
        }

        Integer uid = intValue(order.get("uid"));
        Map<String, Object> user = orderPaySuccessTaskMapper.selectUserForUpdate(uid);
        if (user == null) {
            throw new IllegalStateException("订单支付成功task处理，未找到用户，uid=" + uid);
        }

        BigDecimal payPrice = money(order.get("payPrice"));
        Integer useIntegral = intValue(order.get("useIntegral"));
        Integer userIntegral = intValue(user.get("integral"));
        BigDecimal userBalance = money(user.get("nowMoney"));
        Integer oldExperience = intValue(user.get("experience"));
        int addExperience = payPrice.setScale(0, RoundingMode.DOWN).intValue();
        int newExperience = Math.max(0, nvl(oldExperience) + addExperience);

        orderPaySuccessTaskMapper.insertPaySuccessLog(orderId);
        orderPaySuccessTaskMapper.insertPayBill(
                uid,
                orderId,
                payPrice,
                userBalance,
                "支付" + payPrice + "元购买商品");

        if (useIntegral != null && useIntegral > 0) {
            orderPaySuccessTaskMapper.insertIntegralSubRecord(
                    uid,
                    orderNo,
                    useIntegral,
                    nvl(userIntegral),
                    "订单支付抵扣" + useIntegral + "积分购买商品");
        }

        if (addExperience > 0) {
            orderPaySuccessTaskMapper.insertExperienceRecord(
                    uid,
                    orderNo,
                    addExperience,
                    newExperience,
                    "用户付款成功增加" + addExperience + "经验");
        }
        orderPaySuccessTaskMapper.updateUserAfterPay(uid, addExperience);

        int frozenIntegralDays = intConfig("freeze_integral_day", 0);
        int orderGiveIntegral = orderGiveIntegral(payPrice);
        if (orderGiveIntegral > 0) {
            orderPaySuccessTaskMapper.insertIntegralAddRecord(
                    uid,
                    orderNo,
                    orderGiveIntegral,
                    nvl(userIntegral),
                    "用户付款成功,订单增加" + orderGiveIntegral + "积分",
                    frozenIntegralDays);
        }

        Integer productGiveIntegral = orderPaySuccessTaskMapper.sumProductGiveIntegral(orderId);
        if (productGiveIntegral != null && productGiveIntegral > 0) {
            orderPaySuccessTaskMapper.insertIntegralAddRecord(
                    uid,
                    orderNo,
                    productGiveIntegral,
                    nvl(userIntegral),
                    "用户付款成功,商品增加" + productGiveIntegral + "积分",
                    frozenIntegralDays);
        }

        assignCommission(order, user);
        maybePromoteUser(order, user);
        processPink(order);
        logger.info("OrderPaySuccessTask.orderPayAfter processed orderNo = {}", orderNo);
        return Boolean.TRUE;
    }

    private int orderGiveIntegral(BigDecimal payPrice) {
        String rate = orderPaySuccessTaskMapper.selectConfigValue("order_give_integral");
        if (!StringUtils.hasText(rate) || payPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return new BigDecimal(rate).multiply(payPrice).setScale(0, RoundingMode.DOWN).intValue();
    }

    private void assignCommission(Map<String, Object> order, Map<String, Object> user) {
        if (!"1".equals(config("brokerage_func_status", "0"))) {
            return;
        }
        if (nvl(intValue(order.get("combinationId"))) > 0
                || nvl(intValue(order.get("seckillId"))) > 0
                || nvl(intValue(order.get("bargainId"))) > 0) {
            return;
        }
        Integer uid = intValue(order.get("uid"));
        Integer spreadUid = intValue(user.get("spreadUid"));
        if (spreadUid == null || spreadUid < 1 || spreadUid.equals(uid)) {
            return;
        }

        List<Map<String, Object>> spreadUsers = spreadUsers(spreadUid);
        if (spreadUsers.isEmpty()) {
            return;
        }
        List<Map<String, Object>> orderInfoList = orderPaySuccessTaskMapper.selectOrderInfo(intValue(order.get("id")));
        int frozenDays = intConfig("extract_time", 0);
        String orderNo = stringValue(order.get("orderNo"));
        for (int i = 0; i < spreadUsers.size(); i++) {
            int level = i + 1;
            BigDecimal commission = calculateCommission(orderInfoList, level);
            if (commission.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Integer brokerageUid = intValue(spreadUsers.get(i).get("uid"));
            orderPaySuccessTaskMapper.insertBrokerageRecord(
                    brokerageUid,
                    orderNo,
                    commission,
                    "获得推广佣金，分佣" + commission,
                    frozenDays,
                    level);
        }
    }

    private List<Map<String, Object>> spreadUsers(Integer firstSpreadUid) {
        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, Object> first = orderPaySuccessTaskMapper.selectSpreadUser(firstSpreadUid);
        if (first == null || !canBrokerage(first)) {
            return records;
        }
        records.add(first);
        Integer secondSpreadUid = intValue(first.get("spreadUid"));
        if (secondSpreadUid == null || secondSpreadUid < 1) {
            return records;
        }
        Map<String, Object> second = orderPaySuccessTaskMapper.selectSpreadUser(secondSpreadUid);
        if (second != null && canBrokerage(second)) {
            records.add(second);
        }
        return records;
    }

    private boolean canBrokerage(Map<String, Object> user) {
        String model = config("store_brokerage_status", "");
        return !"1".equals(model) || nvl(intValue(user.get("isPromoter"))) == 1;
    }

    private BigDecimal calculateCommission(List<Map<String, Object>> orderInfoList, int level) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal rate = new BigDecimal(config(level == 1 ? "store_brokerage_ratio" : "store_brokerage_two", "1"))
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.DOWN);
        for (Map<String, Object> orderInfo : orderInfoList) {
            BigDecimal commission;
            if (nvl(intValue(orderInfo.get("isSub"))) == 1) {
                Map<String, Object> attrBrokerage = orderPaySuccessTaskMapper.selectAttrBrokerage(intValue(orderInfo.get("attrValueId")));
                Object value = attrBrokerage == null ? null : attrBrokerage.get(level == 1 ? "brokerage" : "brokerageTwo");
                commission = money(value);
            } else {
                BigDecimal price = money(firstPresent(orderInfo.get("vipPrice"), orderInfo.get("price")));
                commission = price.multiply(rate).setScale(2, RoundingMode.DOWN);
            }
            Integer payNum = intValue(orderInfo.get("payNum"));
            if (commission.compareTo(BigDecimal.ZERO) > 0 && payNum != null && payNum > 1) {
                commission = commission.multiply(BigDecimal.valueOf(payNum));
            }
            total = total.add(commission);
        }
        return total.setScale(2, RoundingMode.DOWN);
    }

    private void maybePromoteUser(Map<String, Object> order, Map<String, Object> user) {
        if (nvl(intValue(user.get("isPromoter"))) == 1) {
            return;
        }
        if (!"1".equals(config("brokerage_func_status", "0"))) {
            return;
        }
        String quotaConfig = config("store_brokerage_quota", "-1");
        if ("-1".equals(quotaConfig)) {
            return;
        }
        if (money(order.get("payPrice")).compareTo(new BigDecimal(quotaConfig)) >= 0) {
            orderPaySuccessTaskMapper.markPromoter(intValue(order.get("uid")));
        }
    }

    private void processPink(Map<String, Object> order) {
        if (nvl(intValue(order.get("combinationId"))) < 1) {
            return;
        }
        Integer pinkId = intValue(order.get("pinkId"));
        if (pinkId == null || pinkId < 1) {
            return;
        }
        Map<String, Object> pink = orderPaySuccessTaskMapper.selectPink(pinkId);
        if (pink == null) {
            return;
        }
        Integer kid = intValue(pink.get("kId"));
        if (kid == null || kid <= 0) {
            return;
        }
        int people = nvl(intValue(pink.get("people")));
        int memberCount = orderPaySuccessTaskMapper.countPinkMembers(kid) + 1;
        if (memberCount < people) {
            return;
        }
        List<Integer> ids = orderPaySuccessTaskMapper.selectPinkGroupIds(intValue(pink.get("cid")), kid);
        if (!ids.isEmpty()) {
            orderPaySuccessTaskMapper.markPinkGroupSuccess(ids);
        }
    }

    private int intConfig(String name, int defaultValue) {
        String value = orderPaySuccessTaskMapper.selectConfigValue(name);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private String config(String name, String defaultValue) {
        String value = orderPaySuccessTaskMapper.selectConfigValue(name);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private Object firstPresent(Object first, Object second) {
        if (first instanceof BigDecimal decimal && decimal.compareTo(BigDecimal.ZERO) > 0) {
            return decimal;
        }
        if (first instanceof Number number && number.doubleValue() > 0) {
            return first;
        }
        if (first != null && StringUtils.hasText(String.valueOf(first)) && new BigDecimal(String.valueOf(first)).compareTo(BigDecimal.ZERO) > 0) {
            return first;
        }
        return second;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private BigDecimal money(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
