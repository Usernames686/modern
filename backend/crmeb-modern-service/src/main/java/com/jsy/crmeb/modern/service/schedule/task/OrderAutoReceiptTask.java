package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.order.mapper.OrderAutoReceiptTaskMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Component("OrderAutoReceiptTask")
public class OrderAutoReceiptTask {
    private static final Logger logger = LoggerFactory.getLogger(OrderAutoReceiptTask.class);
    private static final String AUTO_TAKE_DELIVERY_DAY = "auto_take_delivery_day";
    private static final String ORDER_TASK_REDIS_KEY_AFTER_TAKE_BY_USER = "alterOrderTakeByUser";
    private static final int DEFAULT_AUTO_DAY = 14;

    private final OrderAutoReceiptTaskMapper orderAutoReceiptTaskMapper;
    private final StringRedisTemplate redisTemplate;
    private final TransactionTemplate transactionTemplate;

    public OrderAutoReceiptTask(
            OrderAutoReceiptTaskMapper orderAutoReceiptTaskMapper,
            StringRedisTemplate redisTemplate,
            TransactionTemplate transactionTemplate) {
        this.orderAutoReceiptTaskMapper = orderAutoReceiptTaskMapper;
        this.redisTemplate = redisTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    public void autoTakeDelivery() {
        int day = autoTakeDeliveryDay();
        LocalDateTime deadline = LocalDateTime.now().minusDays(day);
        List<Integer> orderIds = orderAutoReceiptTaskMapper.selectAwaitTakeDeliveryOrderIds(deadline);
        logger.info("OrderAutoReceiptTask.autoTakeDelivery deadline = {}, size = {}", deadline, orderIds.size());
        for (Integer orderId : orderIds) {
            try {
                Boolean updated = transactionTemplate.execute(status ->
                        orderAutoReceiptTaskMapper.markAutoTakeDelivery(orderId, deadline) == 1);
                if (Boolean.TRUE.equals(updated)) {
                    redisTemplate.opsForList().leftPush(ORDER_TASK_REDIS_KEY_AFTER_TAKE_BY_USER, String.valueOf(orderId));
                }
            } catch (Exception exception) {
                logger.error("自动收货异常：订单id = {}", orderId, exception);
            }
        }
    }

    private int autoTakeDeliveryDay() {
        String value = orderAutoReceiptTaskMapper.selectConfigValue(AUTO_TAKE_DELIVERY_DAY);
        if (!StringUtils.hasText(value)) {
            return DEFAULT_AUTO_DAY;
        }
        try {
            int day = Integer.parseInt(value.trim());
            return day >= 1 ? day : DEFAULT_AUTO_DAY;
        } catch (NumberFormatException exception) {
            logger.warn("自动收货天数配置无效，使用默认值 {}，value = {}", DEFAULT_AUTO_DAY, value);
            return DEFAULT_AUTO_DAY;
        }
    }
}
