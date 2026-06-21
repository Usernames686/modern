package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.order.mapper.OrderReceiptTaskMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component("OrderReceiptTask")
public class OrderReceiptTask {
    private static final Logger logger = LoggerFactory.getLogger(OrderReceiptTask.class);
    public static final String ORDER_TASK_REDIS_KEY_AFTER_TAKE_BY_USER = "alterOrderTakeByUser";

    private final StringRedisTemplate redisTemplate;
    private final OrderReceiptTaskMapper orderReceiptTaskMapper;
    private final TransactionTemplate transactionTemplate;

    public OrderReceiptTask(
            StringRedisTemplate redisTemplate,
            OrderReceiptTaskMapper orderReceiptTaskMapper,
            TransactionTemplate transactionTemplate) {
        this.redisTemplate = redisTemplate;
        this.orderReceiptTaskMapper = orderReceiptTaskMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public void orderReceipt() {
        Long size = redisTemplate.opsForList().size(ORDER_TASK_REDIS_KEY_AFTER_TAKE_BY_USER);
        logger.info("OrderReceiptTask.orderReceipt queue size: {}", size);
        if (size == null || size < 1) {
            return;
        }
        for (int i = 0; i < size; i++) {
            String value = redisTemplate.opsForList().rightPop(ORDER_TASK_REDIS_KEY_AFTER_TAKE_BY_USER);
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                Integer orderId = Integer.valueOf(value);
                Boolean success = transactionTemplate.execute(status -> processOrder(orderId));
                if (!Boolean.TRUE.equals(success)) {
                    redisTemplate.opsForList().leftPush(ORDER_TASK_REDIS_KEY_AFTER_TAKE_BY_USER, value);
                }
            } catch (Exception exception) {
                redisTemplate.opsForList().leftPush(ORDER_TASK_REDIS_KEY_AFTER_TAKE_BY_USER, value);
                logger.error("订单收货后置任务失败，已重新入队，orderId = {}", value, exception);
            }
        }
    }

    private Boolean processOrder(Integer orderId) {
        Map<String, Object> order = orderReceiptTaskMapper.selectOrder(orderId);
        if (order == null) {
            throw new IllegalStateException("订单收货task处理，未找到订单，id=" + orderId);
        }
        String orderNo = stringValue(order.get("orderId"));
        Integer uid = intValue(order.get("uid"));
        if (orderReceiptTaskMapper.countNonCreateBrokerageRecords(orderNo) > 0) {
            throw new IllegalStateException("订单收货task处理，订单佣金记录不是创建状态，id=" + orderId);
        }
        long nowMillis = System.currentTimeMillis();
        int brokerageCount = orderReceiptTaskMapper.freezeBrokerageRecords(orderNo, nowMillis);
        int integralCount = orderReceiptTaskMapper.freezeIntegralRecords(orderNo, uid, nowMillis);
        orderReceiptTaskMapper.insertTakeDeliveryLog(orderId);
        logger.info(
                "OrderReceiptTask.orderReceipt processed orderId = {}, brokerage = {}, integral = {}",
                orderId,
                brokerageCount,
                integralCount);
        return Boolean.TRUE;
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
