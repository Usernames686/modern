package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.order.mapper.OrderAutoCancelTaskMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Component("OrderAutoCancelTask")
public class OrderAutoCancelTask {
    private static final Logger logger = LoggerFactory.getLogger(OrderAutoCancelTask.class);
    public static final String ORDER_AUTO_CANCEL_KEY = "order_auto_cancel_key";
    private static final int PRODUCT_TYPE_NORMAL = 0;
    private static final int PRODUCT_TYPE_SECKILL = 1;
    private static final int PRODUCT_TYPE_BARGAIN = 2;
    private static final int PRODUCT_TYPE_COMBINATION = 3;

    private final StringRedisTemplate redisTemplate;
    private final OrderAutoCancelTaskMapper orderAutoCancelTaskMapper;
    private final TransactionTemplate transactionTemplate;

    public OrderAutoCancelTask(
            StringRedisTemplate redisTemplate,
            OrderAutoCancelTaskMapper orderAutoCancelTaskMapper,
            TransactionTemplate transactionTemplate) {
        this.redisTemplate = redisTemplate;
        this.orderAutoCancelTaskMapper = orderAutoCancelTaskMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public void autoCancel() {
        Long size = redisTemplate.opsForList().size(ORDER_AUTO_CANCEL_KEY);
        logger.info("OrderAutoCancelTask.autoCancel queue size: {}", size);
        if (size == null || size < 1) {
            return;
        }
        for (int i = 0; i < size; i++) {
            String value = redisTemplate.opsForList().rightPop(ORDER_AUTO_CANCEL_KEY);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            try {
                Boolean success = transactionTemplate.execute(status -> processOrder(value));
                if (!Boolean.TRUE.equals(success)) {
                    redisTemplate.opsForList().leftPush(ORDER_AUTO_CANCEL_KEY, value);
                }
            } catch (Exception exception) {
                redisTemplate.opsForList().leftPush(ORDER_AUTO_CANCEL_KEY, value);
                logger.error("系统自动取消未支付订单任务失败，已重新入队，orderNo = {}", value, exception);
            }
        }
    }

    private Boolean processOrder(String orderNo) {
        Map<String, Object> order = orderAutoCancelTaskMapper.selectOrderByOrderNo(orderNo);
        if (order == null) {
            throw new IllegalStateException("系统自动取消未支付订单task处理，未找到订单，orderNo=" + orderNo);
        }
        if (nvl(intValue(order.get("paid"))) > 0
                || nvl(intValue(order.get("isDel"))) > 0
                || nvl(intValue(order.get("isSystemDel"))) > 0) {
            return Boolean.TRUE;
        }
        if (!expired(order)) {
            return Boolean.FALSE;
        }
        Integer orderId = intValue(order.get("id"));
        if (orderAutoCancelTaskMapper.markSystemDeleted(orderId) <= 0) {
            return Boolean.TRUE;
        }
        orderAutoCancelTaskMapper.insertAutoCancelLog(orderId);
        Integer couponId = intValue(order.get("couponId"));
        if (couponId != null && couponId > 0) {
            orderAutoCancelTaskMapper.restoreCoupon(couponId);
        }
        rollbackStock(orderId, order);
        logger.info("OrderAutoCancelTask.autoCancel processed orderNo = {}", orderNo);
        return Boolean.TRUE;
    }

    private boolean expired(Map<String, Object> order) {
        LocalDateTime createTime = localDateTime(order.get("createTime"));
        if (createTime == null) {
            throw new IllegalStateException("系统自动取消未支付订单task处理，订单创建时间为空");
        }
        Integer orderType = intValue(order.get("type"));
        LocalDateTime expireTime;
        if (orderType != null && orderType == 1) {
            expireTime = createTime.plusMinutes(3);
        } else {
            int hours = intConfig(activityOrder(order) ? "order_activity_time" : "order_cancel_time", 1);
            expireTime = createTime.plusHours(hours);
        }
        return !LocalDateTime.now().isBefore(expireTime);
    }

    private boolean activityOrder(Map<String, Object> order) {
        return nvl(intValue(order.get("seckillId"))) > 0
                || nvl(intValue(order.get("bargainId"))) > 0
                || nvl(intValue(order.get("combinationId"))) > 0;
    }

    private void rollbackStock(Integer orderId, Map<String, Object> order) {
        List<Map<String, Object>> orderInfoList = orderAutoCancelTaskMapper.selectOrderInfo(orderId);
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
                orderAutoCancelTaskMapper.restoreProductStock(productId, payNum);
                orderAutoCancelTaskMapper.restoreAttrStock(attrId, PRODUCT_TYPE_NORMAL, payNum);
            }
        }
    }

    private void rollbackActivityStock(Map<String, Object> orderInfo, Integer activityId, int productType) {
        Integer attrId = intValue(orderInfo.get("attrValueId"));
        Integer payNum = intValue(orderInfo.get("payNum"));
        if (productType == PRODUCT_TYPE_SECKILL) {
            orderAutoCancelTaskMapper.restoreSeckillStock(activityId, payNum);
        } else if (productType == PRODUCT_TYPE_BARGAIN) {
            orderAutoCancelTaskMapper.restoreBargainStock(activityId, payNum);
        } else {
            orderAutoCancelTaskMapper.restoreCombinationStock(activityId, payNum);
        }
        orderAutoCancelTaskMapper.restoreActivityAttrStock(activityId, attrId, productType, payNum);

        Integer productId = linkedProductId(activityId, productType);
        if (productId == null) {
            throw new IllegalStateException("未找到活动关联商品，activityId=" + activityId + ", type=" + productType);
        }
        orderAutoCancelTaskMapper.restoreProductStock(productId, payNum);
        String sku = stringValue(orderInfo.get("sku"));
        for (Integer normalAttrId : orderAutoCancelTaskMapper.selectNormalAttrIdsBySku(productId, sku)) {
            orderAutoCancelTaskMapper.restoreAttrStock(normalAttrId, PRODUCT_TYPE_NORMAL, payNum);
        }
    }

    private Integer linkedProductId(Integer activityId, int productType) {
        if (productType == PRODUCT_TYPE_SECKILL) {
            return orderAutoCancelTaskMapper.selectSeckillProductId(activityId);
        }
        if (productType == PRODUCT_TYPE_BARGAIN) {
            return orderAutoCancelTaskMapper.selectBargainProductId(activityId);
        }
        return orderAutoCancelTaskMapper.selectCombinationProductId(activityId);
    }

    private int intConfig(String name, int defaultValue) {
        String value = orderAutoCancelTaskMapper.selectConfigValue(name);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return Integer.parseInt(value);
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
