package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.order.mapper.OrderCompleteTaskMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component("OrderAutoCompleteTask")
public class OrderAutoCompleteTask {
    private static final Logger logger = LoggerFactory.getLogger(OrderAutoCompleteTask.class);
    private static final int COMPLETE_AFTER_DAYS = 7;

    private final StringRedisTemplate redisTemplate;
    private final OrderCompleteTaskMapper orderCompleteTaskMapper;
    private final TransactionTemplate transactionTemplate;

    public OrderAutoCompleteTask(
            StringRedisTemplate redisTemplate,
            OrderCompleteTaskMapper orderCompleteTaskMapper,
            TransactionTemplate transactionTemplate) {
        this.redisTemplate = redisTemplate;
        this.orderCompleteTaskMapper = orderCompleteTaskMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public void autoComplete() {
        List<Map<String, Object>> orders = orderCompleteTaskMapper.selectReceiptOrders();
        logger.info("OrderAutoCompleteTask.autoComplete candidate size: {}", orders.size());
        for (Map<String, Object> order : orders) {
            try {
                Integer orderId = intValue(order.get("id"));
                Boolean success = transactionTemplate.execute(status -> processOrder(order));
                if (Boolean.TRUE.equals(success)) {
                    redisTemplate.opsForList().leftPush(OrderCompleteTask.ORDER_TASK_REDIS_KEY_AFTER_COMPLETE_BY_USER, String.valueOf(orderId));
                }
            } catch (Exception exception) {
                logger.error("订单自动完成失败，orderId = {}", order.get("id"), exception);
            }
        }
    }

    private Boolean processOrder(Map<String, Object> order) {
        Integer orderId = intValue(order.get("id"));
        Map<String, Object> lastStatus = orderCompleteTaskMapper.selectLastOrderStatus(orderId);
        if (lastStatus == null || !"user_take_delivery".equals(stringValue(lastStatus.get("changeType")))) {
            logger.error("订单自动完成：订单记录最后一条不是收货状态，orderId = {}", orderId);
            return Boolean.FALSE;
        }
        LocalDateTime takeTime = localDateTime(lastStatus.get("createTime"));
        if (takeTime == null || LocalDateTime.now().isBefore(takeTime.plusDays(COMPLETE_AFTER_DAYS))) {
            return Boolean.FALSE;
        }

        List<Map<String, Object>> orderInfoList = orderCompleteTaskMapper.selectOrderInfo(orderId);
        if (orderInfoList.isEmpty()) {
            logger.error("订单自动完成：无订单详情数据，orderId = {}", orderId);
            return Boolean.FALSE;
        }
        Integer uid = intValue(order.get("uid"));
        Map<String, Object> user = orderCompleteTaskMapper.selectUser(uid);
        String nickname = displayName(user, uid);
        String avatar = user == null ? "" : stringValue(user.get("avatar"));
        for (Map<String, Object> orderInfo : orderInfoList) {
            if (nvl(intValue(orderInfo.get("isReply"))) > 0) {
                continue;
            }
            Integer productId = intValue(orderInfo.get("productId"));
            String unique = stringValue(orderInfo.get("unique"));
            if (orderCompleteTaskMapper.countProductReply(orderId, productId, unique) <= 0) {
                orderCompleteTaskMapper.insertAutoReply(
                        uid,
                        orderId,
                        unique,
                        productId,
                        nickname,
                        avatar,
                        stringValue(orderInfo.get("sku")));
            }
            orderCompleteTaskMapper.markOrderInfoReplied(intValue(orderInfo.get("id")));
        }
        return orderCompleteTaskMapper.markOrderComplete(orderId) > 0;
    }

    private String displayName(Map<String, Object> user, Integer uid) {
        if (user == null) {
            return "用户" + uid;
        }
        String nickname = stringValue(user.get("nickname"));
        if (!nickname.isBlank()) {
            return nickname;
        }
        String account = stringValue(user.get("account"));
        return account.isBlank() ? "用户" + uid : account;
    }

    private LocalDateTime localDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value == null) {
            return null;
        }
        return LocalDateTime.parse(String.valueOf(value).replace(' ', 'T'));
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

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
