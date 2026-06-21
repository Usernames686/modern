package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.order.mapper.OrderCancelTaskMapper;
import com.jsy.crmeb.modern.service.order.mapper.OrderRefundTaskMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Component("OrderRefundTask")
public class OrderRefundTask {
    private static final Logger logger = LoggerFactory.getLogger(OrderRefundTask.class);
    public static final String ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER = "alterOrderRefundByUser";
    private static final int PRODUCT_TYPE_NORMAL = 0;
    private static final int PRODUCT_TYPE_SECKILL = 1;
    private static final int PRODUCT_TYPE_BARGAIN = 2;
    private static final int PRODUCT_TYPE_COMBINATION = 3;

    private final StringRedisTemplate redisTemplate;
    private final OrderRefundTaskMapper orderRefundTaskMapper;
    private final OrderCancelTaskMapper orderCancelTaskMapper;
    private final TransactionTemplate transactionTemplate;

    public OrderRefundTask(
            StringRedisTemplate redisTemplate,
            OrderRefundTaskMapper orderRefundTaskMapper,
            OrderCancelTaskMapper orderCancelTaskMapper,
            TransactionTemplate transactionTemplate) {
        this.redisTemplate = redisTemplate;
        this.orderRefundTaskMapper = orderRefundTaskMapper;
        this.orderCancelTaskMapper = orderCancelTaskMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public void orderRefund() {
        Long size = redisTemplate.opsForList().size(ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER);
        logger.info("OrderRefundTask.orderRefund queue size: {}", size);
        if (size == null || size < 1) {
            return;
        }
        for (int i = 0; i < size; i++) {
            String value = redisTemplate.opsForList().rightPop(ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            try {
                Integer orderId = Integer.valueOf(value);
                Boolean success = transactionTemplate.execute(status -> processOrder(orderId));
                if (!Boolean.TRUE.equals(success)) {
                    redisTemplate.opsForList().leftPush(ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER, value);
                }
            } catch (Exception exception) {
                redisTemplate.opsForList().leftPush(ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER, value);
                logger.error("订单退款后置任务失败，已重新入队，orderId = {}", value, exception);
            }
        }
    }

    private Boolean processOrder(Integer orderId) {
        Map<String, Object> order = orderRefundTaskMapper.selectOrder(orderId);
        if (order == null) {
            throw new IllegalStateException("订单退款task处理，未找到订单，id=" + orderId);
        }
        Integer refundStatus = intValue(order.get("refundStatus"));
        if (Integer.valueOf(2).equals(refundStatus)) {
            return Boolean.TRUE;
        }
        Integer uid = intValue(order.get("uid"));
        String orderNo = stringValue(order.get("orderNo"));
        BigDecimal refundPrice = decimalValue(order.get("refundPrice"));

        restoreIntegral(uid, orderNo);
        restoreExperience(uid, orderNo);
        int brokerageCount = orderRefundTaskMapper.invalidateBrokerage(orderNo);
        rollbackStock(orderId, order);
        Integer couponId = intValue(order.get("couponId"));
        if (couponId != null && couponId > 0) {
            orderRefundTaskMapper.restoreCoupon(couponId);
        }
        Integer combinationId = intValue(order.get("combinationId"));
        if (combinationId != null && combinationId > 0) {
            orderRefundTaskMapper.markPinkRefunded(orderNo);
        }
        orderRefundTaskMapper.markRefunded(orderId);
        if (orderRefundTaskMapper.countRefundSuccessLog(orderId, refundPrice) == 0) {
            orderRefundTaskMapper.insertRefundLog(orderId, "退款给用户" + refundPrice.toPlainString() + "元成功");
        }
        logger.info("OrderRefundTask.orderRefund processed orderId = {}, brokerage invalidated = {}", orderId, brokerageCount);
        return Boolean.TRUE;
    }

    private void restoreIntegral(Integer uid, String orderNo) {
        Integer integral = orderRefundTaskMapper.selectRefundIntegral(uid, orderNo);
        if (integral != null && integral > 0) {
            orderRefundTaskMapper.restoreUserIntegral(uid, integral);
            Integer balance = orderRefundTaskMapper.selectUserIntegral(uid);
            orderRefundTaskMapper.insertRefundIntegralRecord(
                    uid,
                    orderNo,
                    integral,
                    balance == null ? integral : balance,
                    "订单退款，返还支付扣除得" + integral + "积分");
        }
        orderRefundTaskMapper.invalidateOrderAddIntegral(uid, orderNo);
    }

    private void restoreExperience(Integer uid, String orderNo) {
        Integer experience = orderRefundTaskMapper.selectRefundExperience(uid, orderNo);
        if (experience == null || experience <= 0) {
            return;
        }
        orderRefundTaskMapper.deductUserExperience(uid, experience);
        Integer balance = orderRefundTaskMapper.selectUserExperience(uid);
        orderRefundTaskMapper.insertRefundExperienceRecord(
                uid,
                orderNo,
                experience,
                balance == null ? 0 : balance,
                "订单退款，扣除" + experience + "赠送经验");
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

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(value));
    }
}
