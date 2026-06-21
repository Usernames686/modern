package com.jsy.crmeb.modern.service.front;

import com.jsy.crmeb.modern.service.front.mapper.FrontCommerceMapper;
import com.jsy.crmeb.modern.service.schedule.task.OrderPaySuccessTask;
import com.jsy.crmeb.modern.service.user.entity.User;
import com.jsy.crmeb.modern.service.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FrontPayService {
    private static final String PAY_TYPE_BALANCE = "yue";
    private static final String PAY_TYPE_WECHAT = "weixin";
    private static final String PAY_TYPE_ALIPAY = "alipay";

    private final FrontCommerceMapper commerceMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    public FrontPayService(FrontCommerceMapper commerceMapper, UserMapper userMapper, StringRedisTemplate redisTemplate) {
        this.commerceMapper = commerceMapper;
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Object> payConfig(Integer uid) {
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("yuePayStatus", true);
        response.put("payWechatOpen", true);
        response.put("aliPayStatus", true);
        response.put("dryRun", true);
        response.put("userBalance", money(user.getNowMoney()));
        response.put("payConfig", List.of(
                Map.of(
                        "name", "微信支付",
                        "value", "weixin",
                        "title", "微信快捷支付（需配置）",
                        "icon", "icon-weixinzhifu",
                        "payStatus", 1,
                        "dryRun", true),
                Map.of(
                        "name", "余额支付",
                        "value", PAY_TYPE_BALANCE,
                        "title", "可用余额",
                        "icon", "icon-yuezhifu",
                        "payStatus", 1,
                        "userBalance", money(user.getNowMoney())),
                Map.of(
                        "name", "支付宝支付",
                        "value", "alipay",
                        "title", "支付宝快捷支付（需配置）",
                        "icon", "icon-zhifubao",
                        "payStatus", 1,
                        "dryRun", true)));
        return response;
    }

    @Transactional
    public Map<String, Object> payment(Integer uid, Map<String, Object> body) {
        String orderNo = stringValue(body, "orderNo", "orderId", "uni");
        String payType = normalizePayType(stringValue(body, "payType"));
        String payChannel = normalizePayChannel(stringValue(body, "payChannel", "channel", "from"));
        if (!StringUtils.hasText(orderNo)) {
            throw new IllegalArgumentException("订单编号不能为空");
        }
        if (!StringUtils.hasText(payType)) {
            throw new IllegalArgumentException("支付类型不能为空");
        }
        Map<String, Object> order = commerceMapper.selectPayOrder(uid, orderNo);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        Map<String, Object> response = payResponse(order, payType, false);
        if (numberValue(order.get("paid")) > 0) {
            response.put("status", true);
            response.put("message", "订单已支付");
            return response;
        }

        BigDecimal payPrice = money(order.get("payPrice"));
        if (payPrice.compareTo(BigDecimal.ZERO) <= 0) {
            markPaid(uid, orderNo, PAY_TYPE_BALANCE, numberValue(order.get("useIntegral")));
            response.put("status", true);
            response.put("payType", PAY_TYPE_BALANCE);
            response.put("message", "0元订单支付成功");
            return response;
        }

        if (PAY_TYPE_WECHAT.equals(payType) || PAY_TYPE_ALIPAY.equals(payType)) {
            int channel = payChannelCode(payType, payChannel);
            commerceMapper.updateOrderPayIntent(uid, orderNo, payType, channel);
            response = payResponse(order, payType, true);
            response.put("status", true);
            response.put("paid", 0);
            response.put("dryRun", true);
            response.put("payChannel", payChannel);
            response.put("payStatus", "dry_run");
            response.put("message", payTypeName(payType) + "预支付已生成，支付服务需配置后启用，当前不会发起真实扣款");
            response.put("jsConfig", dryRunJsConfig(order, payType, payChannel));
            return response;
        }
        if (!PAY_TYPE_BALANCE.equals(payType)) {
            throw new IllegalArgumentException("该支付方式需配置后启用");
        }
        if (userMapper.deductBalanceAndIntegralForPayment(uid, payPrice, numberValue(order.get("useIntegral"))) <= 0) {
            throw new IllegalArgumentException("用户余额不足");
        }
        markPaid(uid, orderNo, PAY_TYPE_BALANCE, 0);
        response.put("status", true);
        response.put("message", "余额支付成功");
        return response;
    }

    public Map<String, Object> queryPayResult(Integer uid, String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new IllegalArgumentException("订单编号不能为空");
        }
        Map<String, Object> order = commerceMapper.selectPayOrder(uid, orderNo);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        String payType = stringOrDefault(order.get("payType"), "");
        Map<String, Object> response = payResponse(order, payType, numberValue(order.get("paid")) > 0);
        response.put("payResult", numberValue(order.get("paid")) > 0);
        response.put("payStatus", numberValue(order.get("paid")) > 0 ? "paid" : "unpaid");
        return response;
    }

    private void markPaid(Integer uid, String orderNo, String payType, Integer useIntegral) {
        if (useIntegral != null && useIntegral > 0 && userMapper.deductBalanceAndIntegralForPayment(uid, BigDecimal.ZERO, useIntegral) <= 0) {
            throw new IllegalArgumentException("用户积分不足");
        }
        if (commerceMapper.markOrderPaid(uid, orderNo, payType) <= 0) {
            throw new IllegalArgumentException("订单支付状态更新失败");
        }
        redisTemplate.opsForList().leftPush(OrderPaySuccessTask.ORDER_TASK_PAY_SUCCESS_AFTER, orderNo);
    }

    private Map<String, Object> payResponse(Map<String, Object> order, String payType, boolean status) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("paid", numberValue(order.get("paid")));
        response.put("payResult", numberValue(order.get("paid")) > 0);
        response.put("dryRun", false);
        response.put("jsConfig", null);
        response.put("payType", payType);
        response.put("orderNo", order.get("orderId"));
        response.put("orderId", order.get("orderId"));
        response.put("id", order.get("id"));
        response.put("payPrice", money(order.get("payPrice")));
        response.put("createTime", order.get("createTime"));
        response.put("payTime", order.get("payTime"));
        response.put("pinkId", order.get("pinkId"));
        response.put("combinationId", order.get("combinationId"));
        response.put("seckillId", order.get("seckillId"));
        response.put("bargainId", order.get("bargainId"));
        response.put("message", status ? "支付成功" : "订单未支付");
        return response;
    }

    private Map<String, Object> dryRunJsConfig(Map<String, Object> order, String payType, String payChannel) {
        Map<String, Object> config = new HashMap<>();
        config.put("dryRun", true);
        config.put("provider", payType);
        config.put("payChannel", payChannel);
        config.put("orderNo", order.get("orderId"));
        config.put("outTradeNo", "dryrun_" + order.get("orderId"));
        config.put("payPrice", money(order.get("payPrice")));
        if (PAY_TYPE_WECHAT.equals(payType)) {
            config.put("appId", "dry-run");
            config.put("nonceStr", "dry-run");
            config.put("package", "prepay_id=dry-run");
            config.put("signType", "MD5");
            config.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
            config.put("paySign", "dry-run");
            config.put("mwebUrl", "");
            config.put("mweb_url", "");
        }
        if (PAY_TYPE_ALIPAY.equals(payType)) {
            config.put("payUrl", "");
            config.put("orderStr", "dry-run");
        }
        return config;
    }

    private String stringOrDefault(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private String normalizePayType(String payType) {
        if (!StringUtils.hasText(payType)) {
            return "";
        }
        String value = payType.trim();
        if ("balance".equalsIgnoreCase(value) || "money".equalsIgnoreCase(value)) {
            return PAY_TYPE_BALANCE;
        }
        if ("wechat".equalsIgnoreCase(value) || "wxpay".equalsIgnoreCase(value)) {
            return PAY_TYPE_WECHAT;
        }
        if ("ali".equalsIgnoreCase(value) || "appalipay".equalsIgnoreCase(value) || "app_ali_pay".equalsIgnoreCase(value)) {
            return PAY_TYPE_ALIPAY;
        }
        return value.toLowerCase();
    }

    private String normalizePayChannel(String payChannel) {
        if (!StringUtils.hasText(payChannel)) {
            return "h5";
        }
        String value = payChannel.trim();
        if ("weixinh5".equalsIgnoreCase(value) || "h5".equalsIgnoreCase(value)) {
            return "weixinh5";
        }
        if ("public".equalsIgnoreCase(value) || "wechatPublic".equalsIgnoreCase(value)) {
            return "public";
        }
        if ("routine".equalsIgnoreCase(value) || "mini".equalsIgnoreCase(value)) {
            return "routine";
        }
        if ("weixinAppIos".equalsIgnoreCase(value) || "weixinAppAndroid".equalsIgnoreCase(value) || "app".equalsIgnoreCase(value)) {
            return value;
        }
        if ("appAliPay".equalsIgnoreCase(value) || "alipay".equalsIgnoreCase(value)) {
            return "appAliPay";
        }
        return value;
    }

    private int payChannelCode(String payType, String payChannel) {
        if (PAY_TYPE_ALIPAY.equals(payType)) {
            return 6;
        }
        if ("public".equals(payChannel)) {
            return 0;
        }
        if ("routine".equals(payChannel)) {
            return 1;
        }
        if ("weixinAppIos".equals(payChannel)) {
            return 4;
        }
        if ("weixinAppAndroid".equals(payChannel)) {
            return 5;
        }
        return 2;
    }

    private String payTypeName(String payType) {
        if (PAY_TYPE_WECHAT.equals(payType)) {
            return "微信";
        }
        if (PAY_TYPE_ALIPAY.equals(payType)) {
            return "支付宝";
        }
        return payType;
    }

    private String stringValue(Map<String, Object> body, String... names) {
        if (body == null) {
            return "";
        }
        for (String name : names) {
            Object value = body.get(name);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private int numberValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
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
}
