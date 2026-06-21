package com.jsy.crmeb.modern.service.front;

import com.jsy.crmeb.modern.common.web.PageResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.service.coupon.entity.StoreCoupon;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponUserResponse;
import com.jsy.crmeb.modern.service.coupon.entity.StoreCouponUser;
import com.jsy.crmeb.modern.service.coupon.mapper.StoreCouponMapper;
import com.jsy.crmeb.modern.service.coupon.mapper.StoreCouponUserMapper;
import com.jsy.crmeb.modern.service.front.mapper.FrontCommerceMapper;
import com.jsy.crmeb.modern.service.product.entity.StoreProduct;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductMapper;
import com.jsy.crmeb.modern.service.schedule.task.OrderAutoCancelTask;
import com.jsy.crmeb.modern.service.schedule.task.OrderCancelTask;
import com.jsy.crmeb.modern.service.schedule.task.OrderReceiptTask;
import com.jsy.crmeb.modern.service.user.entity.User;
import com.jsy.crmeb.modern.service.user.mapper.UserMapper;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FrontCommerceService {
    private static final int PRODUCT_TYPE_SECKILL = 1;
    private static final int PRODUCT_TYPE_BARGAIN = 2;
    private static final int PRODUCT_TYPE_COMBINATION = 3;

    private final FrontCommerceMapper commerceMapper;
    private final StoreProductMapper productMapper;
    private final UserMapper userMapper;
    private final StoreCouponMapper couponMapper;
    private final StoreCouponUserMapper couponUserMapper;
    private final StringRedisTemplate redisTemplate;

    public FrontCommerceService(FrontCommerceMapper commerceMapper,
                                StoreProductMapper productMapper,
                                UserMapper userMapper,
                                StoreCouponMapper couponMapper,
                                StoreCouponUserMapper couponUserMapper,
                                StringRedisTemplate redisTemplate) {
        this.commerceMapper = commerceMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.couponMapper = couponMapper;
        this.couponUserMapper = couponUserMapper;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public Map<String, String> addCart(Integer uid, Map<String, Object> body) {
        Integer productId = intValue(body, "productId", "product_id", "id");
        Integer cartNum = Math.max(1, intValue(body, "cartNum", "cart_num", "number"));
        String type = stringValue(body, "type");
        if (!StringUtils.hasText(type)) {
            type = "product";
        }
        if (productId == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        StoreProduct product = productMapper.selectById(productId);
        if (product == null
                || !Integer.valueOf(1).equals(product.getIsShow())
                || Integer.valueOf(1).equals(product.getIsDel())
                || Integer.valueOf(1).equals(product.getIsRecycle())) {
            throw new IllegalArgumentException("商品不存在或已下架");
        }

        String uniqueId = stringValue(body, "uniqueId", "unique", "productAttrUnique", "product_attr_unique");
        Map<String, Object> sku = null;
        if (StringUtils.hasText(uniqueId)) {
            sku = commerceMapper.selectSku(productId, Integer.valueOf(uniqueId));
        }
        if (sku == null) {
            sku = commerceMapper.selectFirstSku(productId);
        }
        if (sku == null || sku.get("id") == null) {
            throw new IllegalArgumentException("商品规格不存在");
        }
        uniqueId = String.valueOf(sku.get("id"));

        Integer cartId = commerceMapper.selectActiveCartId(uid, productId, uniqueId, type);
        if (cartId == null) {
            commerceMapper.insertCart(uid, type, productId, uniqueId, cartNum);
            cartId = commerceMapper.selectActiveCartId(uid, productId, uniqueId, type);
        } else {
            commerceMapper.incrementCartNum(cartId, uid, cartNum);
        }
        return Map.of("cartId", String.valueOf(cartId));
    }

    public PageResponse<Map<String, Object>> cartList(Integer uid, boolean isValid, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        List<Map<String, Object>> list = commerceMapper.selectCartList(uid, isValid, offset, safeLimit).stream()
                .map(this::normalizeCart)
                .toList();
        return new PageResponse<>(safePage, safeLimit, commerceMapper.countCart(uid, isValid), list);
    }

    public Map<String, Integer> cartCount(Integer uid, Boolean numType, String type) {
        Integer count = commerceMapper.countCartNum(uid);
        return Map.of("count", count == null ? 0 : count);
    }

    public void updateCartNum(Integer uid, Integer id, Integer number) {
        if (id == null || number == null || number <= 0) {
            throw new IllegalArgumentException("购物车参数错误");
        }
        if (commerceMapper.updateCartNum(id, uid, number) <= 0) {
            throw new IllegalArgumentException("购物车商品不存在");
        }
    }

    @Transactional
    public Map<String, Object> resetCart(Integer uid, Map<String, Object> body) {
        Integer id = intValue(body, "id", "cartId", "cart_id");
        Integer number = intValue(body, "num", "number", "cartNum", "cart_num");
        Integer productId = intValue(body, "productId", "product_id");
        Integer unique = intValue(body, "unique", "uniqueId", "productAttrUnique", "product_attr_unique");
        if (id == null || productId == null || unique == null) {
            throw new IllegalArgumentException("购物车参数错误");
        }
        if (number == null || number <= 0 || number >= 999) {
            throw new IllegalArgumentException("数量不合法");
        }
        Map<String, Object> cart = commerceMapper.selectCartRowById(uid, id);
        if (cart == null) {
            throw new IllegalArgumentException("购物车不存在");
        }
        if (!productId.equals(numberValue(cart.get("productId")))) {
            throw new IllegalArgumentException("购物车商品不匹配");
        }
        if (numberValue(cart.get("isShow")) != 1
                || numberValue(cart.get("isDel")) > 0
                || numberValue(cart.get("isRecycle")) > 0) {
            throw new IllegalArgumentException("商品不存在或已下架");
        }
        Map<String, Object> sku = commerceMapper.selectSku(productId, unique);
        if (sku == null || sku.get("id") == null) {
            throw new IllegalArgumentException("商品规格不存在");
        }
        if (numberValue(sku.get("stock")) < number) {
            throw new IllegalArgumentException("商品库存不足");
        }
        if (commerceMapper.resetCart(uid, id, productId, String.valueOf(unique), number) <= 0) {
            throw new IllegalArgumentException("重选添加购物车失败");
        }
        Map<String, Object> updated = commerceMapper.selectCartRowById(uid, id);
        return Map.of(
                "cartId", String.valueOf(id),
                "id", id,
                "cartInfo", updated == null ? Map.of() : normalizeCart(updated));
    }

    public void deleteCart(Integer uid, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请选择要删除的购物车商品");
        }
        commerceMapper.deleteCart(uid, ids);
    }

    public PageResponse<Map<String, Object>> addressList(Integer uid, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        return new PageResponse<>(
                safePage,
                safeLimit,
                commerceMapper.countAddress(uid),
                commerceMapper.selectAddressList(uid, offset, safeLimit).stream().map(this::normalizeAddress).toList());
    }

    public Map<String, Object> addressDetail(Integer uid, Integer id) {
        Map<String, Object> address = commerceMapper.selectAddressById(uid, id);
        if (address == null) {
            throw new IllegalArgumentException("地址不存在");
        }
        return normalizeAddress(address);
    }

    public Map<String, Object> defaultAddress(Integer uid) {
        Map<String, Object> address = commerceMapper.selectDefaultAddress(uid);
        return address == null ? Map.of() : normalizeAddress(address);
    }

    @Transactional
    public Map<String, Object> saveAddress(Integer uid, Map<String, Object> body) {
        Map<String, Object> address = addressParams(uid, body);
        if (Integer.valueOf(1).equals(address.get("isDefault"))) {
            commerceMapper.clearDefaultAddress(uid);
        }
        Integer id = intValue(body, "id");
        if (id == null || id <= 0) {
            commerceMapper.insertAddress(address);
            id = commerceMapper.selectLatestAddressId(uid);
        } else {
            address.put("id", id);
            if (commerceMapper.updateAddress(address) <= 0) {
                throw new IllegalArgumentException("地址不存在");
            }
        }
        return addressDetail(uid, id);
    }

    public void deleteAddress(Integer uid, Integer id) {
        if (commerceMapper.deleteAddress(uid, id) <= 0) {
            throw new IllegalArgumentException("地址不存在");
        }
    }

    @Transactional
    public Map<String, Object> setDefaultAddress(Integer uid, Integer id) {
        commerceMapper.clearDefaultAddress(uid);
        if (commerceMapper.setDefaultAddress(uid, id) <= 0) {
            throw new IllegalArgumentException("地址不存在");
        }
        return addressDetail(uid, id);
    }

    public Map<String, Object> orderConfirm(Integer uid, Map<String, Object> body) {
        List<Integer> cartIds = cartIdsFromBody(body);
        if (cartIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要购买的商品");
        }
        return preOrder(uid, Map.of("preOrderType", "shoppingCart", "cartIds", cartIds));
    }

    public Map<String, Object> preOrder(Integer uid, Map<String, Object> body) {
        String preOrderType = stringValue(body, "preOrderType", "type");
        if ("again".equals(preOrderType)) {
            String orderNo = againOrderNo(body);
            List<Map<String, Object>> rows = checkedAgainRows(uid, orderNo);
            String preOrderNo = encodePreOrderNo(uid, "again", orderNo);
            return preOrderResponse(uid, preOrderNo, rows);
        }
        if ("buyNow".equals(preOrderType)) {
            BuyNowDetail detail = buyNowDetail(body);
            if (positive(detail.seckillId())) {
                List<Map<String, Object>> rows = checkedSeckillRows(uid, detail);
                String preOrderNo = encodePreOrderNo(uid, "buyNowSeckill", detail.encode());
                return preOrderResponse(uid, preOrderNo, rows);
            }
            if (positive(detail.bargainId())) {
                List<Map<String, Object>> rows = checkedBargainRows(uid, detail);
                String preOrderNo = encodePreOrderNo(uid, "buyNowBargain", detail.encode());
                return preOrderResponse(uid, preOrderNo, rows);
            }
            if (positive(detail.combinationId())) {
                List<Map<String, Object>> rows = checkedCombinationRows(uid, detail);
                String preOrderNo = encodePreOrderNo(uid, "buyNowCombination", detail.encode());
                return preOrderResponse(uid, preOrderNo, rows);
            }
            List<Map<String, Object>> rows = checkedBuyNowProductRows(detail);
            String preOrderNo = encodePreOrderNo(uid, "buyNowProduct", detail.encode());
            return preOrderResponse(uid, preOrderNo, rows);
        }
        List<Integer> cartIds = cartIdsFromBody(body);
        if (cartIds.isEmpty()) {
            List<?> orderDetails = body == null ? List.of() : (List<?>) body.getOrDefault("orderDetails", List.of());
            for (Object detail : orderDetails) {
                if (detail instanceof Map<?, ?> map) {
                    Object cartId = map.get("cartId");
                    if (cartId != null) {
                        cartIds.add(Integer.valueOf(String.valueOf(cartId)));
                    }
                }
            }
        }
        if (cartIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要购买的商品");
        }
        List<Map<String, Object>> rows = checkedCartRows(uid, cartIds);
        String preOrderNo = encodePreOrderNo(uid, "shoppingCart", cartIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        return preOrderResponse(uid, preOrderNo, rows);
    }

    public Map<String, Object> loadPreOrder(Integer uid, String preOrderNo) {
        PreOrderSource source = decodePreOrderNo(uid, preOrderNo);
        return preOrderResponse(uid, preOrderNo, checkedPreOrderRows(uid, source));
    }

    public Map<String, Object> computedPrice(Integer uid, Map<String, Object> body) {
        String preOrderNo = stringValue(body, "preOrderNo");
        List<Map<String, Object>> rows = checkedPreOrderRows(uid, decodePreOrderNo(uid, preOrderNo));
        return priceGroup(uid, rows, Boolean.TRUE.equals(body == null ? null : body.get("useIntegral")), intValue(body, "couponId"));
    }

    public Map<String, Object> storeList(Map<String, Object> body) {
        int page = Math.max(1, intValueOrDefault(body, "page", 1));
        int limit = Math.max(1, Math.min(100, intValueOrDefault(body, "limit", 20)));
        int offset = (page - 1) * limit;
        List<Map<String, Object>> list = commerceMapper.selectStoreList(offset, limit).stream()
                .map(this::normalizeStore)
                .toList();
        Map<String, Object> response = new HashMap<>();
        response.put("list", list);
        response.put("total", commerceMapper.countStoreList());
        return response;
    }

    @Transactional
    public Map<String, Object> createOrder(Integer uid, Map<String, Object> body) {
        String preOrderNo = stringValue(body, "preOrderNo");
        PreOrderSource source = decodePreOrderNo(uid, preOrderNo);
        List<Map<String, Object>> rows = checkedPreOrderRows(uid, source);
        Integer couponId = intValue(body, "couponId");
        Map<String, Object> priceGroup = priceGroup(uid, rows, Boolean.TRUE.equals(body == null ? null : body.get("useIntegral")), couponId);
        Integer shippingType = intValue(body, "shippingType");
        if (shippingType == null) {
            shippingType = 1;
        }
        Map<String, Object> address;
        Map<String, Object> store = null;
        if (shippingType == 2) {
            if (!selfMentionEnabled()) {
                throw new IllegalArgumentException("请先联系管理员开启门店自提");
            }
            Integer storeId = intValue(body, "storeId");
            store = storeId == null ? null : commerceMapper.selectStoreById(storeId);
            if (store == null) {
                throw new IllegalArgumentException("暂无门店无法选择门店自提");
            }
            address = pickupAddress(body, store);
        } else {
            Integer addressId = intValue(body, "addressId");
            address = addressId == null ? commerceMapper.selectDefaultAddress(uid) : commerceMapper.selectAddressById(uid, addressId);
            if (address == null) {
                throw new IllegalArgumentException("请选择收货地址");
            }
        }

        String orderNo = createOrderNo();
        Map<String, Object> order = new HashMap<>();
        Map<String, Object> activity = activityParams(rows);
        order.put("orderId", orderNo);
        order.put("uid", uid);
        order.put("realName", stringOrDefault(address.get("realName"), ""));
        order.put("userPhone", stringOrDefault(address.get("phone"), ""));
        order.put("userAddress", fullAddress(address));
        order.put("totalNum", rows.stream().mapToInt(row -> numberValue(row.get("cartNum"))).sum());
        order.put("totalPrice", priceGroup.get("proTotalFee"));
        order.put("payPrice", priceGroup.get("payFee"));
        order.put("mark", stringValue(body, "mark"));
        order.put("cost", totalCost(rows));
        order.put("shippingType", shippingType);
        order.put("proTotalPrice", priceGroup.get("proTotalFee"));
        order.put("couponId", priceGroup.get("couponId"));
        order.put("couponPrice", priceGroup.get("couponFee"));
        order.put("deductionPrice", priceGroup.get("deductionPrice"));
        order.put("storeId", store == null ? 0 : store.get("id"));
        order.put("verifyCode", shippingType == 2 ? createVerifyCode() : "");
        order.put("seckillId", activity.get("seckillId"));
        order.put("bargainId", activity.get("bargainId"));
        order.put("combinationId", activity.get("combinationId"));
        order.put("pinkId", activity.get("pinkId"));
        order.put("bargainUserId", activity.get("bargainUserId"));
        lockActivityStockIfNeeded(rows);
        commerceMapper.insertOrder(order);
        Integer orderDbId = commerceMapper.selectOrderDbId(uid, orderNo);
        if (orderDbId == null) {
            throw new IllegalArgumentException("创建订单失败");
        }
        commerceMapper.insertOrderInfos(orderInfoRows(orderDbId, orderNo, rows));
        if (source.isShoppingCart()) {
            commerceMapper.deleteCart(uid, source.cartIds());
        }
        if (numberValue(priceGroup.get("couponId")) > 0) {
            commerceMapper.useCoupon(uid, numberValue(priceGroup.get("couponId")));
        }
        redisTemplate.opsForList().leftPush(OrderAutoCancelTask.ORDER_AUTO_CANCEL_KEY, orderNo);

        Map<String, Object> response = new HashMap<>();
        response.put("orderNo", orderNo);
        response.put("orderId", orderNo);
        response.put("id", orderDbId);
        response.put("payPrice", priceGroup.get("payFee"));
        response.put("status", "待付款");
        return response;
    }

    public List<Map<String, Object>> orderCoupons(Integer uid, String preOrderNo) {
        List<Map<String, Object>> rows = checkedPreOrderRows(uid, decodePreOrderNo(uid, preOrderNo));
        BigDecimal proTotal = productTotal(rows);
        List<Integer> productIds = rows.stream()
                .map(row -> numberValue(row.get("productId")))
                .filter(id -> id > 0)
                .distinct()
                .toList();
        List<Integer> categoryIds = categoryIds(rows);
        return commerceMapper.selectCouponsForOrder(uid, proTotal, productIds, categoryIds).stream()
                .map(coupon -> {
                    Map<String, Object> item = new HashMap<>(coupon);
                    item.put("id", coupon.get("id"));
                    item.put("minPrice", coupon.get("minPrice"));
                    item.put("useType", coupon.get("useType"));
                    item.put("day", 0);
                    item.put("isUse", 0);
                    item.put("use_title", "");
                    return item;
                })
                .toList();
    }

    public PageResponse<StoreCouponUserResponse> userCoupons(Integer uid, String type, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        QueryWrapper<StoreCouponUser> query = new QueryWrapper<>();
        query.eq("uid", uid);
        LocalDateTime now = LocalDateTime.now();
        if ("usable".equals(type)) {
            query.eq("status", 0);
            query.and(wrapper -> wrapper.isNull("end_time").or().gt("end_time", now));
            query.orderByDesc("id");
        } else if ("unusable".equals(type)) {
            query.and(wrapper -> wrapper.gt("status", 0)
                    .or(nested -> nested.eq("status", 0).isNotNull("end_time").le("end_time", now)));
            query.orderByDesc("status").orderByDesc("id");
        } else {
            query.orderByAsc("status").orderByDesc("id");
        }
        Page<StoreCouponUser> couponPage = couponUserMapper.selectPage(new Page<>(safePage, safeLimit), query);
        List<StoreCouponUserResponse> list = couponPage.getRecords().stream()
                .map(this::toUserCouponResponse)
                .toList();
        return new PageResponse<>(safePage, safeLimit, couponPage.getTotal(), list);
    }

    public PageResponse<Map<String, Object>> receiveCoupons(Integer uid, Integer type, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        QueryWrapper<StoreCoupon> query = new QueryWrapper<>();
        query.eq("is_del", 0);
        query.eq("status", 1);
        query.eq("type", 1);
        if (type != null && type >= 1 && type <= 3) {
            query.eq("use_type", type);
        }
        query.and(wrapper -> wrapper.eq("is_limited", 0).or().gt("last_total", 0));
        LocalDateTime now = LocalDateTime.now();
        query.and(wrapper -> wrapper.isNull("receive_start_time").or().le("receive_start_time", now));
        query.and(wrapper -> wrapper.isNull("receive_end_time").or().gt("receive_end_time", now));
        query.orderByDesc("sort").orderByDesc("id");
        Page<StoreCoupon> couponPage = couponMapper.selectPage(new Page<>(safePage, safeLimit), query);
        Set<Integer> receivedIds = uid == null ? Set.of() : receivedCouponIds(uid, couponPage.getRecords());
        List<Map<String, Object>> list = couponPage.getRecords().stream()
                .map(coupon -> toReceiveCouponResponse(coupon, receivedIds.contains(coupon.getId())))
                .toList();
        return new PageResponse<>(safePage, safeLimit, couponPage.getTotal(), list);
    }

    @Transactional
    public boolean receiveCoupon(Integer uid, Integer couponId) {
        if (couponId == null || couponId <= 0) {
            throw new IllegalArgumentException("优惠券id不能为空");
        }
        StoreCoupon coupon = couponMapper.selectById(couponId);
        validateReceiveCoupon(coupon);
        QueryWrapper<StoreCouponUser> existingQuery = new QueryWrapper<>();
        existingQuery.eq("coupon_id", couponId).eq("uid", uid);
        if (couponUserMapper.selectCount(existingQuery) > 0) {
            throw new IllegalArgumentException("当前用户已经领取过此优惠券了！");
        }

        LocalDateTime startTime = coupon.getUseStartTime();
        LocalDateTime endTime = coupon.getUseEndTime();
        if (!Boolean.TRUE.equals(coupon.getIsFixedTime())) {
            startTime = LocalDateTime.now();
            endTime = startTime.plusDays(coupon.getDay() == null ? 0 : coupon.getDay());
        }

        StoreCouponUser couponUser = new StoreCouponUser();
        couponUser.setCouponId(coupon.getId());
        couponUser.setCid(0);
        couponUser.setUid(uid);
        couponUser.setName(coupon.getName());
        couponUser.setMoney(coupon.getMoney());
        couponUser.setMinPrice(coupon.getMinPrice());
        couponUser.setType("get");
        couponUser.setStatus(0);
        couponUser.setStartTime(startTime);
        couponUser.setEndTime(endTime);
        couponUser.setUseType(coupon.getUseType());
        if (coupon.getUseType() != null && coupon.getUseType() > 1) {
            couponUser.setPrimaryKey(coupon.getPrimaryKey());
        }
        couponUserMapper.insert(couponUser);

        if (Boolean.TRUE.equals(coupon.getIsLimited())) {
            StoreCoupon update = new StoreCoupon();
            update.setId(coupon.getId());
            update.setLastTotal(Math.max(0, coupon.getLastTotal() - 1));
            update.setUpdateTime(LocalDateTime.now());
            couponMapper.updateById(update);
        }
        return true;
    }

    public PageResponse<Map<String, Object>> orderList(Integer uid, String keywords, Integer type, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        List<Map<String, Object>> orders = commerceMapper.selectOrders(uid, keywords, type, null, offset, safeLimit);
        attachOrderInfos(orders);
        return new PageResponse<>(safePage, safeLimit, commerceMapper.countOrders(uid, keywords, type, null), orders);
    }

    public PageResponse<Map<String, Object>> refundOrderList(Integer uid, Integer refundStatus, Integer legacyType, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        Integer safeRefundStatus = normalizeRefundStatus(refundStatus == null ? legacyType : refundStatus);
        List<Map<String, Object>> orders = commerceMapper.selectOrders(uid, null, -3, safeRefundStatus, offset, safeLimit);
        attachOrderInfos(orders);
        return new PageResponse<>(safePage, safeLimit, commerceMapper.countOrders(uid, null, -3, safeRefundStatus), orders);
    }

    public Map<String, Object> orderDetail(Integer uid, String orderId) {
        Map<String, Object> order = commerceMapper.selectOrderDetail(uid, orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        attachOrderInfos(List.of(order));
        return order;
    }

    public Map<String, Object> refundApplyOrderInfo(Integer uid, String orderId) {
        Map<String, Object> order = commerceMapper.selectOrderDetail(uid, orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        attachOrderInfos(List.of(order));
        return order;
    }

    public Map<String, Object> orderProduct(Integer uid, Map<String, Object> body) {
        String unique = stringValue(body, "uni", "unique");
        String orderNo = stringValue(body, "orderNo", "order_id");
        String rawOrderId = stringValue(body, "orderId", "id", "oid");
        Integer orderDbId = null;
        if (StringUtils.hasText(rawOrderId) && rawOrderId.chars().allMatch(Character::isDigit)) {
            orderDbId = Integer.valueOf(rawOrderId);
        } else if (!StringUtils.hasText(orderNo)) {
            orderNo = rawOrderId;
        }
        Map<String, Object> row = orderDbId == null
                ? commerceMapper.selectOrderInfoForReplyByOrderNo(uid, orderNo, unique)
                : commerceMapper.selectOrderInfoForReplyByOrderId(uid, orderDbId, unique);
        if (row == null) {
            throw new IllegalArgumentException("没有找到商品信息");
        }
        Map<String, Object> result = new HashMap<>(row);
        result.put("image", normalizeAsset((String) row.get("image")));
        result.put("storeName", row.get("productName"));
        result.put("truePrice", row.get("price"));
        result.put("cartNum", row.get("payNum"));
        result.put("attrId", row.get("unique"));
        return result;
    }

    public Map<String, Object> expressOrder(Integer uid, String orderId) {
        Map<String, Object> order = commerceMapper.selectOrderExpress(uid, orderId);
        if (order == null) {
            throw new IllegalArgumentException("未找到该订单信息");
        }
        if (!"express".equals(String.valueOf(order.getOrDefault("deliveryType", "")))
                || !StringUtils.hasText(String.valueOf(order.getOrDefault("deliveryId", "")))) {
            throw new IllegalArgumentException("该订单不存在快递订单号");
        }
        Integer orderDbId = ((Number) order.get("id")).intValue();
        List<Map<String, Object>> infos = commerceMapper.selectOrderInfos(List.of(orderDbId)).stream()
                .map(info -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("payNum", info.get("payNum"));
                    item.put("price", info.get("price"));
                    item.put("productName", info.get("productName"));
                    item.put("productImg", normalizeAsset((String) info.get("image")));
                    return item;
                })
                .toList();
        Map<String, Object> orderInfo = new HashMap<>();
        orderInfo.put("deliveryId", order.get("deliveryId"));
        orderInfo.put("deliveryName", order.get("deliveryName"));
        orderInfo.put("deliveryType", order.get("deliveryType"));
        orderInfo.put("info", infos);

        List<Map<String, Object>> traces = new ArrayList<>();
        traces.add(Map.of(
                "status", "商家已发货，等待物流更新",
                "time", stringOrDefault(order.get("updateTime"), stringOrDefault(order.get("createTime"), ""))));
        traces.add(Map.of(
                "status", "订单已支付，商家正在处理",
                "time", stringOrDefault(order.get("payTime"), stringOrDefault(order.get("createTime"), ""))));

        Map<String, Object> response = new HashMap<>();
        response.put("order", orderInfo);
        response.put("express", Map.of(
                "nu", order.get("deliveryId"),
                "com", order.get("deliveryCode"),
                "state", "0",
                "list", traces));
        return response;
    }

    @Transactional
    public boolean refundApply(Integer uid, Map<String, Object> body) {
        String orderId = stringValue(body, "uni", "orderId", "orderNo");
        String reason = stringValue(body, "text", "reason");
        String explain = stringValue(body, "refund_reason_wap_explain", "explain");
        String reasonImage = clearUploadPrefix(stringValue(body, "refund_reason_wap_img", "reasonImage"));
        if (!StringUtils.hasText(orderId)) {
            throw new IllegalArgumentException("订单编号不能为空");
        }
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("退款原因必须填写");
        }
        if (reason.length() > 255) {
            throw new IllegalArgumentException("退款原因不能超过255个字符");
        }

        Map<String, Object> order = commerceMapper.selectPaidOrderForRefund(uid, orderId);
        if (order == null) {
            throw new IllegalArgumentException("支付订单不存在");
        }
        int refundStatus = numberValue(order.get("refundStatus"));
        if (refundStatus == 1) {
            throw new IllegalArgumentException("正在申请退款中");
        }
        if (refundStatus == 2) {
            throw new IllegalArgumentException("订单已退款");
        }
        if (refundStatus == 3) {
            throw new IllegalArgumentException("订单退款中");
        }
        if (commerceMapper.applyRefund(uid, orderId, reason, explain, reasonImage) <= 0) {
            throw new IllegalArgumentException("申请退款失败");
        }
        Integer orderDbId = ((Number) order.get("id")).intValue();
        String logReason = StringUtils.hasText(explain) ? explain : reason;
        commerceMapper.insertOrderStatus(orderDbId, "apply_refund", "用户申请退款原因：" + logReason);
        return true;
    }

    public List<String> refundReasons() {
        String value = commerceMapper.selectConfigValue("stor_reason");
        if (!StringUtils.hasText(value)) {
            return List.of("收货地址填错了", "与描述不符", "信息填错了，重新拍", "收到商品损坏了", "未按预定时间发货", "其它原因");
        }
        return value.replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace("rn", "\n")
                .lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private Integer normalizeRefundStatus(Integer status) {
        if (status == null || status <= 0 || status == -3) {
            return null;
        }
        if (status == 1 || status == 2 || status == 3 || status == 4) {
            return status;
        }
        throw new IllegalArgumentException("售后状态参数错误");
    }

    @Transactional
    public boolean cancelOrder(Integer uid, Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        Map<String, Object> order = commerceMapper.selectOrderById(uid, id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (numberValue(order.get("paid")) > 0) {
            throw new IllegalArgumentException("已支付订单请走退款/售后流程");
        }
        if (commerceMapper.cancelOrder(uid, id) <= 0) {
            return false;
        }
        redisTemplate.opsForList().leftPush(OrderCancelTask.ORDER_TASK_REDIS_KEY_AFTER_CANCEL_BY_USER, String.valueOf(id));
        return true;
    }

    @Transactional
    public boolean takeOrder(Integer uid, Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        Map<String, Object> order = commerceMapper.selectOrderById(uid, id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (numberValue(order.get("paid")) <= 0 || numberValue(order.get("status")) != 1 || numberValue(order.get("refundStatus")) > 0) {
            throw new IllegalArgumentException("订单状态错误");
        }
        if (commerceMapper.takeOrder(uid, id) <= 0) {
            throw new IllegalArgumentException("确认收货失败");
        }
        redisTemplate.opsForList().leftPush(OrderReceiptTask.ORDER_TASK_REDIS_KEY_AFTER_TAKE_BY_USER, String.valueOf(id));
        return true;
    }

    @Transactional
    public boolean commentOrder(Integer uid, Map<String, Object> body) {
        String orderNo = stringValue(body, "orderNo", "uni", "orderId");
        String unique = stringValue(body, "unique", "uni");
        String comment = stringValue(body, "comment");
        Integer productId = intValue(body, "productId", "product_id");
        Integer productScore = intValue(body, "productScore", "product_score");
        Integer serviceScore = intValue(body, "serviceScore", "service_score");
        if (!StringUtils.hasText(orderNo)) {
            throw new IllegalArgumentException("订单号参数不能为空");
        }
        if (!StringUtils.hasText(unique)) {
            throw new IllegalArgumentException("商品规格参数不能为空");
        }
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (!StringUtils.hasText(comment)) {
            throw new IllegalArgumentException("请填写你对宝贝的心得！");
        }
        if (comment.length() > 512) {
            throw new IllegalArgumentException("评价内容不能超过512字");
        }
        if (!validScore(productScore) || !validScore(serviceScore)) {
            throw new IllegalArgumentException("请选择评分");
        }
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        Map<String, Object> order = commerceMapper.selectOrderForReply(uid, orderNo);
        if (order == null) {
            throw new IllegalArgumentException("该订单不存在");
        }
        if (numberValue(order.get("paid")) <= 0 || numberValue(order.get("status")) != 2 || numberValue(order.get("refundStatus")) > 0) {
            throw new IllegalArgumentException("订单状态错误");
        }
        Integer orderDbId = ((Number) order.get("id")).intValue();
        Map<String, Object> orderInfo = commerceMapper.selectOrderInfoForReplyByOrderId(uid, orderDbId, unique);
        if (orderInfo == null || !productId.equals(numberValue(orderInfo.get("productId")))) {
            throw new IllegalArgumentException("没有找到商品信息");
        }
        if (numberValue(orderInfo.get("isReply")) > 0 || commerceMapper.countProductReply(orderDbId, productId, unique) > 0) {
            throw new IllegalArgumentException("该商品已评价");
        }

        Map<String, Object> reply = new HashMap<>();
        reply.put("uid", uid);
        reply.put("oid", orderDbId);
        reply.put("unique", unique);
        reply.put("productId", productId);
        reply.put("productScore", productScore);
        reply.put("serviceScore", serviceScore);
        reply.put("comment", comment);
        reply.put("pics", normalizePics(stringValue(body, "pics")));
        reply.put("nickname", stringOrDefault(user.getNickname(), stringOrDefault(user.getAccount(), "用户" + uid)));
        reply.put("avatar", clearUploadPrefix(stringOrDefault(user.getAvatar(), "")));
        reply.put("sku", stringOrDefault(orderInfo.get("sku"), stringValue(body, "sku")));
        commerceMapper.insertProductReply(reply);
        commerceMapper.updateOrderInfoReply(orderDbId, productId, unique);
        if (commerceMapper.countUnrepliedOrderInfo(orderDbId) <= 0) {
            commerceMapper.completeOrderAfterReply(uid, orderDbId);
            commerceMapper.insertOrderStatus(orderDbId, "check_order_over", "用户评价");
        }
        return true;
    }

    @Transactional
    public boolean deleteOrder(Integer uid, Integer queryId, Map<String, Object> body) {
        Integer id = queryId;
        String bodyId = stringValue(body, "id");
        String orderNo = stringValue(body, "uni", "orderNo", "orderId", "order_id");
        if (id == null && StringUtils.hasText(bodyId)) {
            if (bodyId.chars().allMatch(Character::isDigit)) {
                id = Integer.valueOf(bodyId);
            } else if (!StringUtils.hasText(orderNo)) {
                orderNo = bodyId;
            }
        }
        if (id == null && !StringUtils.hasText(orderNo)) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        Map<String, Object> order = id == null
                ? commerceMapper.selectOrderByOrderNo(uid, orderNo)
                : commerceMapper.selectOrderById(uid, id);
        if (order == null) {
            throw new IllegalArgumentException("没有找到相关订单信息");
        }
        if (numberValue(order.get("paid")) <= 0) {
            throw new IllegalArgumentException("未支付订单无法删除");
        }
        int refundStatus = numberValue(order.get("refundStatus"));
        if (refundStatus > 0 && refundStatus != 2) {
            throw new IllegalArgumentException("订单在退款流程中无法删除");
        }
        if (refundStatus == 0 && numberValue(order.get("status")) != 3) {
            throw new IllegalArgumentException("只能删除已完成订单");
        }
        Integer orderDbId = ((Number) order.get("id")).intValue();
        if (commerceMapper.deleteUserOrder(uid, orderDbId) <= 0) {
            throw new IllegalArgumentException("订单已删除");
        }
        commerceMapper.insertOrderStatus(orderDbId, "remove_order", "删除订单");
        return true;
    }

    public Map<String, Integer> orderData(Integer uid) {
        Map<String, Integer> data = new HashMap<>();
        data.put("unPaidCount", commerceMapper.countUnpaid(uid));
        data.put("unShippedCount", commerceMapper.countUnShipped(uid));
        data.put("receivedCount", commerceMapper.countReceived(uid));
        data.put("evaluatedCount", commerceMapper.countEvaluated(uid));
        data.put("refundCount", commerceMapper.countRefund(uid));
        return data;
    }

    private void attachOrderInfos(List<Map<String, Object>> orders) {
        if (orders.isEmpty()) {
            return;
        }
        List<Integer> ids = orders.stream().map(order -> ((Number) order.get("id")).intValue()).toList();
        Map<Integer, List<Map<String, Object>>> infosByOrderId = new HashMap<>();
        for (Map<String, Object> info : commerceMapper.selectOrderInfos(ids)) {
            Integer orderId = ((Number) info.get("orderId")).intValue();
            info.put("image", normalizeAsset((String) info.get("image")));
            info.put("productImg", info.get("image"));
            info.put("storeName", info.get("productName"));
            info.put("cartNum", info.get("payNum"));
            infosByOrderId.computeIfAbsent(orderId, key -> new ArrayList<>()).add(info);
        }
        for (Map<String, Object> order : orders) {
            Integer id = ((Number) order.get("id")).intValue();
            List<Map<String, Object>> infoList = infosByOrderId.getOrDefault(id, List.of());
            order.put("cartInfo", infoList);
            order.put("orderInfoList", infoList);
            order.put("statusText", orderStatusText(order));
            order.put("refundReasonWapImg", normalizeJoinedAssets(stringOrDefault(order.get("refundReasonWapImg"), "")));
            order.put("refundStatusText", refundStatusText(numberValue(order.get("refundStatus"))));
            if (numberValue(order.get("shippingType")) == 2 && numberValue(order.get("storeId")) > 0) {
                Map<String, Object> store = commerceMapper.selectStoreById(numberValue(order.get("storeId")));
                if (store != null) {
                    order.put("systemStore", normalizeStore(store));
                }
            }
        }
    }

    private Map<String, Object> normalizeCart(Map<String, Object> row) {
        Map<String, Object> item = new HashMap<>(row);
        String image = (String) firstPresent(row.get("skuImage"), row.get("image"));
        BigDecimal price = (BigDecimal) firstPresent(row.get("skuPrice"), row.get("price"));
        item.put("image", normalizeAsset(image));
        item.put("truePrice", price);
        item.put("price", price);
        item.put("stock", firstPresent(row.get("skuStock"), row.get("productStock")));
        item.put("attrInfo", Map.of(
                "id", firstPresent(row.get("attrValueId"), row.get("productAttrUnique")),
                "suk", row.getOrDefault("suk", ""),
                "price", price,
                "image", normalizeAsset(image),
                "stock", firstPresent(row.get("skuStock"), row.get("productStock"))));
        return item;
    }

    private Map<String, Object> preOrderResponse(Integer uid, String preOrderNo, List<Map<String, Object>> rows) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> priceGroup = priceGroup(uid, rows, false, null);
        Map<String, Object> orderInfoVo = new HashMap<>();
        orderInfoVo.put("cartInfo", rows.stream().map(this::normalizeCart).toList());
        orderInfoVo.put("priceGroup", priceGroup);
        orderInfoVo.put("totalNum", rows.stream().mapToInt(row -> numberValue(row.get("cartNum"))).sum());

        response.put("preOrderNo", preOrderNo);
        response.put("orderInfoVo", orderInfoVo);
        response.put("cartInfo", orderInfoVo.get("cartInfo"));
        response.put("priceGroup", priceGroup);
        response.put("addressInfo", defaultAddress(uid));
        response.put("storeSelfMention", selfMentionEnabled() ? "1" : "0");
        response.put("yuePayStatus", "1");
        response.put("payWeixinOpen", "0");
        response.put("aliPayStatus", "0");
        return response;
    }

    private Map<String, Object> priceGroup(Integer uid, List<Map<String, Object>> rows, boolean useIntegral, Integer couponId) {
        BigDecimal proTotal = productTotal(rows);
        BigDecimal couponFee = couponDiscount(uid, rows, couponId, proTotal);
        BigDecimal payFee = proTotal.subtract(couponFee);
        if (payFee.compareTo(BigDecimal.ZERO) < 0) {
            payFee = BigDecimal.ZERO.setScale(2);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("couponFee", couponFee);
        response.put("couponId", couponFee.compareTo(BigDecimal.ZERO) > 0 ? couponId : 0);
        response.put("deductionPrice", BigDecimal.ZERO.setScale(2));
        response.put("freightFee", BigDecimal.ZERO.setScale(2));
        response.put("payFee", payFee);
        response.put("proTotalFee", proTotal);
        response.put("surplusIntegral", 0);
        response.put("useIntegral", useIntegral);
        response.put("usedIntegral", 0);
        return response;
    }

    private BigDecimal productTotal(List<Map<String, Object>> rows) {
        return money(rows.stream()
                .map(row -> money(firstPresent(row.get("skuPrice"), row.get("price"))).multiply(BigDecimal.valueOf(numberValue(row.get("cartNum")))))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal couponDiscount(Integer uid, List<Map<String, Object>> rows, Integer couponId, BigDecimal proTotal) {
        if (couponId == null || couponId <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        Map<String, Object> coupon = commerceMapper.selectCouponUser(uid, couponId);
        if (coupon == null) {
            throw new IllegalArgumentException("优惠券领取记录不存在！");
        }
        if (numberValue(coupon.get("status")) == 1) {
            throw new IllegalArgumentException("此优惠券已使用！");
        }
        if (numberValue(coupon.get("status")) == 2) {
            throw new IllegalArgumentException("此优惠券已失效！");
        }
        if (money(coupon.get("minPrice")).compareTo(proTotal) > 0) {
            throw new IllegalArgumentException("总金额小于优惠券最小使用金额");
        }
        int useType = numberValue(coupon.get("useType"));
        BigDecimal couponMoney = money(coupon.get("money"));
        if (useType == 1) {
            return couponMoney.min(proTotal);
        }
        Set<Integer> primaryIds = idSet(stringOrDefault(coupon.get("primaryKey"), ""));
        if (primaryIds.isEmpty()) {
            throw new IllegalArgumentException("优惠券适用范围无效");
        }
        BigDecimal allowedTotal = BigDecimal.ZERO.setScale(2);
        if (useType == 2) {
            for (Map<String, Object> row : rows) {
                if (primaryIds.contains(numberValue(row.get("productId")))) {
                    allowedTotal = allowedTotal.add(money(firstPresent(row.get("skuPrice"), row.get("price")))
                            .multiply(BigDecimal.valueOf(numberValue(row.get("cartNum")))));
                }
            }
            if (allowedTotal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("此优惠券为商品券，请购买相关商品之后再使用！");
            }
            return couponMoney.min(money(allowedTotal));
        }
        if (useType == 3) {
            for (Map<String, Object> row : rows) {
                Set<Integer> rowCategories = idSet(stringOrDefault(row.get("cateId"), ""));
                rowCategories.retainAll(primaryIds);
                if (!rowCategories.isEmpty()) {
                    allowedTotal = allowedTotal.add(money(firstPresent(row.get("skuPrice"), row.get("price")))
                            .multiply(BigDecimal.valueOf(numberValue(row.get("cartNum")))));
                }
            }
            if (allowedTotal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("此优惠券为分类券，请购买相关分类下的商品之后再使用！");
            }
            return couponMoney.min(money(allowedTotal));
        }
        throw new IllegalArgumentException("优惠券适用范围无效");
    }

    private BigDecimal totalCost(List<Map<String, Object>> rows) {
        BigDecimal cost = rows.stream()
                .map(row -> money(row.get("cost")).multiply(BigDecimal.valueOf(numberValue(row.get("cartNum")))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return money(cost);
    }

    private List<Map<String, Object>> checkedCartRows(Integer uid, List<Integer> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要购买的商品");
        }
        List<Map<String, Object>> rows = commerceMapper.selectCartRowsByIds(uid, cartIds).stream().map(this::normalizeCart).toList();
        if (rows.size() != cartIds.stream().distinct().count()) {
            throw new IllegalArgumentException("购物车商品已失效，请刷新后重试");
        }
        for (Map<String, Object> row : rows) {
            if (numberValue(row.get("stock")) < numberValue(row.get("cartNum"))) {
                throw new IllegalArgumentException("商品库存不足：" + row.get("storeName"));
            }
        }
        return rows;
    }

    private StoreCouponUserResponse toUserCouponResponse(StoreCouponUser coupon) {
        StoreCouponUserResponse response = new StoreCouponUserResponse();
        response.setId(coupon.getId());
        response.setCouponId(coupon.getCouponId());
        response.setCid(coupon.getCid());
        response.setUid(coupon.getUid());
        response.setName(coupon.getName());
        response.setMoney(coupon.getMoney());
        response.setMinPrice(coupon.getMinPrice());
        response.setType(coupon.getType());
        response.setStatus(coupon.getStatus());
        response.setCreateTime(coupon.getCreateTime());
        response.setUpdateTime(coupon.getUpdateTime());
        response.setStartTime(coupon.getStartTime());
        response.setEndTime(coupon.getEndTime());
        response.setUseTime(coupon.getUseTime());
        response.setUseType(coupon.getUseType());
        response.setPrimaryKey(coupon.getPrimaryKey());
        response.setValidStr(couponValidStr(coupon));
        response.setIsValid("usable".equals(response.getValidStr()));
        response.setUseStartTimeStr(formatCouponDate(coupon.getStartTime()));
        response.setUseEndTimeStr(formatCouponDate(coupon.getEndTime()));
        return response;
    }

    private Set<Integer> receivedCouponIds(Integer uid, List<StoreCoupon> coupons) {
        if (coupons.isEmpty()) {
            return Set.of();
        }
        List<Integer> ids = coupons.stream().map(StoreCoupon::getId).toList();
        QueryWrapper<StoreCouponUser> query = new QueryWrapper<>();
        query.select("coupon_id");
        query.eq("uid", uid);
        query.in("coupon_id", ids);
        return couponUserMapper.selectList(query).stream()
                .map(StoreCouponUser::getCouponId)
                .collect(Collectors.toSet());
    }

    private Map<String, Object> toReceiveCouponResponse(StoreCoupon coupon, boolean received) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", coupon.getId());
        item.put("couponId", coupon.getId());
        item.put("name", coupon.getName());
        item.put("money", coupon.getMoney());
        item.put("minPrice", coupon.getMinPrice());
        item.put("useType", coupon.getUseType());
        item.put("type", coupon.getType());
        item.put("day", coupon.getDay() == null ? 0 : coupon.getDay());
        item.put("isUse", received);
        item.put("use_title", received ? "已领取" : "");
        item.put("lastTotal", coupon.getLastTotal());
        item.put("isLimited", coupon.getIsLimited());
        item.put("useStartTimeStr", formatCouponDate(coupon.getUseStartTime()));
        item.put("useEndTimeStr", formatCouponDate(coupon.getUseEndTime()));
        item.put("receiveStartTimeStr", formatCouponDate(coupon.getReceiveStartTime()));
        item.put("receiveEndTimeStr", formatCouponDate(coupon.getReceiveEndTime()));
        return item;
    }

    private void validateReceiveCoupon(StoreCoupon coupon) {
        if (coupon == null || Boolean.TRUE.equals(coupon.getIsDel()) || !Boolean.TRUE.equals(coupon.getStatus())) {
            throw new IllegalArgumentException("优惠券信息不存在或者已失效！");
        }
        if (!Integer.valueOf(1).equals(coupon.getType())) {
            throw new IllegalArgumentException("此优惠券不能主动领取！");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getReceiveStartTime() != null && coupon.getReceiveStartTime().isAfter(now)) {
            throw new IllegalArgumentException("优惠券暂未开始领取！");
        }
        if (coupon.getReceiveEndTime() != null && !coupon.getReceiveEndTime().isAfter(now)) {
            throw new IllegalArgumentException("已超过优惠券领取最后期限！");
        }
        if (Boolean.TRUE.equals(coupon.getIsLimited()) && (coupon.getLastTotal() == null || coupon.getLastTotal() < 1)) {
            throw new IllegalArgumentException("此优惠券已经被领完了！");
        }
    }

    private String couponValidStr(StoreCouponUser coupon) {
        if (Integer.valueOf(1).equals(coupon.getStatus())) {
            return "unusable";
        }
        if (Integer.valueOf(2).equals(coupon.getStatus())) {
            return "overdue";
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() != null && coupon.getStartTime().isAfter(now)) {
            return "notStart";
        }
        if (coupon.getEndTime() != null && !coupon.getEndTime().isAfter(now)) {
            return "overdue";
        }
        return "usable";
    }

    private String formatCouponDate(LocalDateTime value) {
        return value == null ? "" : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private List<Map<String, Object>> checkedPreOrderRows(Integer uid, PreOrderSource source) {
        if (source.isAgain()) {
            return checkedAgainRows(uid, source.value());
        }
        if (source.isBuyNowSeckill()) {
            return checkedSeckillRows(uid, BuyNowDetail.decode(source.value()));
        }
        if (source.isBuyNowBargain()) {
            return checkedBargainRows(uid, BuyNowDetail.decode(source.value()));
        }
        if (source.isBuyNowCombination()) {
            return checkedCombinationRows(uid, BuyNowDetail.decode(source.value()));
        }
        if (source.isBuyNowProduct()) {
            return checkedBuyNowProductRows(BuyNowDetail.decode(source.value()));
        }
        return checkedCartRows(uid, source.cartIds());
    }

    private List<Map<String, Object>> checkedBuyNowProductRows(BuyNowDetail detail) {
        if (detail.productId() == null || detail.productId() <= 0) {
            throw new IllegalArgumentException("商品编号不能为空");
        }
        if (detail.attrValueId() == null || detail.attrValueId() <= 0) {
            throw new IllegalArgumentException("商品规格属性值不能为空");
        }
        int productNum = detail.productNum() == null || detail.productNum() <= 0 ? 1 : detail.productNum();
        Map<String, Object> row = commerceMapper.selectBuyNowProductRow(detail.productId(), detail.attrValueId());
        if (row == null) {
            throw new IllegalArgumentException("商品规格信息不存在，请刷新后重新选择");
        }
        if (numberValue(row.get("isShow")) != 1
                || numberValue(row.get("isDel")) > 0
                || numberValue(row.get("isRecycle")) > 0) {
            throw new IllegalArgumentException("商品已下架，请刷新后重新选择");
        }
        if (numberValue(row.get("productStock")) < productNum) {
            throw new IllegalArgumentException("商品库存不足，请刷新后重新选择");
        }
        if (numberValue(row.get("skuStock")) < productNum) {
            throw new IllegalArgumentException("商品规格库存不足，请刷新后重新选择");
        }
        row.put("cartNum", productNum);
        row.put("productType", 0);
        row.put("stock", firstPresent(row.get("skuStock"), row.get("productStock")));
        return List.of(normalizeCart(row));
    }

    private List<Map<String, Object>> checkedSeckillRows(Integer uid, BuyNowDetail detail) {
        int productNum = detail.productNum() == null || detail.productNum() <= 0 ? 1 : detail.productNum();
        Map<String, Object> row = commerceMapper.selectSeckillOrderRow(
                detail.seckillId(),
                detail.attrValueId(),
                detail.productId());
        if (row == null) {
            throw new IllegalArgumentException("秒杀商品规格不存在");
        }
        validateSeckillRow(uid, productNum, row);
        row.put("cartNum", productNum);
        row.put("productType", PRODUCT_TYPE_SECKILL);
        row.put("price", money(row.get("activitySkuPrice")));
        row.put("skuPrice", money(row.get("activitySkuPrice")));
        row.put("truePrice", money(row.get("activitySkuPrice")));
        row.put("stock", firstPresent(row.get("skuStock"), row.get("activityStock")));
        row.put("weight", firstPresent(row.get("skuWeight"), row.get("weight")));
        row.put("volume", firstPresent(row.get("skuVolume"), row.get("volume")));
        return List.of(normalizeCart(row));
    }

    private List<Map<String, Object>> checkedBargainRows(Integer uid, BuyNowDetail detail) {
        if (detail.bargainUserId() == null || detail.bargainUserId() <= 0) {
            throw new IllegalArgumentException("用户砍价活动id必须大于0");
        }
        int productNum = detail.productNum() == null || detail.productNum() <= 0 ? 1 : detail.productNum();
        Map<String, Object> row = commerceMapper.selectBargainOrderRow(
                detail.bargainId(),
                detail.attrValueId(),
                detail.productId());
        if (row == null) {
            throw new IllegalArgumentException("砍价商品规格不存在");
        }
        validateBargainRow(uid, detail.bargainUserId(), productNum, row);
        row.put("cartNum", productNum);
        row.put("productType", PRODUCT_TYPE_BARGAIN);
        row.put("bargainUserId", detail.bargainUserId());
        row.put("price", money(row.get("bargainMinPrice")));
        row.put("skuPrice", money(row.get("bargainMinPrice")));
        row.put("truePrice", money(row.get("bargainMinPrice")));
        row.put("stock", firstPresent(row.get("skuStock"), row.get("bargainStock")));
        row.put("weight", firstPresent(row.get("skuWeight"), row.get("weight")));
        row.put("volume", firstPresent(row.get("skuVolume"), row.get("volume")));
        return List.of(normalizeCart(row));
    }

    private void validateBargainRow(Integer uid, Integer bargainUserId, int productNum, Map<String, Object> row) {
        Integer bargainId = numberValue(row.get("bargainId"));
        if (numberValue(row.get("bargainStatus")) != 1 || numberValue(row.get("bargainIsDel")) > 0) {
            throw new IllegalArgumentException("砍价商品已下架");
        }
        long now = Instant.now().toEpochMilli();
        if (now < longValue(row.get("startTime"))) {
            throw new IllegalArgumentException("砍价活动未开始");
        }
        if (now > longValue(row.get("stopTime"))) {
            throw new IllegalArgumentException("砍价活动已结束");
        }
        if (numberValue(row.get("bargainStock")) < productNum || numberValue(row.get("bargainQuota")) < productNum) {
            throw new IllegalArgumentException("砍价商品库存不足");
        }
        if (numberValue(row.get("skuStock")) < productNum || numberValue(row.get("skuQuota")) < productNum) {
            throw new IllegalArgumentException("砍价商品规格库存不足");
        }
        if (numberValue(row.get("isShow")) != 1 || numberValue(row.get("isDel")) > 0 || numberValue(row.get("isRecycle")) > 0) {
            throw new IllegalArgumentException("砍价主商品不存在或已下架");
        }
        if (numberValue(row.get("productStock")) < productNum) {
            throw new IllegalArgumentException("砍价主商品库存不足");
        }
        if (row.get("normalAttrValueId") == null || numberValue(row.get("normalSkuStock")) < productNum) {
            throw new IllegalArgumentException("砍价主商品规格库存不足");
        }

        Map<String, Object> bargainUser = commerceMapper.selectBargainUserForOrder(bargainUserId);
        if (bargainUser == null || !bargainId.equals(numberValue(bargainUser.get("bargainId")))) {
            throw new IllegalArgumentException("用户砍价活动不存在");
        }
        if (!uid.equals(numberValue(bargainUser.get("uid")))) {
            throw new IllegalArgumentException("只能购买自己的砍价活动");
        }
        if (numberValue(bargainUser.get("isDel")) > 0) {
            throw new IllegalArgumentException("用户砍价活动已取消");
        }
        BigDecimal minPrice = money(bargainUser.get("bargainPriceMin"));
        BigDecimal bargainPrice = money(bargainUser.get("bargainPrice"));
        BigDecimal cutPrice = money(bargainUser.get("price"));
        if (numberValue(bargainUser.get("status")) != 3
                && minPrice.compareTo(bargainPrice.subtract(cutPrice).setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalArgumentException("请先完成砍价活动");
        }

        Map<String, Object> bargainOrder = commerceMapper.selectBargainOrder(bargainId, bargainUserId);
        if (bargainOrder != null) {
            if (numberValue(bargainOrder.get("paid")) <= 0) {
                throw new IllegalArgumentException("订单已创建，尚未支付");
            }
            throw new IllegalArgumentException("该砍价活动已创建了订单");
        }
        Map<String, Object> currentOrders = commerceMapper.countUserCurrentBargainOrders(uid, bargainId);
        if (numberValue(currentOrders.get("unpaidCount")) > 0) {
            throw new IllegalArgumentException("您有砍价待支付订单，请支付后再购买");
        }
        if (numberValue(currentOrders.get("totalCount")) >= numberValue(row.get("activityLimit"))) {
            throw new IllegalArgumentException("您已经达到当前砍价活动上限");
        }
    }

    private List<Map<String, Object>> checkedCombinationRows(Integer uid, BuyNowDetail detail) {
        int productNum = detail.productNum() == null || detail.productNum() <= 0 ? 1 : detail.productNum();
        Map<String, Object> row = commerceMapper.selectCombinationOrderRow(
                detail.combinationId(),
                detail.attrValueId(),
                detail.productId());
        if (row == null) {
            throw new IllegalArgumentException("拼团商品规格不存在");
        }
        validateCombinationRow(uid, detail.pinkId(), productNum, row);
        row.put("cartNum", productNum);
        row.put("productType", PRODUCT_TYPE_COMBINATION);
        row.put("pinkId", detail.pinkId() == null ? 0 : detail.pinkId());
        row.put("price", money(row.get("activitySkuPrice")));
        row.put("skuPrice", money(row.get("activitySkuPrice")));
        row.put("truePrice", money(row.get("activitySkuPrice")));
        row.put("stock", firstPresent(row.get("skuStock"), row.get("activityStock")));
        row.put("weight", firstPresent(row.get("skuWeight"), row.get("weight")));
        row.put("volume", firstPresent(row.get("skuVolume"), row.get("volume")));
        return List.of(normalizeCart(row));
    }

    private void validateSeckillRow(Integer uid, int productNum, Map<String, Object> row) {
        if (numberValue(row.get("activityStatus")) != 1
                || numberValue(row.get("activityIsShow")) != 1
                || numberValue(row.get("activityIsDel")) > 0) {
            throw new IllegalArgumentException("秒杀商品已关闭");
        }
        if (numberValue(row.get("activityStock")) < productNum || numberValue(row.get("activityQuota")) < productNum) {
            throw new IllegalArgumentException("秒杀商品库存不足");
        }
        validateActivityBase("秒杀", productNum, row);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = localDateTime(row.get("startTime"));
        LocalDateTime stopTime = localDateTime(row.get("stopTime"));
        if (startTime == null || now.isBefore(startTime)) {
            throw new IllegalArgumentException("秒杀活动未开始");
        }
        if (stopTime == null || !now.isBefore(stopTime)) {
            throw new IllegalArgumentException("秒杀商品已过期");
        }
        int startHour = numberValue(row.get("managerStartHour"));
        int endHour = numberValue(row.get("managerEndHour"));
        int currentHour = now.getHour();
        if (currentHour < startHour || currentHour >= endHour) {
            throw new IllegalArgumentException("秒杀商品已过期");
        }
        Map<String, Object> currentOrders = commerceMapper.countUserCurrentDaySeckillOrders(uid, numberValue(row.get("seckillId")));
        if (numberValue(currentOrders.get("unpaidCount")) > 0) {
            throw new IllegalArgumentException("您有秒杀待支付订单，请支付后再购买");
        }
        if (numberValue(currentOrders.get("totalCount")) >= numberValue(row.get("activityLimit"))) {
            throw new IllegalArgumentException("您已经达到当前秒杀活动上限");
        }
    }

    private void validateCombinationRow(Integer uid, Integer pinkId, int productNum, Map<String, Object> row) {
        if (numberValue(row.get("activityIsShow")) != 1 || numberValue(row.get("activityIsDel")) > 0) {
            throw new IllegalArgumentException("拼团商品已关闭");
        }
        long now = Instant.now().toEpochMilli();
        if (now < longValue(row.get("startTime"))) {
            throw new IllegalArgumentException("拼团商品活动未开始");
        }
        if (now >= longValue(row.get("stopTime"))) {
            throw new IllegalArgumentException("拼团商品已过期");
        }
        if (productNum > numberValue(row.get("onceNum"))) {
            throw new IllegalArgumentException("购买数量超过单次拼团购买上限");
        }
        if (numberValue(row.get("activityStock")) < productNum || numberValue(row.get("activityQuota")) < productNum) {
            throw new IllegalArgumentException("拼团商品库存不足");
        }
        validateActivityBase("拼团", productNum, row);
        if (pinkId != null && pinkId > 0) {
            Map<String, Object> pink = commerceMapper.selectPinkForJoin(pinkId);
            if (pink == null
                    || numberValue(pink.get("cid")) != numberValue(row.get("combinationId"))
                    || numberValue(pink.get("status")) != 1
                    || numberValue(pink.get("isRefund")) > 0
                    || numberValue(pink.get("kId")) != 0
                    || longValue(pink.get("stopTime")) <= now) {
                throw new IllegalArgumentException("拼团状态已失效");
            }
        }
        Map<String, Object> currentOrders = commerceMapper.countUserCurrentCombinationOrders(uid, numberValue(row.get("combinationId")));
        if (numberValue(currentOrders.get("unpaidCount")) > 0) {
            throw new IllegalArgumentException("您有拼团待支付订单，请支付后再购买");
        }
        int boughtNum = numberValue(currentOrders.get("totalNum"));
        int limit = numberValue(row.get("activityLimit"));
        if (boughtNum >= limit) {
            throw new IllegalArgumentException("您已达到该商品拼团活动上限");
        }
        if (boughtNum + productNum > limit) {
            throw new IllegalArgumentException("超过该商品拼团活动您的购买上限");
        }
    }

    private void validateActivityBase(String name, int productNum, Map<String, Object> row) {
        if (numberValue(row.get("skuStock")) < productNum || numberValue(row.get("skuQuota")) < productNum) {
            throw new IllegalArgumentException(name + "商品规格库存不足");
        }
        if (numberValue(row.get("isShow")) != 1 || numberValue(row.get("isDel")) > 0 || numberValue(row.get("isRecycle")) > 0) {
            throw new IllegalArgumentException(name + "主商品不存在或已下架");
        }
        if (numberValue(row.get("productStock")) < productNum) {
            throw new IllegalArgumentException(name + "主商品库存不足");
        }
        if (row.get("normalAttrValueId") == null || numberValue(row.get("normalSkuStock")) < productNum) {
            throw new IllegalArgumentException(name + "主商品规格库存不足");
        }
    }

    private void lockActivityStockIfNeeded(List<Map<String, Object>> rows) {
        if (rows.size() != 1) {
            return;
        }
        Map<String, Object> row = rows.get(0);
        int productType = numberValue(row.get("productType"));
        if (productType != PRODUCT_TYPE_SECKILL
                && productType != PRODUCT_TYPE_BARGAIN
                && productType != PRODUCT_TYPE_COMBINATION) {
            return;
        }
        int num = numberValue(row.get("cartNum"));
        Integer activityId = activityId(row);
        Integer activityAttrValueId = numberValue(row.get("attrValueId"));
        Integer productId = numberValue(row.get("productId"));
        Integer normalAttrValueId = numberValue(row.get("normalAttrValueId"));
        if (decreaseActivityStock(activityId, productType, num) <= 0
                || commerceMapper.decreaseActivityAttrStock(activityId, activityAttrValueId, productType, num) <= 0
                || commerceMapper.decreaseProductStock(productId, num) <= 0
                || commerceMapper.decreaseNormalAttrStock(normalAttrValueId, num) <= 0) {
            throw new IllegalArgumentException("活动商品库存不足，请刷新后重新选择");
        }
    }

    private Map<String, Object> activityParams(List<Map<String, Object>> rows) {
        Map<String, Object> params = new HashMap<>();
        params.put("seckillId", 0);
        params.put("bargainId", 0);
        params.put("combinationId", 0);
        params.put("pinkId", 0);
        params.put("bargainUserId", 0);
        if (rows.size() != 1) {
            return params;
        }
        Map<String, Object> row = rows.get(0);
        int productType = numberValue(row.get("productType"));
        if (productType == PRODUCT_TYPE_SECKILL) {
            params.put("seckillId", numberValue(row.get("seckillId")));
        } else if (productType == PRODUCT_TYPE_BARGAIN) {
            params.put("bargainId", numberValue(row.get("bargainId")));
            params.put("bargainUserId", numberValue(row.get("bargainUserId")));
        } else if (productType == PRODUCT_TYPE_COMBINATION) {
            params.put("combinationId", numberValue(row.get("combinationId")));
            params.put("pinkId", numberValue(row.get("pinkId")));
        }
        return params;
    }

    private Integer activityId(Map<String, Object> row) {
        int productType = numberValue(row.get("productType"));
        if (productType == PRODUCT_TYPE_SECKILL) {
            return numberValue(row.get("seckillId"));
        }
        if (productType == PRODUCT_TYPE_COMBINATION) {
            return numberValue(row.get("combinationId"));
        }
        return numberValue(row.get("bargainId"));
    }

    private int decreaseActivityStock(Integer activityId, int productType, int num) {
        if (productType == PRODUCT_TYPE_SECKILL) {
            return commerceMapper.decreaseSeckillStock(activityId, num);
        }
        if (productType == PRODUCT_TYPE_COMBINATION) {
            return commerceMapper.decreaseCombinationStock(activityId, num);
        }
        return commerceMapper.decreaseBargainStock(activityId, num);
    }

    private List<Map<String, Object>> checkedAgainRows(Integer uid, String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new IllegalArgumentException("再次购买订单编号不能为空");
        }
        Map<String, Object> order = commerceMapper.selectOrderForAgain(uid, orderNo);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (numberValue(order.get("refundStatus")) > 0 || numberValue(order.get("status")) != 3) {
            throw new IllegalArgumentException("只有已完成状态订单才能再次购买");
        }
        if (numberValue(order.get("seckillId")) > 0
                || numberValue(order.get("bargainId")) > 0
                || numberValue(order.get("combinationId")) > 0) {
            throw new IllegalArgumentException("活动商品订单不能再次购买");
        }
        if (numberValue(order.get("type")) == 1) {
            throw new IllegalArgumentException("视频订单不能再次购买");
        }
        Integer orderDbId = ((Number) order.get("id")).intValue();
        List<Map<String, Object>> rows = commerceMapper.selectAgainOrderRows(uid, orderDbId).stream().map(this::normalizeCart).toList();
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("订单详情未找到");
        }
        if (rows.size() != commerceMapper.countOrderInfoRows(orderDbId)) {
            throw new IllegalArgumentException("订单商品已失效，请刷新后重新选择");
        }
        for (Map<String, Object> row : rows) {
            if (numberValue(row.get("productStock")) < numberValue(row.get("cartNum"))) {
                throw new IllegalArgumentException("商品库存不足，请刷新后重新选择");
            }
            if (numberValue(row.get("skuStock")) < numberValue(row.get("cartNum"))) {
                throw new IllegalArgumentException("商品规格库存不足，请刷新后重新选择");
            }
        }
        return rows;
    }

    private List<Integer> categoryIds(List<Map<String, Object>> rows) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            ids.addAll(idSet(stringOrDefault(row.get("cateId"), "")));
        }
        return new ArrayList<>(ids);
    }

    private Set<Integer> idSet(String value) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (!StringUtils.hasText(value)) {
            return ids;
        }
        for (String item : value.split("[,/]+")) {
            if (StringUtils.hasText(item)) {
                try {
                    ids.add(Integer.valueOf(item.trim()));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed legacy category fragments.
                }
            }
        }
        return ids;
    }

    private List<Map<String, Object>> orderInfoRows(Integer orderDbId, String orderNo, List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> info = new HashMap<>();
            info.put("orderDbId", orderDbId);
            info.put("productId", row.get("productId"));
            info.put("info", orderInfoJson(row));
            info.put("unique", UUID.randomUUID().toString().replace("-", ""));
            info.put("orderNo", orderNo);
            info.put("productName", truncate(String.valueOf(row.get("storeName")), 128));
            info.put("attrValueId", firstPresent(row.get("attrValueId"), row.get("productAttrUnique")));
            info.put("image", stringOrDefault(firstPresent(row.get("skuImage"), row.get("image")), ""));
            info.put("sku", truncate(stringOrDefault(row.get("suk"), "默认规格"), 128));
            info.put("price", money(row.get("price")));
            info.put("payNum", numberValue(row.get("cartNum")));
            info.put("weight", money(row.get("weight")));
            info.put("volume", money(row.get("volume")));
            info.put("giveIntegral", numberValue(row.get("giveIntegral")));
            info.put("isSub", numberValue(row.get("isSub")));
            info.put("vipPrice", money(row.get("vipPrice")));
            info.put("productType", numberValue(row.get("productType")));
            result.add(info);
        }
        return result;
    }

    private String orderInfoJson(Map<String, Object> row) {
        return "{"
                + "\"productId\":" + row.get("productId") + ","
                + "\"storeName\":\"" + jsonEscape(String.valueOf(row.get("storeName"))) + "\","
                + "\"suk\":\"" + jsonEscape(stringOrDefault(row.get("suk"), "")) + "\","
                + "\"price\":\"" + money(row.get("price")) + "\","
                + "\"cartNum\":" + numberValue(row.get("cartNum"))
                + "}";
    }

    private Map<String, Object> normalizeAddress(Map<String, Object> address) {
        Map<String, Object> result = new HashMap<>(address);
        result.put("isDefault", Boolean.TRUE.equals(address.get("isDefault"))
                || Integer.valueOf(1).equals(address.get("isDefault")));
        result.put("address", Map.of(
                "province", stringOrDefault(address.get("province"), ""),
                "city", stringOrDefault(address.get("city"), ""),
                "district", stringOrDefault(address.get("district"), ""),
                "cityId", address.getOrDefault("cityId", 0)));
        result.put("fullAddress", fullAddress(address));
        return result;
    }

    private Map<String, Object> addressParams(Integer uid, Map<String, Object> body) {
        String province = stringValue(body, "province");
        String city = stringValue(body, "city");
        String district = stringValue(body, "district");
        Object addressObject = body == null ? null : body.get("address");
        if (addressObject instanceof Map<?, ?> address) {
            province = firstText(province, address.get("province"));
            city = firstText(city, address.get("city"));
            district = firstText(district, address.get("district"));
        }
        Map<String, Object> params = new HashMap<>();
        params.put("uid", uid);
        params.put("realName", stringValue(body, "realName", "real_name"));
        params.put("phone", stringValue(body, "phone"));
        params.put("province", province);
        params.put("city", city);
        params.put("district", district);
        params.put("detail", stringValue(body, "detail"));
        params.put("isDefault", Boolean.TRUE.equals(body == null ? null : body.get("isDefault")) ? 1 : 0);
        if (!StringUtils.hasText((String) params.get("realName"))
                || !StringUtils.hasText((String) params.get("phone"))
                || !StringUtils.hasText((String) params.get("detail"))) {
            throw new IllegalArgumentException("请填写完整收货地址");
        }
        return params;
    }

    private Map<String, Object> pickupAddress(Map<String, Object> body, Map<String, Object> store) {
        String realName = stringValue(body, "realName", "contacts");
        String phone = stringValue(body, "phone", "contactsTel");
        if (!StringUtils.hasText(realName) || !StringUtils.hasText(phone)) {
            throw new IllegalArgumentException("请填写姓名和电话");
        }
        Map<String, Object> address = new HashMap<>();
        address.put("realName", realName);
        address.put("phone", phone);
        address.put("province", "");
        address.put("city", "");
        address.put("district", "");
        address.put("detail", stringOrDefault(store.get("name"), "到店自提"));
        return address;
    }

    private List<Integer> cartIdsFromBody(Map<String, Object> body) {
        if (body == null) {
            return new ArrayList<>();
        }
        Object value = firstPresent(body.get("cartIds"), body.get("cartId"));
        if (value == null) {
            value = body.get("ids");
        }
        List<Integer> ids = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                ids.add(Integer.valueOf(String.valueOf(item)));
            }
        } else if (value != null) {
            for (String item : String.valueOf(value).split(",")) {
                if (StringUtils.hasText(item)) {
                    ids.add(Integer.valueOf(item.trim()));
                }
            }
        }
        return ids.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
    }

    private String encodePreOrderNo(Integer uid, String type, String value) {
        String payload = uid + "|" + type + "|" + value + "|" + Instant.now().toEpochMilli();
        return "pre" + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private PreOrderSource decodePreOrderNo(Integer uid, String preOrderNo) {
        if (!StringUtils.hasText(preOrderNo) || !preOrderNo.startsWith("pre")) {
            throw new IllegalArgumentException("预下单订单号无效");
        }
        String payload = new String(Base64.getUrlDecoder().decode(preOrderNo.substring(3)), StandardCharsets.UTF_8);
        String[] parts = payload.split("\\|");
        if (parts.length < 2 || !String.valueOf(uid).equals(parts[0])) {
            throw new IllegalArgumentException("预下单订单号无效");
        }
        if (parts.length >= 4 && ("shoppingCart".equals(parts[1])
                || "again".equals(parts[1])
                || "buyNowProduct".equals(parts[1])
                || "buyNowSeckill".equals(parts[1])
                || "buyNowBargain".equals(parts[1])
                || "buyNowCombination".equals(parts[1]))) {
            return new PreOrderSource(parts[1], parts[2]);
        }
        return new PreOrderSource("shoppingCart", parts[1]);
    }

    private String againOrderNo(Map<String, Object> body) {
        String orderNo = stringValue(body, "orderNo", "orderId", "uni");
        if (StringUtils.hasText(orderNo)) {
            return orderNo;
        }
        List<?> orderDetails = body == null ? List.of() : (List<?>) body.getOrDefault("orderDetails", List.of());
        for (Object detail : orderDetails) {
            if (detail instanceof Map<?, ?> map) {
                Object value = firstPresent(map.get("orderNo"), firstPresent(map.get("orderId"), map.get("uni")));
                if (value != null && StringUtils.hasText(String.valueOf(value))) {
                    return String.valueOf(value);
                }
            }
        }
        return "";
    }

    private BuyNowDetail buyNowDetail(Map<String, Object> body) {
        Map<?, ?> source = body == null ? Map.of() : body;
        List<?> orderDetails = body == null ? List.of() : (List<?>) body.getOrDefault("orderDetails", List.of());
        if (!orderDetails.isEmpty() && orderDetails.get(0) instanceof Map<?, ?> detail) {
            source = detail;
        }
        return new BuyNowDetail(
                intValueAny(source, "productId", "product_id"),
                intValueAny(source, "attrValueId", "attr_value_id", "uniqueId"),
                intValueAny(source, "productNum", "product_num", "cartNum", "num"),
                intValueAny(source, "seckillId", "seckill_id"),
                intValueAny(source, "bargainId", "bargain_id"),
                intValueAny(source, "bargainUserId", "bargain_user_id", "storeBargainUserId"),
                intValueAny(source, "combinationId", "combination_id"),
                intValueAny(source, "pinkId", "pink_id"));
    }

    private record PreOrderSource(String type, String value) {
        boolean isAgain() {
            return "again".equals(type);
        }

        boolean isBuyNowSeckill() {
            return "buyNowSeckill".equals(type);
        }

        boolean isBuyNowBargain() {
            return "buyNowBargain".equals(type);
        }

        boolean isBuyNowCombination() {
            return "buyNowCombination".equals(type);
        }

        boolean isBuyNowProduct() {
            return "buyNowProduct".equals(type);
        }

        boolean isShoppingCart() {
            return "shoppingCart".equals(type);
        }

        List<Integer> cartIds() {
            List<Integer> ids = new ArrayList<>();
            if (value == null) {
                return ids;
            }
            for (String item : value.split(",")) {
                if (StringUtils.hasText(item)) {
                    ids.add(Integer.valueOf(item.trim()));
                }
            }
            return ids.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
        }
    }

    private record BuyNowDetail(
            Integer productId,
            Integer attrValueId,
            Integer productNum,
            Integer seckillId,
            Integer bargainId,
            Integer bargainUserId,
            Integer combinationId,
            Integer pinkId) {
        String encode() {
            return nvl(productId) + ":" + nvl(attrValueId) + ":" + nvl(productNum) + ":"
                    + nvl(seckillId) + ":" + nvl(bargainId) + ":" + nvl(bargainUserId) + ":"
                    + nvl(combinationId) + ":" + nvl(pinkId);
        }

        static BuyNowDetail decode(String value) {
            String[] parts = value == null ? new String[0] : value.split(":");
            if (parts.length < 5) {
                throw new IllegalArgumentException("预下单订单号无效");
            }
            if (parts.length == 5) {
                return new BuyNowDetail(parse(parts[0]), parse(parts[1]), parse(parts[2]), null, parse(parts[3]), parse(parts[4]), null, null);
            }
            if (parts.length < 8) {
                throw new IllegalArgumentException("预下单订单号无效");
            }
            return new BuyNowDetail(
                    parse(parts[0]),
                    parse(parts[1]),
                    parse(parts[2]),
                    parse(parts[3]),
                    parse(parts[4]),
                    parse(parts[5]),
                    parse(parts[6]),
                    parse(parts[7]));
        }

        private static Integer parse(String value) {
            return StringUtils.hasText(value) && !"0".equals(value) ? Integer.valueOf(value) : null;
        }

        private static Integer nvl(Integer value) {
            return value == null ? 0 : value;
        }
    }

    private String createOrderNo() {
        return "order" + Instant.now().toEpochMilli() + ThreadLocalRandom.current().nextInt(100000, 999999);
    }

    private String createVerifyCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000000, 999999999));
    }

    private String fullAddress(Map<String, Object> address) {
        return truncate(stringOrDefault(address.get("province"), "")
                + stringOrDefault(address.get("city"), "")
                + stringOrDefault(address.get("district"), "")
                + stringOrDefault(address.get("detail"), ""), 100);
    }

    private String orderStatusText(Map<String, Object> order) {
        int paid = numberValue(order.get("paid"));
        int status = numberValue(order.get("status"));
        int refundStatus = numberValue(order.get("refundStatus"));
        if (refundStatus == 1) {
            return "申请退款中";
        }
        if (refundStatus == 2) {
            return "已退款";
        }
        if (refundStatus == 3) {
            return "退款中";
        }
        if (paid == 0) {
            return "待付款";
        }
        if (status == 0 && numberValue(order.get("shippingType")) == 2) {
            return "待核销";
        }
        return switch (status) {
            case 0 -> "待发货";
            case 1 -> "待收货";
            case 2 -> "待评价";
            case 3 -> "已完成";
            default -> "进行中";
        };
    }

    private Object firstPresent(Object first, Object second) {
        return first == null ? second : first;
    }

    private String firstText(String first, Object second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return second == null ? "" : String.valueOf(second);
    }

    private int numberValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        return 0;
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            return Long.parseLong(String.valueOf(value));
        }
        return 0L;
    }

    private int intValueOrDefault(Map<String, Object> body, String key, int defaultValue) {
        Integer value = intValue(body, key);
        return value == null ? defaultValue : value;
    }

    private boolean selfMentionEnabled() {
        String value = commerceMapper.selectConfigValue("store_self_mention");
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private LocalDateTime localDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        return LocalDateTime.parse(String.valueOf(value).replace(' ', 'T'));
    }

    private Map<String, Object> normalizeStore(Map<String, Object> store) {
        Map<String, Object> result = new HashMap<>(store);
        result.put("image", normalizeAsset(stringOrDefault(store.get("image"), "")));
        String address = stringOrDefault(store.get("address"), "");
        String detailedAddress = stringOrDefault(store.get("detailedAddress"), "");
        result.put("fullAddress", address + (StringUtils.hasText(detailedAddress) ? ", " + detailedAddress : ""));
        return result;
    }

    private boolean validScore(Integer score) {
        return score != null && score >= 1 && score <= 5;
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

    private Integer intValue(Map<String, Object> body, String... names) {
        if (body == null) {
            return null;
        }
        for (String name : names) {
            Object value = body.get(name);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return Integer.valueOf(String.valueOf(value));
            }
        }
        return null;
    }

    private Integer intValueAny(Map<?, ?> body, String... names) {
        if (body == null) {
            return null;
        }
        for (String name : names) {
            Object value = body.get(name);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return Integer.valueOf(String.valueOf(value));
            }
        }
        return null;
    }

    private String stringValue(Map<String, Object> body, String name) {
        if (body == null || body.get(name) == null) {
            return "";
        }
        return String.valueOf(body.get(name)).trim();
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

    private String clearUploadPrefix(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replaceAll("https?://[^,]+(?=/crmebimage/)", "")
                .replaceAll("https?://[^,]+(?=/public/)", "");
    }

    private String normalizeJoinedAssets(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return String.join(",", value.split(",")).lines()
                .flatMap(line -> java.util.Arrays.stream(line.split(",")))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::normalizeAsset)
                .collect(Collectors.joining(","));
    }

    private String refundStatusText(int status) {
        return switch (status) {
            case 1 -> "申请退款中";
            case 2 -> "已退款";
            case 3 -> "退款中";
            default -> "未退款";
        };
    }

    private String normalizePics(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return clearUploadPrefix(value.trim()
                .replace("[\"", "")
                .replace("\"]", "")
                .replace("\"", ""));
    }

    private String normalizeAsset(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/")) {
            return value;
        }
        return "/" + value;
    }

    private String stringOrDefault(Object value, String fallback) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return fallback;
        }
        return String.valueOf(value);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
