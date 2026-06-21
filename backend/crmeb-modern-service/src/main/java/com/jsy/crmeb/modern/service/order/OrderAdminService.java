package com.jsy.crmeb.modern.service.order;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.config.CrmebRuntimeProperties;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderCountItemResponse;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderDetailResponse;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderInfoResponse;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderInfoRow;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderItemResponse;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderSpreadInfoResponse;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderSearchRequest;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderStatisticsRequest;
import com.jsy.crmeb.modern.service.order.dto.StoreOrderTimeResponse;
import com.jsy.crmeb.modern.service.order.dto.StoreStaffDetailResponse;
import com.jsy.crmeb.modern.service.order.dto.StoreStaffTopDetailResponse;
import com.jsy.crmeb.modern.service.order.dto.SystemWriteOffOrderResponse;
import com.jsy.crmeb.modern.service.order.dto.SystemWriteOffOrderSearchRequest;
import com.jsy.crmeb.modern.service.order.entity.StoreOrder;
import com.jsy.crmeb.modern.service.order.mapper.StoreOrderInfoMapper;
import com.jsy.crmeb.modern.service.order.mapper.StoreOrderMapper;
import com.jsy.crmeb.modern.service.schedule.task.OrderReceiptTask;
import com.jsy.crmeb.modern.service.schedule.task.OrderRefundTask;
import com.jsy.crmeb.modern.service.admin.entity.SystemAdmin;
import com.jsy.crmeb.modern.service.admin.mapper.SystemAdminMapper;
import com.jsy.crmeb.modern.service.store.entity.SystemStore;
import com.jsy.crmeb.modern.service.store.mapper.SystemStoreMapper;
import com.jsy.crmeb.modern.service.user.entity.User;
import com.jsy.crmeb.modern.service.user.mapper.UserFinanceMapper;
import com.jsy.crmeb.modern.service.user.mapper.UserMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrderAdminService {
    private static final String STATUS_ALL = "all";
    private static final String STATUS_UNPAID = "unPaid";
    private static final String STATUS_NOT_SHIPPED = "notShipped";
    private static final String STATUS_SPIKE = "spike";
    private static final String STATUS_BARGAIN = "bargain";
    private static final String STATUS_COMPLETE = "complete";
    private static final String STATUS_TOBE_WRITTEN_OFF = "toBeWrittenOff";
    private static final String STATUS_APPLY_REFUND = "applyRefund";
    private static final String STATUS_REFUNDING = "refunding";
    private static final String STATUS_REFUNDED = "refunded";
    private static final String STATUS_REFUND_REFUSED = "refundRefused";
    private static final String STATUS_DELETED = "deleted";

    private final StoreOrderMapper storeOrderMapper;
    private final StoreOrderInfoMapper storeOrderInfoMapper;
    private final UserMapper userMapper;
    private final UserFinanceMapper userFinanceMapper;
    private final SystemStoreMapper systemStoreMapper;
    private final SystemAdminMapper systemAdminMapper;
    private final CrmebRuntimeProperties runtimeProperties;
    private final StringRedisTemplate redisTemplate;

    public OrderAdminService(
            StoreOrderMapper storeOrderMapper,
            StoreOrderInfoMapper storeOrderInfoMapper,
            UserMapper userMapper,
            UserFinanceMapper userFinanceMapper,
            SystemStoreMapper systemStoreMapper,
            SystemAdminMapper systemAdminMapper,
            CrmebRuntimeProperties runtimeProperties,
            StringRedisTemplate redisTemplate) {
        this.storeOrderMapper = storeOrderMapper;
        this.storeOrderInfoMapper = storeOrderInfoMapper;
        this.userMapper = userMapper;
        this.userFinanceMapper = userFinanceMapper;
        this.systemStoreMapper = systemStoreMapper;
        this.systemAdminMapper = systemAdminMapper;
        this.runtimeProperties = runtimeProperties;
        this.redisTemplate = redisTemplate;
    }

    public PageResponse<StoreOrderDetailResponse> list(StoreOrderSearchRequest request) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 20 : request.getLimit();
        Page<StoreOrder> orderPage = storeOrderMapper.selectPage(
                new Page<>(page, limit),
                buildQuery(request, request.getStatus(), true));
        Map<Integer, List<StoreOrderInfoResponse>> productMap = getOrderProductMap(orderPage.getRecords());
        Map<Integer, LocalDateTime> deliveryTimeMap = deliveryTimeMap(orderPage.getRecords());
        List<StoreOrderDetailResponse> list = orderPage.getRecords().stream()
                .map(order -> toDetailResponse(order, productMap.getOrDefault(order.getId(), List.of()), deliveryTimeMap.get(order.getId())))
                .toList();
        return new PageResponse<>(page, limit, orderPage.getTotal(), list);
    }

    public Map<String, String> exportOrder(StoreOrderSearchRequest request) {
        Page<StoreOrder> orderPage = storeOrderMapper.selectPage(new Page<>(1, 10000), buildQuery(request, request.getStatus(), true));
        if (orderPage.getRecords().isEmpty()) {
            throw new IllegalArgumentException("没有可导出的数据！");
        }
        Map<Integer, List<StoreOrderInfoResponse>> productMap = getOrderProductMap(orderPage.getRecords());
        Map<Integer, LocalDateTime> deliveryTimeMap = deliveryTimeMap(orderPage.getRecords());
        List<String> lines = new ArrayList<>();
        lines.add(csvLine(List.of("订单号", "实际支付金额", "创建时间", "商品信息", "订单状态", "支付方式", "订单类型", "用户姓名")));
        for (StoreOrder order : orderPage.getRecords()) {
            StoreOrderDetailResponse response = toDetailResponse(order, productMap.getOrDefault(order.getId(), List.of()), deliveryTimeMap.get(order.getId()));
            String productName = response.getProductList().stream()
                    .map(item -> item.getInfo() == null ? "" : item.getInfo().getProductName())
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(","));
            lines.add(csvLine(List.of(
                    response.getOrderId(),
                    moneyText(response.getPayPrice()),
                    formatDateTime(response.getCreateTime()),
                    productName,
                    response.getStatusStr() == null ? "" : response.getStatusStr().getOrDefault("value", ""),
                    response.getPayTypeStr(),
                    response.getOrderType(),
                    response.getRealName())));
        }
        String fileName = "订单导出_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(111111111, 999999999)
                + ".csv";
        return Map.of("fileName", writeExportFile(fileName, lines, "导出订单失败："));
    }

    public StoreOrderCountItemResponse statusNum(String dateLimit, Integer type, String orderNo) {
        StoreOrderSearchRequest request = new StoreOrderSearchRequest();
        request.setDateLimit(dateLimit);
        request.setType(type == null ? 2 : type);
        request.setOrderNo(orderNo);

        StoreOrderCountItemResponse response = new StoreOrderCountItemResponse();
        response.setAll(count(request, STATUS_ALL));
        response.setUnPaid(count(request, STATUS_UNPAID));
        response.setNotShipped(count(request, STATUS_NOT_SHIPPED));
        response.setSpike(count(request, STATUS_SPIKE));
        response.setBargain(count(request, STATUS_BARGAIN));
        response.setComplete(count(request, STATUS_COMPLETE));
        response.setToBeWrittenOff(count(request, STATUS_TOBE_WRITTEN_OFF));
        response.setRefunding(count(request, STATUS_REFUNDING));
        response.setRefunded(count(request, STATUS_REFUNDED));
        response.setRefundRefused(count(request, STATUS_REFUND_REFUSED));
        response.setDeleted(count(request, STATUS_DELETED));
        return response;
    }

    public SystemWriteOffOrderResponse writeOffList(SystemWriteOffOrderSearchRequest request) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 20 : Math.min(request.getLimit(), 100);
        QueryWrapper<StoreOrder> baseQuery = buildWriteOffQuery(request, false);
        Page<StoreOrder> orderPage = storeOrderMapper.selectPage(new Page<>(page, limit), buildWriteOffQuery(request, true));

        SystemWriteOffOrderResponse response = new SystemWriteOffOrderResponse();
        response.setTotal(orderPage.getTotal());
        response.setOrderTotalPrice(sumMoney(buildWriteOffQuery(request, false), "pay_price"));
        response.setRefundTotalPrice(sumMoney(buildWriteOffQuery(request, false), "refund_price"));
        response.setRefundTotal(Math.toIntExact(storeOrderMapper.selectCount(baseQuery.eq("refund_status", 2))));

        Map<Integer, List<StoreOrderInfoResponse>> productMap = getOrderProductMap(orderPage.getRecords());
        Map<Integer, SystemStore> storeMap = loadStores(orderPage.getRecords());
        Map<Integer, SystemAdmin> adminMap = loadAdmins(orderPage.getRecords());
        Map<Integer, User> userMap = loadUsers(orderPage.getRecords());
        Map<Integer, User> spreadUserMap = loadSpreadUsers(userMap);
        List<StoreOrderItemResponse> list = orderPage.getRecords().stream()
                .map(order -> toWriteOffResponse(
                        order,
                        productMap.getOrDefault(order.getId(), List.of()),
                        storeMap.get(order.getStoreId()),
                        adminMap.get(order.getClerkId()),
                        userMap.get(order.getUid()),
                        spreadUserMap))
                .toList();
        response.setList(new PageResponse<>(page, limit, orderPage.getTotal(), list));
        return response;
    }

    public StoreStaffTopDetailResponse statistics() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = today.atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = today.withDayOfMonth(1).plusMonths(1).atStartOfDay();

        StoreStaffTopDetailResponse response = new StoreStaffTopDetailResponse();
        response.setOrderCount(storeOrderMapper.selectCount(paidNoRefundQuery()));
        response.setSumPrice(sumMoney(paidNoRefundQuery(), "pay_price"));
        response.setUnpaidCount(count(new StoreOrderSearchRequest(), STATUS_UNPAID));
        response.setUnshippedCount(count(new StoreOrderSearchRequest(), STATUS_NOT_SHIPPED));
        response.setReceivedCount(count(new StoreOrderSearchRequest(), STATUS_SPIKE));
        response.setVerificationCount(count(new StoreOrderSearchRequest(), STATUS_TOBE_WRITTEN_OFF));
        response.setCompleteCount(count(new StoreOrderSearchRequest(), STATUS_COMPLETE));
        response.setRefundCount(storeOrderMapper.selectCount(refundAnyQuery()));
        response.setTodayCount(countPaidNoRefundBetween("pay_time", todayStart, todayEnd));
        response.setTodayPrice(sumPaidNoRefundBetween("pay_time", todayStart, todayEnd));
        response.setProCount(countPaidNoRefundBetween("create_time", yesterdayStart, yesterdayEnd));
        response.setProPrice(sumPaidNoRefundBetween("create_time", yesterdayStart, yesterdayEnd));
        response.setMonthCount(countPaidNoRefundBetween("pay_time", monthStart, monthEnd));
        response.setMonthPrice(sumPaidNoRefundBetween("pay_time", monthStart, monthEnd));
        return response;
    }

    public List<StoreStaffDetailResponse> statisticsData(StoreOrderStatisticsRequest request) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 10 : Math.min(request.getLimit(), 100);
        int offset = (page - 1) * limit;
        DateRange range = oldDateRange(request.getDateLimit());
        return storeOrderMapper.selectOrderVerificationDetail(range.start(), range.end(), offset, limit).stream()
                .peek(item -> item.setPrice(money(item.getPrice())))
                .toList();
    }

    public StoreOrderTimeResponse orderTime(StoreOrderStatisticsRequest request) {
        int type = request.getType() == 1 ? 1 : 2;
        DateRange current = oldDateRange(request.getDateLimit());
        DateRange previous = previousDateRange(current);
        String dateFormat = "month".equals(request.getDateLimit()) || "lately30".equals(request.getDateLimit())
                ? "%m-%d"
                : "%Y-%m-%d";
        List<StoreOrderTimeResponse.StoreOrderTimeChartItem> chart = storeOrderMapper
                .selectOrderTimeChart(type, dateFormat, current.start(), current.end())
                .stream()
                .peek(item -> item.setNum(money(item.getNum())))
                .toList();
        BigDecimal currentValue = aggregateOrderTime(type, current);
        BigDecimal previousValue = aggregateOrderTime(type, previous);
        BigDecimal increase = money(currentValue.subtract(previousValue));

        StoreOrderTimeResponse response = new StoreOrderTimeResponse();
        response.setChart(chart);
        response.setTime(currentValue);
        response.setIncreaseTime(increase);
        response.setIncreaseTimeStatus(increase.compareTo(BigDecimal.ZERO) >= 0 ? 1 : 0);
        response.setGrowthRate(growthRate(currentValue, previousValue));
        return response;
    }

    @Transactional
    public boolean refund(String orderNo, BigDecimal amount) {
        if (!StringUtils.hasText(orderNo)) {
            throw new IllegalArgumentException("订单编号不能为空");
        }
        StoreOrder order = getOrderByNo(orderNo);
        if (!isTrue(order.getPaid())) {
            throw new IllegalArgumentException("未支付无法退款");
        }
        if (Integer.valueOf(2).equals(order.getRefundStatus())) {
            throw new IllegalArgumentException("订单已退款");
        }
        BigDecimal refundAmount = money(amount == null ? order.getPayPrice() : amount);
        BigDecimal payPrice = money(order.getPayPrice());
        BigDecimal oldRefundPrice = money(order.getRefundPrice());
        if (oldRefundPrice.add(refundAmount).compareTo(payPrice) > 0) {
            throw new IllegalArgumentException("退款金额大于支付金额，请修改退款金额");
        }
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0 && payPrice.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("退款金额不能为0，请修改退款金额");
        }

        if ("yue".equals(order.getPayType()) && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            User user = userMapper.selectById(order.getUid());
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            BigDecimal balance = money(user.getNowMoney()).add(refundAmount);
            storeOrderMapper.addUserBalance(order.getUid(), refundAmount);
            userFinanceMapper.insertRefundBill(
                    order.getUid(),
                    order.getId(),
                    refundAmount,
                    balance,
                    "订单退款到余额" + refundAmount.stripTrailingZeros().toPlainString() + "元");
        }

        order.setRefundStatus(3);
        order.setRefundPrice(refundAmount);
        storeOrderMapper.updateById(order);
        if ("yue".equals(order.getPayType())) {
            storeOrderMapper.insertOrderStatus(
                    order.getId(),
                    "refund_price",
                    "退款给用户" + refundAmount.stripTrailingZeros().toPlainString() + "元，余额支付已本地退回");
        } else {
            storeOrderMapper.insertOrderStatus(
                    order.getId(),
                    "refund_price",
                    "退款给用户" + refundAmount.stripTrailingZeros().toPlainString() + "元，第三方退款未配置，未调用微信/支付宝真实退款");
        }
        redisTemplate.opsForList().leftPush(OrderRefundTask.ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER, String.valueOf(order.getId()));
        return true;
    }

    @Transactional
    public boolean refundRefuse(String orderNo, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("请填写拒绝退款原因");
        }
        StoreOrder order = getOrderByNo(orderNo);
        if (Integer.valueOf(2).equals(order.getRefundStatus())) {
            throw new IllegalArgumentException("订单已退款");
        }
        order.setRefundStatus(0);
        order.setRefundReason(reason.trim());
        storeOrderMapper.updateById(order);
        storeOrderMapper.insertOrderStatus(
                order.getId(),
                "refund_refuse",
                "不退款原因：" + reason.trim());
        return true;
    }

    @Transactional
    public boolean writeOff(String verifyCode, Integer adminId) {
        StoreOrder order = getWriteOffOrder(verifyCode);
        order.setStatus(2);
        order.setClerkId(adminId == null ? 0 : adminId);
        storeOrderMapper.updateById(order);
        storeOrderMapper.insertOrderStatus(order.getId(), "write_order", "核销订单");
        redisTemplate.opsForList().leftPush(OrderReceiptTask.ORDER_TASK_REDIS_KEY_AFTER_TAKE_BY_USER, String.valueOf(order.getId()));
        return true;
    }

    public StoreOrderDetailResponse writeConfirm(String verifyCode) {
        StoreOrder order = getWriteOffOrder(verifyCode);
        return toDetailResponse(order, getOrderProductMap(List.of(order)).getOrDefault(order.getId(), List.of()), deliveryTime(order));
    }

    public StoreOrderDetailResponse info(String orderNo) {
        StoreOrder order = getOrderByNo(orderNo);
        return toDetailResponse(order, getOrderProductMap(List.of(order)).getOrDefault(order.getId(), List.of()), deliveryTime(order));
    }

    public PageResponse<Map<String, Object>> statusList(String orderNo, int page, int limit) {
        StoreOrder order = getOrderByNo(orderNo);
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        List<Map<String, Object>> list = storeOrderMapper.selectOrderStatusList(order.getOrderId(), (safePage - 1) * safeLimit, safeLimit);
        long total = storeOrderMapper.countOrderStatusList(order.getOrderId());
        return new PageResponse<>(safePage, safeLimit, total, list);
    }

    @Transactional
    public boolean send(Map<String, Object> body) {
        String orderNo = stringValue(body, "orderNo", "orderId");
        StoreOrder order = getOrderByNo(orderNo);
        if (!isTrue(order.getPaid())) {
            throw new IllegalArgumentException("未支付订单不能发货");
        }
        if (!isZero(order.getRefundStatus())) {
            throw new IllegalArgumentException("退款订单不能发货");
        }
        if (Integer.valueOf(2).equals(order.getShippingType())) {
            throw new IllegalArgumentException("自提订单请走核销流程");
        }

        String deliveryType = normalizeDeliveryType(stringValue(body, "deliveryType", "type"));
        if ("express".equals(deliveryType)) {
            String expressCode = stringValue(body, "expressCode", "code");
            String expressNumber = stringValue(body, "expressNumber", "deliveryId");
            if (!StringUtils.hasText(expressCode)) {
                throw new IllegalArgumentException("请输入快递公司");
            }
            if (!StringUtils.hasText(expressNumber)) {
                throw new IllegalArgumentException("请输入快递单号");
            }
            order.setDeliveryType("express");
            order.setDeliveryCode(expressCode.trim());
            order.setDeliveryName(expressName(body, expressCode));
            order.setDeliveryId(expressNumber.trim());
            order.setExpressRecordType(intValue(body.get("expressRecordType"), 1));
        } else if ("send".equals(deliveryType)) {
            String deliveryName = stringValue(body, "deliveryName");
            String deliveryTel = stringValue(body, "deliveryTel", "deliveryId");
            if (!StringUtils.hasText(deliveryName)) {
                throw new IllegalArgumentException("请填写送货人");
            }
            if (!StringUtils.hasText(deliveryTel)) {
                throw new IllegalArgumentException("请填写送货电话");
            }
            order.setDeliveryType("send");
            order.setDeliveryName(deliveryName.trim());
            order.setDeliveryId(deliveryTel.trim());
            order.setDeliveryCode("");
            order.setExpressRecordType(0);
        } else {
            order.setDeliveryType("noNeed");
            order.setDeliveryName("");
            order.setDeliveryId("");
            order.setDeliveryCode("");
            order.setExpressRecordType(0);
        }
        order.setStatus(1);
        storeOrderMapper.updateById(order);
        storeOrderMapper.insertOrderStatus(order.getId(), "delivery", deliveryMessage(order));
        return true;
    }

    @Transactional
    public boolean mark(String orderNo, String mark) {
        if (!StringUtils.hasText(mark)) {
            throw new IllegalArgumentException("请填写备注信息");
        }
        StoreOrder order = getOrderByNo(orderNo);
        order.setRemark(mark.trim());
        storeOrderMapper.updateById(order);
        storeOrderMapper.insertOrderStatus(order.getId(), "mark", "订单备注：" + mark.trim());
        return true;
    }

    @Transactional
    public boolean updatePrice(String orderNo, BigDecimal payPrice) {
        if (payPrice == null) {
            throw new IllegalArgumentException("请填写金额");
        }
        BigDecimal nextPayPrice = money(payPrice);
        if (nextPayPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("支付金额不能小于0");
        }
        StoreOrder order = getOrderByNo(orderNo);
        if (isTrue(order.getPaid())) {
            throw new IllegalArgumentException("已支付订单不能改价");
        }
        order.setPayPrice(nextPayPrice);
        order.setIsAlterPrice(1);
        storeOrderMapper.updateById(order);
        storeOrderMapper.insertOrderStatus(order.getId(), "edit_price", "订单改价为" + nextPayPrice.toPlainString() + "元");
        return true;
    }

    @Transactional
    public boolean delete(String orderNo) {
        StoreOrder order = getOrderByNo(orderNo);
        if (!isTrue(order.getIsDel())) {
            throw new IllegalArgumentException("用户未删除的订单无法删除");
        }
        order.setIsSystemDel(1);
        storeOrderMapper.updateById(order);
        storeOrderMapper.insertOrderStatus(order.getId(), "system_delete", "后台删除订单");
        return true;
    }

    public boolean print(String orderNo) {
        StoreOrder order = getOrderByNo(orderNo);
        List<Map<String, Object>> rows = storeOrderMapper.selectOrderStatusList(order.getOrderId(), 0, 10000);
        if (rows.isEmpty()) {
            rows = List.of(Map.of(
                    "oid", order.getId(),
                    "changeType", "order_print",
                    "changeMessage", "订单打印",
                    "createTime", LocalDateTime.now()));
        }
        List<String> lines = new ArrayList<>();
        lines.add(csvLine(List.of("订单号", "操作类型", "操作内容", "时间")));
        for (Map<String, Object> row : rows) {
            lines.add(csvLine(List.of(
                    String.valueOf(order.getOrderId()),
                    String.valueOf(row.getOrDefault("changeType", "")),
                    String.valueOf(row.getOrDefault("changeMessage", "")),
                    String.valueOf(row.getOrDefault("createTime", "")))));
        }
        String fileName = "订单小票_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(111111111, 999999999)
                + ".csv";
        Path exportDir = Path.of(runtimeProperties.getImagePath(), "crmebimage", "export").toAbsolutePath().normalize();
        try {
            Files.createDirectories(exportDir);
            Files.writeString(exportDir.resolve(fileName), "\uFEFF" + String.join("\n", lines), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("打印失败：" + exception.getMessage());
        }
        return true;
    }

    private long count(StoreOrderSearchRequest request, String status) {
        return storeOrderMapper.selectCount(buildQuery(request, status, false));
    }

    private StoreOrder getOrderByNo(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new IllegalArgumentException("订单编号不能为空");
        }
        StoreOrder order = storeOrderMapper.selectOne(new QueryWrapper<StoreOrder>()
                .eq("order_id", orderNo.trim())
                .eq("is_system_del", 0)
                .last("limit 1"));
        if (order == null) {
            throw new IllegalArgumentException("未找到对应订单信息");
        }
        return order;
    }

    private StoreOrder getWriteOffOrder(String verifyCode) {
        if (!StringUtils.hasText(verifyCode)) {
            throw new IllegalArgumentException("请输入核销码");
        }
        StoreOrder order = storeOrderMapper.selectOne(new QueryWrapper<StoreOrder>()
                .eq("verify_code", verifyCode.trim())
                .eq("paid", 1)
                .eq("refund_status", 0)
                .eq("shipping_type", 2)
                .eq("is_del", 0)
                .eq("is_system_del", 0)
                .last("limit 1"));
        if (order == null) {
            throw new IllegalArgumentException("核销码 " + verifyCode.trim() + " 的订单未找到");
        }
        if (!isZero(order.getStatus())) {
            throw new IllegalArgumentException("核销码 " + verifyCode.trim() + " 的订单已核销");
        }
        return order;
    }

    private Map<Integer, List<StoreOrderInfoResponse>> getOrderProductMap(List<StoreOrder> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<Integer> orderIds = orders.stream().map(StoreOrder::getId).distinct().toList();
        return storeOrderInfoMapper.selectByOrderIds(orderIds).stream()
                .map(this::toInfoResponse)
                .collect(Collectors.groupingBy(StoreOrderInfoResponse::getOrderId));
    }

    private Map<Integer, LocalDateTime> deliveryTimeMap(List<StoreOrder> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<Integer> orderIds = orders.stream()
                .map(StoreOrder::getId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, LocalDateTime> result = new LinkedHashMap<>();
        for (Map<String, Object> row : storeOrderMapper.selectDeliveryTimes(orderIds)) {
            Object oid = row.get("oid");
            LocalDateTime deliveryTime = localDateTime(row.get("deliveryTime"));
            if (oid != null && deliveryTime != null) {
                result.put(Integer.parseInt(String.valueOf(oid)), deliveryTime);
            }
        }
        return result;
    }

    private LocalDateTime deliveryTime(StoreOrder order) {
        if (order == null || order.getId() == null) {
            return null;
        }
        return deliveryTimeMap(List.of(order)).get(order.getId());
    }

    private LocalDateTime localDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime time) {
            return time;
        }
        return parseOldDateTime(String.valueOf(value), true);
    }

    private StoreOrderInfoResponse toInfoResponse(StoreOrderInfoRow row) {
        StoreOrderInfoResponse response = new StoreOrderInfoResponse();
        response.setId(row.getId());
        response.setOrderId(row.getOrderId());
        response.setProductId(row.getProductId());
        response.setUnique(row.getUnique());

        StoreOrderInfoResponse.OrderInfoDetail info = new StoreOrderInfoResponse.OrderInfoDetail();
        info.setImage(normalizeAsset(row.getImage()));
        info.setProductId(row.getProductId());
        info.setAttrValueId(row.getAttrValueId());
        info.setProductName(row.getProductName());
        info.setSku(row.getSku());
        info.setPrice(row.getPrice());
        info.setPayNum(row.getPayNum());
        response.setInfo(info);
        return response;
    }

    private StoreOrderDetailResponse toDetailResponse(StoreOrder order, List<StoreOrderInfoResponse> products, LocalDateTime deliveryTime) {
        StoreOrderDetailResponse response = new StoreOrderDetailResponse();
        response.setId(order.getId());
        response.setOrderId(order.getOrderId());
        response.setUid(order.getUid());
        response.setTotalPrice(order.getTotalPrice());
        response.setTotalNum(order.getTotalNum());
        response.setTotalPostage(order.getTotalPostage());
        response.setPayPrice(order.getPayPrice());
        response.setPayPostage(order.getPayPostage());
        response.setPayType(order.getPayType());
        response.setCreateTime(order.getCreateTime());
        response.setStatus(order.getStatus());
        response.setProductList(products);
        response.setStatusStr(statusStr(order));
        response.setPayTypeStr(payTypeStr(order.getPayType()));
        response.setIsDel(isTrue(order.getIsDel()));
        response.setRefundStatus(order.getRefundStatus());
        response.setRefundPrice(order.getRefundPrice());
        response.setRefundReasonWapImg(normalizeRefundImages(order.getRefundReasonWapImg()));
        response.setRefundReasonWapExplain(order.getRefundReasonWapExplain());
        response.setRefundReasonTime(order.getRefundReasonTime());
        response.setRefundReasonWap(order.getRefundReasonWap());
        response.setRefundReason(order.getRefundReason());
        response.setOrderType(orderTypeStr(order));
        response.setRemark(order.getRemark());
        response.setMark(order.getMark());
        response.setRealName(order.getRealName());
        response.setProTotalPrice(order.getProTotalPrice());
        response.setCouponPrice(order.getCouponPrice());
        response.setPaid(isTrue(order.getPaid()));
        response.setType(order.getType());
        response.setIsAlterPrice(isTrue(order.getIsAlterPrice()));
        response.setVerifyCode(order.getVerifyCode());
        response.setShippingType(order.getShippingType());
        response.setStoreId(order.getStoreId());
        response.setUserPhone(order.getUserPhone());
        response.setUserAddress(order.getUserAddress());
        response.setDeliveryName(order.getDeliveryName());
        response.setDeliveryType(order.getDeliveryType());
        response.setDeliveryId(order.getDeliveryId());
        response.setDeliveryCode(order.getDeliveryCode());
        response.setExpressRecordType(order.getExpressRecordType());
        response.setDeliveryTime(deliveryTime);
        return response;
    }

    private StoreOrderItemResponse toWriteOffResponse(
            StoreOrder order,
            List<StoreOrderInfoResponse> products,
            SystemStore store,
            SystemAdmin clerk,
            User user,
            Map<Integer, User> spreadUserMap) {
        StoreOrderItemResponse response = new StoreOrderItemResponse();
        response.setId(order.getId());
        response.setOrderId(order.getOrderId());
        response.setUid(order.getUid());
        response.setRealName(order.getRealName());
        response.setUserPhone(order.getUserPhone());
        response.setTotalPrice(order.getTotalPrice());
        response.setPayPrice(order.getPayPrice());
        response.setPaid(isTrue(order.getPaid()));
        response.setPayTime(order.getPayTime());
        response.setPayType(order.getPayType());
        response.setCreateTime(order.getCreateTime());
        response.setStatus(order.getStatus());
        response.setStoreName(store == null ? "" : store.getName());
        response.setClerkName(clerk == null ? "" : StringUtils.hasText(clerk.getRealName()) ? clerk.getRealName() : clerk.getAccount());
        response.setProductList(products);
        response.setStatusStr(statusStr(order));
        response.setPayTypeStr(payTypeStr(order.getPayType()));
        response.setTotalPostage(order.getTotalPostage());
        response.setPayPostage(order.getPayPostage());
        response.setIsDel(isTrue(order.getIsDel()));
        response.setIsSystemDel(isTrue(order.getIsSystemDel()));
        response.setRemark(order.getRemark());
        response.setRefundPrice(order.getRefundPrice());
        response.setRefundStatus(order.getRefundStatus());
        response.setTotalNum(order.getTotalNum());
        response.setShippingType(order.getShippingType());
        response.setVerifyCode(order.getVerifyCode());
        response.setSpreadInfo(spreadInfo(user, spreadUserMap));
        response.setOrderType(orderTypeStr(order));
        return response;
    }

    private StoreOrderSpreadInfoResponse spreadInfo(User user, Map<Integer, User> spreadUserMap) {
        StoreOrderSpreadInfoResponse response = new StoreOrderSpreadInfoResponse();
        if (user == null || user.getSpreadUid() == null || user.getSpreadUid() <= 0) {
            return response;
        }
        User spread = spreadUserMap.get(user.getSpreadUid());
        if (spread == null) {
            return response;
        }
        response.setId(spread.getUid());
        response.setName(StringUtils.hasText(spread.getNickname()) ? spread.getNickname() : StringUtils.hasText(spread.getRealName()) ? spread.getRealName() : "");
        return response;
    }

    private QueryWrapper<StoreOrder> buildQuery(StoreOrderSearchRequest request, String status, boolean includeOrder) {
        QueryWrapper<StoreOrder> query = new QueryWrapper<>();
        if (StringUtils.hasText(request.getOrderNo())) {
            query.eq("order_id", request.getOrderNo().trim());
        }
        applyDateLimit(query, request.getDateLimit());
        applyStatus(query, status);
        if (request.getType() != null && !Integer.valueOf(2).equals(request.getType())) {
            query.eq("type", request.getType());
        }
        if (includeOrder) {
            query.orderByDesc("id");
        }
        return query;
    }

    private QueryWrapper<StoreOrder> buildWriteOffQuery(SystemWriteOffOrderSearchRequest request, boolean includeOrder) {
        QueryWrapper<StoreOrder> query = new QueryWrapper<>();
        query.eq("is_del", 0).eq("shipping_type", 2);
        applyDateLimit(query, request.getDateLimit());
        if (request.getStoreId() != null && request.getStoreId() > 0) {
            query.eq("store_id", request.getStoreId());
        }
        if (StringUtils.hasText(request.getKeywords())) {
            String keyword = request.getKeywords().trim();
            query.and(item -> {
                item.like("real_name", keyword).or().eq("user_phone", keyword).or().eq("order_id", keyword);
                try {
                    item.or().eq("id", Integer.parseInt(keyword));
                } catch (NumberFormatException exception) {
                    // 老接口只在关键字为数字时匹配数据库 ID。
                }
            });
        }
        if (includeOrder) {
            query.orderByDesc("id");
        }
        return query;
    }

    private BigDecimal sumMoney(QueryWrapper<StoreOrder> query, String column) {
        query.select("coalesce(sum(" + column + "), 0)");
        List<Object> values = storeOrderMapper.selectObjs(query);
        if (values == null || values.isEmpty() || values.get(0) == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return money(new BigDecimal(String.valueOf(values.get(0))));
    }

    private String writeExportFile(String fileName, List<String> lines, String errorPrefix) {
        Path exportDir = Path.of(runtimeProperties.getImagePath(), "crmebimage", "export").toAbsolutePath().normalize();
        try {
            Files.createDirectories(exportDir);
            Files.writeString(exportDir.resolve(fileName), "\uFEFF" + String.join("\n", lines), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException(errorPrefix + exception.getMessage());
        }
        return "/crmebimage/export/" + fileName;
    }

    private String csvLine(List<String> values) {
        return values.stream().map(this::csvValue).collect(Collectors.joining(","));
    }

    private String csvValue(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private String moneyText(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private QueryWrapper<StoreOrder> paidNoRefundQuery() {
        return new QueryWrapper<StoreOrder>()
                .eq("is_del", 0)
                .eq("paid", 1)
                .eq("refund_status", 0);
    }

    private QueryWrapper<StoreOrder> refundAnyQuery() {
        return new QueryWrapper<StoreOrder>()
                .eq("is_del", 0)
                .eq("paid", 1)
                .in("refund_status", 1, 2, 3);
    }

    private long countPaidNoRefundBetween(String column, LocalDateTime start, LocalDateTime end) {
        return storeOrderMapper.selectCount(paidNoRefundQuery().between(column, start, end));
    }

    private BigDecimal sumPaidNoRefundBetween(String column, LocalDateTime start, LocalDateTime end) {
        return sumMoney(paidNoRefundQuery().between(column, start, end), "pay_price");
    }

    private BigDecimal aggregateOrderTime(int type, DateRange range) {
        QueryWrapper<StoreOrder> query = paidNoRefundQuery();
        if (range.start() != null) {
            query.ge("pay_time", range.start());
        }
        if (range.end() != null) {
            query.lt("pay_time", range.end());
        }
        if (type == 1) {
            return sumMoney(query, "pay_price");
        }
        return BigDecimal.valueOf(storeOrderMapper.selectCount(query)).setScale(2, RoundingMode.HALF_UP);
    }

    private DateRange previousDateRange(DateRange current) {
        if (current.start() == null || current.end() == null) {
            LocalDate today = LocalDate.now();
            return new DateRange(today.minusDays(1).atStartOfDay(), today.atStartOfDay());
        }
        java.time.Duration duration = java.time.Duration.between(current.start(), current.end());
        LocalDateTime end = current.start();
        return new DateRange(end.minus(duration), end);
    }

    private BigDecimal growthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100).setScale(2) : BigDecimal.ZERO.setScale(2);
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 2, RoundingMode.HALF_UP);
    }

    private DateRange oldDateRange(String dateLimit) {
        LocalDate today = LocalDate.now();
        if (!StringUtils.hasText(dateLimit)) {
            return new DateRange(null, LocalDateTime.now());
        }
        return switch (dateLimit.trim()) {
            case "today" -> new DateRange(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
            case "yesterday" -> new DateRange(today.minusDays(1).atStartOfDay(), today.atStartOfDay());
            case "lately7" -> new DateRange(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay());
            case "lately30" -> new DateRange(today.minusDays(30).atStartOfDay(), today.plusDays(1).atStartOfDay());
            case "month" -> new DateRange(today.withDayOfMonth(1).atStartOfDay(), today.withDayOfMonth(1).plusMonths(1).atStartOfDay());
            case "year" -> new DateRange(today.withDayOfYear(1).atStartOfDay(), today.withDayOfYear(1).plusYears(1).atStartOfDay());
            default -> customDateRange(dateLimit);
        };
    }

    private DateRange customDateRange(String dateLimit) {
        String[] parts = dateLimit.trim().split("\\s*,\\s*|\\s+-\\s+");
        if (parts.length < 2) {
            return new DateRange(null, LocalDateTime.now());
        }
        LocalDateTime start = parseOldDateTime(parts[0], true);
        LocalDateTime end = parseOldDateTime(parts[1], false);
        return new DateRange(start, end);
    }

    private LocalDateTime parseOldDateTime(String value, boolean start) {
        String text = value == null ? "" : value.trim();
        if (!StringUtils.hasText(text)) {
            return start ? null : LocalDateTime.now();
        }
        text = normalizeLegacyDateTimeText(text);
        if (text.length() <= 10) {
            LocalDate day = LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return start ? day.atStartOfDay() : day.plusDays(1).atStartOfDay();
        }
        DateTimeFormatter formatter = text.contains(".")
                ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(text, formatter);
    }

    private Map<Integer, SystemStore> loadStores(List<StoreOrder> orders) {
        List<Integer> ids = orders.stream().map(StoreOrder::getStoreId).filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return systemStoreMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(SystemStore::getId, Function.identity()));
    }

    private Map<Integer, SystemAdmin> loadAdmins(List<StoreOrder> orders) {
        List<Integer> ids = orders.stream().map(StoreOrder::getClerkId).filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return systemAdminMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(SystemAdmin::getId, Function.identity()));
    }

    private Map<Integer, User> loadUsers(List<StoreOrder> orders) {
        List<Integer> ids = orders.stream().map(StoreOrder::getUid).filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(User::getUid, Function.identity()));
    }

    private Map<Integer, User> loadSpreadUsers(Map<Integer, User> userMap) {
        List<Integer> ids = userMap.values().stream()
                .map(User::getSpreadUid)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(User::getUid, Function.identity()));
    }

    private void applyDateLimit(QueryWrapper<StoreOrder> query, String dateLimit) {
        if (!StringUtils.hasText(dateLimit)) {
            return;
        }
        String[] parts = dateLimit.trim().split("\\s*,\\s*|\\s+-\\s+");
        if (parts.length < 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            return;
        }
        LocalDateTime start = parseOldDateTime(parts[0], true);
        LocalDateTime end = parseOldDateTime(parts[1], false);
        if (start == null || end == null) {
            return;
        }
        query.between("create_time", start, end);
    }

    private String normalizeLegacyDateTimeText(String value) {
        String text = value.trim().replace('T', ' ');
        if (!text.contains(".")) {
            return text;
        }
        String[] parts = text.split("\\.", 2);
        String fraction = parts.length > 1 ? parts[1].replaceAll("\\D.*$", "") : "";
        if (!StringUtils.hasText(fraction)) {
            return parts[0];
        }
        if (fraction.length() > 3) {
            fraction = fraction.substring(0, 3);
        }
        return parts[0] + "." + String.format("%-3s", fraction).replace(' ', '0');
    }

    private void applyStatus(QueryWrapper<StoreOrder> query, String status) {
        if (!StringUtils.hasText(status) || STATUS_ALL.equals(status)) {
            query.eq("is_system_del", 0);
            return;
        }
        switch (status) {
            case STATUS_UNPAID -> query.eq("paid", 0).eq("status", 0).eq("is_del", 0);
            case STATUS_NOT_SHIPPED -> query.eq("paid", 1).eq("status", 0).eq("refund_status", 0).eq("shipping_type", 1).eq("is_del", 0);
            case STATUS_SPIKE -> query.eq("paid", 1).eq("status", 1).eq("refund_status", 0).eq("is_del", 0);
            case STATUS_BARGAIN -> query.eq("paid", 1).eq("status", 2).eq("refund_status", 0).eq("is_del", 0);
            case STATUS_COMPLETE -> query.eq("paid", 1).eq("status", 3).eq("refund_status", 0).eq("is_del", 0);
            case STATUS_TOBE_WRITTEN_OFF -> query.eq("paid", 1).eq("status", 0).eq("refund_status", 0).eq("shipping_type", 2).eq("is_del", 0);
            case STATUS_REFUNDING -> query.eq("paid", 1).in("refund_status", 1, 3).eq("is_del", 0);
            case STATUS_REFUNDED -> query.eq("paid", 1).eq("refund_status", 2).eq("is_del", 0);
            case STATUS_REFUND_REFUSED -> query.eq("paid", 1)
                    .eq("refund_status", 0)
                    .isNotNull("refund_reason")
                    .ne("refund_reason", "")
                    .eq("is_del", 0);
            case STATUS_DELETED -> query.eq("is_del", 1);
            default -> {
            }
        }
        query.eq("is_system_del", 0);
    }

    private Map<String, String> statusStr(StoreOrder order) {
        if (isTrue(order.getPaid()) && isZero(order.getRefundStatus()) && StringUtils.hasText(order.getRefundReason())
                && !isTrue(order.getIsDel()) && !isTrue(order.getIsSystemDel())) {
            return status(STATUS_REFUND_REFUSED, "已拒绝退款");
        }
        if (!isTrue(order.getPaid()) && isZero(order.getStatus()) && isZero(order.getRefundStatus())
                && !isTrue(order.getIsDel()) && !isTrue(order.getIsSystemDel())) {
            return status(STATUS_UNPAID, "未支付");
        }
        if (isTrue(order.getPaid()) && isZero(order.getStatus()) && isZero(order.getRefundStatus())
                && Integer.valueOf(1).equals(order.getShippingType()) && !isTrue(order.getIsDel()) && !isTrue(order.getIsSystemDel())) {
            return status(STATUS_NOT_SHIPPED, "未发货");
        }
        if (isTrue(order.getPaid()) && Integer.valueOf(1).equals(order.getStatus()) && isZero(order.getRefundStatus())
                && Integer.valueOf(1).equals(order.getShippingType()) && !isTrue(order.getIsDel()) && !isTrue(order.getIsSystemDel())) {
            return status(STATUS_SPIKE, "待收货");
        }
        if (isTrue(order.getPaid()) && Integer.valueOf(2).equals(order.getStatus()) && isZero(order.getRefundStatus())
                && !isTrue(order.getIsDel()) && !isTrue(order.getIsSystemDel())) {
            return status(STATUS_BARGAIN, "待评价");
        }
        if (isTrue(order.getPaid()) && Integer.valueOf(3).equals(order.getStatus()) && isZero(order.getRefundStatus())
                && !isTrue(order.getIsDel()) && !isTrue(order.getIsSystemDel())) {
            return status(STATUS_COMPLETE, "交易完成");
        }
        if (isTrue(order.getPaid()) && isZero(order.getStatus()) && isZero(order.getRefundStatus())
                && Integer.valueOf(2).equals(order.getShippingType()) && !isTrue(order.getIsDel()) && !isTrue(order.getIsSystemDel())) {
            return status(STATUS_TOBE_WRITTEN_OFF, "待核销");
        }
        if (isTrue(order.getPaid()) && Integer.valueOf(1).equals(order.getRefundStatus())
                && !isTrue(order.getIsDel()) && !isTrue(order.getIsSystemDel())) {
            return status(STATUS_APPLY_REFUND, "申请退款");
        }
        if (isTrue(order.getPaid()) && Integer.valueOf(3).equals(order.getRefundStatus())
                && !isTrue(order.getIsDel()) && !isTrue(order.getIsSystemDel())) {
            return status(STATUS_REFUNDING, "退款中");
        }
        if (isTrue(order.getPaid()) && Integer.valueOf(2).equals(order.getRefundStatus())
                && !isTrue(order.getIsDel()) && !isTrue(order.getIsSystemDel())) {
            return status(STATUS_REFUNDED, "已退款");
        }
        if (isTrue(order.getIsDel()) || isTrue(order.getIsSystemDel())) {
            return status(STATUS_DELETED, "已删除");
        }
        return status("", "");
    }

    private Map<String, String> status(String key, String value) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key", key);
        map.put("value", value);
        return map;
    }

    private String payTypeStr(String payType) {
        if ("weixin".equals(payType)) {
            return "微信支付";
        }
        if ("yue".equals(payType)) {
            return "余额支付";
        }
        if ("alipay".equals(payType)) {
            return "支付宝支付";
        }
        if ("offline".equals(payType)) {
            return "线下支付";
        }
        if ("zeroPay".equals(payType)) {
            return "零元支付";
        }
        return "其他支付";
    }

    private String orderTypeStr(StoreOrder order) {
        String orderType = "[普通订单]";
        if (StringUtils.hasText(order.getVerifyCode())) {
            orderType = "[核销订单]";
        }
        if (order.getSeckillId() != null && order.getSeckillId() > 0) {
            orderType = "[秒杀订单]";
        }
        if (order.getBargainId() != null && order.getBargainId() > 0) {
            orderType = "[砍价订单]";
        }
        if (order.getCombinationId() != null && order.getCombinationId() > 0) {
            orderType = "[拼团订单]";
        }
        return orderType;
    }

    private String normalizeRefundImages(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return List.of(value.split(",")).stream().map(this::normalizeAsset).collect(Collectors.joining(","));
    }

    private String normalizeAsset(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("/")) {
            return trimmed;
        }
        return "/" + trimmed;
    }

    private String normalizeDeliveryType(String value) {
        String type = value == null ? "" : value.trim();
        if ("1".equals(type) || "express".equals(type)) {
            return "express";
        }
        if ("2".equals(type) || "send".equals(type)) {
            return "send";
        }
        return "noNeed";
    }

    @SuppressWarnings("unchecked")
    private String expressName(Map<String, Object> body, String expressCode) {
        Object company = body.get("company");
        if (company instanceof Map<?, ?> companyMap) {
            Object name = companyMap.get("name");
            if (name != null && StringUtils.hasText(String.valueOf(name))) {
                return String.valueOf(name).trim();
            }
        }
        String explicit = stringValue(body, "expressName", "deliveryName");
        if (StringUtils.hasText(explicit)) {
            return explicit.trim();
        }
        String dbName = storeOrderMapper.selectExpressName(expressCode.trim());
        return StringUtils.hasText(dbName) ? dbName : expressCode.trim();
    }

    private String deliveryMessage(StoreOrder order) {
        if ("send".equals(order.getDeliveryType())) {
            return "订单配送，送货人：" + order.getDeliveryName() + "，电话：" + order.getDeliveryId();
        }
        if ("noNeed".equals(order.getDeliveryType())) {
            return "订单无需发货";
        }
        return "订单发货，快递公司：" + order.getDeliveryName() + "，快递单号：" + order.getDeliveryId();
    }

    private String stringValue(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private int intValue(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean isTrue(Integer value) {
        return Integer.valueOf(1).equals(value);
    }

    private boolean isZero(Integer value) {
        return value == null || value == 0;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {
    }
}
