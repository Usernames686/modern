package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.order.mapper.OrderCancelTaskMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Component("OrderCancelTask")
public class OrderCancelTask {
    private static final Logger logger = LoggerFactory.getLogger(OrderCancelTask.class);
    public static final String ORDER_TASK_REDIS_KEY_AFTER_CANCEL_BY_USER = "alterOrderCancelByUser";
    private static final int PRODUCT_TYPE_NORMAL = 0;
    private static final int PRODUCT_TYPE_SECKILL = 1;
    private static final int PRODUCT_TYPE_BARGAIN = 2;
    private static final int PRODUCT_TYPE_COMBINATION = 3;

    private final StringRedisTemplate redisTemplate;
    private final OrderCancelTaskMapper orderCancelTaskMapper;
    private final TransactionTemplate transactionTemplate;

    public OrderCancelTask(
            StringRedisTemplate redisTemplate,
            OrderCancelTaskMapper orderCancelTaskMapper,
            TransactionTemplate transactionTemplate) {
        this.redisTemplate = redisTemplate;
        this.orderCancelTaskMapper = orderCancelTaskMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public void userCancel() {
        Long size = redisTemplate.opsForList().size(ORDER_TASK_REDIS_KEY_AFTER_CANCEL_BY_USER);
        logger.info("OrderCancelTask.userCancel queue size: {}", size);
        if (size == null || size < 1) {
            return;
        }
        for (int i = 0; i < size; i++) {
            String value = redisTemplate.opsForList().rightPop(ORDER_TASK_REDIS_KEY_AFTER_CANCEL_BY_USER);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            try {
                Integer orderId = Integer.valueOf(value);
                Boolean success = transactionTemplate.execute(status -> processOrder(orderId));
                if (!Boolean.TRUE.equals(success)) {
                    redisTemplate.opsForList().leftPush(ORDER_TASK_REDIS_KEY_AFTER_CANCEL_BY_USER, value);
                }
            } catch (Exception exception) {
                redisTemplate.opsForList().leftPush(ORDER_TASK_REDIS_KEY_AFTER_CANCEL_BY_USER, value);
                logger.error("用户取消订单后置任务失败，已重新入队，orderId = {}", value, exception);
            }
        }
    }

    private Boolean processOrder(Integer orderId) {
        Map<String, Object> order = orderCancelTaskMapper.selectOrder(orderId);
        if (order == null) {
            throw new IllegalStateException("用户取消订单task处理，未找到订单，id=" + orderId);
        }
        orderCancelTaskMapper.insertCancelLog(orderId);
        Integer couponId = intValue(order.get("couponId"));
        if (couponId != null && couponId > 0) {
            orderCancelTaskMapper.restoreCoupon(couponId);
        }
        rollbackStock(orderId, order);
        return Boolean.TRUE;
    }

    private void rollbackStock(Integer orderId, Map<String, Object> order) {
        List<Map<String, Object>> orderInfoList = orderCancelTaskMapper.selectOrderInfo(orderId);
        if (orderInfoList.isEmpty()) {
            return;
        }
        Integer seckillId = intValue(order.get("seckillId"));
        Integer bargainId = intValue(order.get("bargainId"));
        Integer combinationId = intValue(order.get("combinationId"));
        if (seckillId != null && seckillId > 0) {
            rollbackActivityStock(orderInfoList.get(0), seckillId, PRODUCT_TYPE_SECKILL);
        } else if (bargainId != null && bargainId > 0) {
            rollbackActivityStock(orderInfoList.get(0), bargainId, PRODUCT_TYPE_BARGAIN);
        } else if (combinationId != null && combinationId > 0) {
            rollbackActivityStock(orderInfoList.get(0), combinationId, PRODUCT_TYPE_COMBINATION);
        } else {
            for (Map<String, Object> orderInfo : orderInfoList) {
                Integer productId = intValue(orderInfo.get("productId"));
                Integer attrId = intValue(orderInfo.get("attrValueId"));
                Integer payNum = intValue(orderInfo.get("payNum"));
                orderCancelTaskMapper.restoreProductStock(productId, payNum);
                orderCancelTaskMapper.restoreAttrStock(attrId, PRODUCT_TYPE_NORMAL, payNum);
            }
        }
    }

    private void rollbackActivityStock(Map<String, Object> orderInfo, Integer activityId, int productType) {
        Integer attrId = intValue(orderInfo.get("attrValueId"));
        Integer payNum = intValue(orderInfo.get("payNum"));
        if (productType == PRODUCT_TYPE_SECKILL) {
            orderCancelTaskMapper.restoreSeckillStock(activityId, payNum);
        } else if (productType == PRODUCT_TYPE_BARGAIN) {
            orderCancelTaskMapper.restoreBargainStock(activityId, payNum);
        } else {
            orderCancelTaskMapper.restoreCombinationStock(activityId, payNum);
        }
        orderCancelTaskMapper.restoreActivityAttrStock(activityId, attrId, productType, payNum);

        Integer productId = linkedProductId(activityId, productType);
        if (productId == null) {
            throw new IllegalStateException("未找到活动关联商品，activityId=" + activityId + ", type=" + productType);
        }
        orderCancelTaskMapper.restoreProductStock(productId, payNum);
        String sku = stringValue(orderInfo.get("sku"));
        for (Integer normalAttrId : orderCancelTaskMapper.selectNormalAttrIdsBySku(productId, sku)) {
            orderCancelTaskMapper.restoreAttrStock(normalAttrId, PRODUCT_TYPE_NORMAL, payNum);
        }
    }

    private Integer linkedProductId(Integer activityId, int productType) {
        if (productType == PRODUCT_TYPE_SECKILL) {
            return orderCancelTaskMapper.selectSeckillProductId(activityId);
        }
        if (productType == PRODUCT_TYPE_BARGAIN) {
            return orderCancelTaskMapper.selectBargainProductId(activityId);
        }
        return orderCancelTaskMapper.selectCombinationProductId(activityId);
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
