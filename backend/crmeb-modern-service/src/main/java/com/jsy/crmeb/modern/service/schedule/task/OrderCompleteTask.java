package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.order.mapper.OrderCompleteTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Component("OrderCompleteTask")
public class OrderCompleteTask {
    private static final Logger logger = LoggerFactory.getLogger(OrderCompleteTask.class);
    public static final String ORDER_TASK_REDIS_KEY_AFTER_COMPLETE_BY_USER = "alterOrderCompleteByUser";

    private final StringRedisTemplate redisTemplate;
    private final OrderCompleteTaskMapper orderCompleteTaskMapper;
    private final TransactionTemplate transactionTemplate;

    public OrderCompleteTask(
            StringRedisTemplate redisTemplate,
            OrderCompleteTaskMapper orderCompleteTaskMapper,
            TransactionTemplate transactionTemplate) {
        this.redisTemplate = redisTemplate;
        this.orderCompleteTaskMapper = orderCompleteTaskMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public void orderComplete() {
        Long size = redisTemplate.opsForList().size(ORDER_TASK_REDIS_KEY_AFTER_COMPLETE_BY_USER);
        logger.info("OrderCompleteTask.orderComplete queue size: {}", size);
        if (size == null || size < 1) {
            return;
        }
        for (int i = 0; i < size; i++) {
            String value = redisTemplate.opsForList().rightPop(ORDER_TASK_REDIS_KEY_AFTER_COMPLETE_BY_USER);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            try {
                Integer orderId = Integer.valueOf(value);
                Boolean success = transactionTemplate.execute(status -> processOrder(orderId));
                if (!Boolean.TRUE.equals(success)) {
                    redisTemplate.opsForList().leftPush(ORDER_TASK_REDIS_KEY_AFTER_COMPLETE_BY_USER, value);
                }
            } catch (Exception exception) {
                redisTemplate.opsForList().leftPush(ORDER_TASK_REDIS_KEY_AFTER_COMPLETE_BY_USER, value);
                logger.error("用户订单完成后置任务失败，已重新入队，orderId = {}", value, exception);
            }
        }
    }

    private Boolean processOrder(Integer orderId) {
        if (orderCompleteTaskMapper.selectOrderId(orderId) == null) {
            throw new IllegalStateException("用户订单完成task处理，未找到订单，id=" + orderId);
        }
        if (orderCompleteTaskMapper.countCompleteLog(orderId) <= 0) {
            orderCompleteTaskMapper.insertCompleteLog(orderId);
        }
        return Boolean.TRUE;
    }
}
