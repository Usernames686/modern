package com.jsy.crmeb.modern.service.coupon;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponInfoResponse;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponReceiveRequest;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponSaveRequest;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponSearchRequest;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponUserResponse;
import com.jsy.crmeb.modern.service.coupon.dto.StoreCouponUserSearchRequest;
import com.jsy.crmeb.modern.service.coupon.entity.StoreCoupon;
import com.jsy.crmeb.modern.service.coupon.entity.StoreCouponUser;
import com.jsy.crmeb.modern.service.coupon.mapper.StoreCouponMapper;
import com.jsy.crmeb.modern.service.coupon.mapper.StoreCouponUserMapper;
import com.jsy.crmeb.modern.service.product.entity.Category;
import com.jsy.crmeb.modern.service.product.entity.StoreProduct;
import com.jsy.crmeb.modern.service.product.mapper.CategoryMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductMapper;
import com.jsy.crmeb.modern.service.user.entity.User;
import com.jsy.crmeb.modern.service.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CouponAdminService {
    private final StoreCouponMapper storeCouponMapper;
    private final StoreCouponUserMapper storeCouponUserMapper;
    private final UserMapper userMapper;
    private final StoreProductMapper storeProductMapper;
    private final CategoryMapper categoryMapper;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CouponAdminService(StoreCouponMapper storeCouponMapper,
                              StoreCouponUserMapper storeCouponUserMapper,
                              UserMapper userMapper,
                              StoreProductMapper storeProductMapper,
                              CategoryMapper categoryMapper) {
        this.storeCouponMapper = storeCouponMapper;
        this.storeCouponUserMapper = storeCouponUserMapper;
        this.userMapper = userMapper;
        this.storeProductMapper = storeProductMapper;
        this.categoryMapper = categoryMapper;
    }

    public PageResponse<StoreCoupon> list(StoreCouponSearchRequest request) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 20 : request.getLimit();
        Page<StoreCoupon> couponPage = storeCouponMapper.selectPage(new Page<>(page, limit), buildListQuery(request));
        return new PageResponse<>(page, limit, couponPage.getTotal(), couponPage.getRecords());
    }

    public PageResponse<StoreCoupon> sendList(StoreCouponSearchRequest request) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 20 : request.getLimit();
        Page<StoreCoupon> couponPage = storeCouponMapper.selectPage(new Page<>(page, limit), buildSendQuery(request));
        return new PageResponse<>(page, limit, couponPage.getTotal(), couponPage.getRecords());
    }

    public List<StoreCoupon> byIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        QueryWrapper<StoreCoupon> query = new QueryWrapper<>();
        query.in("id", ids);
        List<StoreCoupon> coupons = storeCouponMapper.selectList(query);
        coupons.sort((left, right) -> Integer.compare(ids.indexOf(left.getId()), ids.indexOf(right.getId())));
        return coupons;
    }

    public PageResponse<StoreCouponUserResponse> userList(StoreCouponUserSearchRequest request) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 20 : request.getLimit();
        Page<StoreCouponUser> couponUserPage = storeCouponUserMapper.selectPage(
                new Page<>(page, limit),
                buildUserQuery(request));
        List<StoreCouponUser> records = couponUserPage.getRecords();
        Map<Integer, User> userMap = loadUsers(records);
        List<StoreCouponUserResponse> list = records.stream()
                .map(record -> toCouponUserResponse(record, userMap.get(record.getUid())))
                .toList();
        return new PageResponse<>(page, limit, couponUserPage.getTotal(), list);
    }

    public StoreCouponInfoResponse info(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("优惠券ID不能为空");
        }
        StoreCoupon coupon = storeCouponMapper.selectById(id);
        if (coupon == null || Boolean.TRUE.equals(coupon.getIsDel())) {
            throw new IllegalArgumentException("优惠券信息不存在或者已失效！");
        }
        List<Integer> primaryIds = parseIdList(coupon.getPrimaryKey());
        List<StoreProduct> products = List.of();
        List<Category> categories = List.of();
        if (!primaryIds.isEmpty() && Integer.valueOf(2).equals(coupon.getUseType())) {
            products = storeProductMapper.selectBatchIds(primaryIds);
            products.sort((left, right) -> Integer.compare(primaryIds.indexOf(left.getId()), primaryIds.indexOf(right.getId())));
        }
        if (!primaryIds.isEmpty() && Integer.valueOf(3).equals(coupon.getUseType())) {
            categories = categoryMapper.selectBatchIds(primaryIds);
            categories.sort((left, right) -> Integer.compare(primaryIds.indexOf(left.getId()), primaryIds.indexOf(right.getId())));
        }
        return new StoreCouponInfoResponse(toSaveRequest(coupon), products, categories);
    }

    @Transactional
    public boolean save(StoreCouponSaveRequest request) {
        validateSaveRequest(request);
        StoreCoupon existing = null;
        if (request.getId() != null && request.getId() > 0) {
            existing = storeCouponMapper.selectById(request.getId());
            if (existing == null || Boolean.TRUE.equals(existing.getIsDel())) {
                throw new IllegalArgumentException("优惠券不存在");
            }
        }
        StoreCoupon coupon = buildCouponForSave(request, existing);
        if (existing == null) {
            return storeCouponMapper.insert(coupon) > 0;
        }
        coupon.setId(existing.getId());
        return storeCouponMapper.updateCoupon(coupon) > 0;
    }

    public boolean updateStatus(Integer id, Boolean status) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("优惠券ID不能为空");
        }
        StoreCoupon coupon = storeCouponMapper.selectById(id);
        if (coupon == null || Boolean.TRUE.equals(coupon.getIsDel())) {
            throw new IllegalArgumentException("优惠券不存在");
        }
        StoreCoupon update = new StoreCoupon();
        update.setId(id);
        update.setStatus(Boolean.TRUE.equals(status));
        return storeCouponMapper.updateById(update) > 0;
    }

    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("优惠券ID不能为空");
        }
        StoreCoupon coupon = storeCouponMapper.selectById(id);
        if (coupon == null || Boolean.TRUE.equals(coupon.getIsDel())) {
            throw new IllegalArgumentException("优惠券不存在");
        }
        StoreCoupon update = new StoreCoupon();
        update.setId(id);
        update.setIsDel(true);
        return storeCouponMapper.updateById(update) > 0;
    }

    @Transactional
    public boolean receive(StoreCouponReceiveRequest request) {
        if (request == null || request.getCouponId() == null || request.getCouponId() <= 0) {
            throw new IllegalArgumentException("优惠券id不能为空");
        }
        if (!StringUtils.hasText(request.getUid())) {
            throw new IllegalArgumentException("领取人不能为空");
        }
        StoreCoupon coupon = getReceiveCoupon(request.getCouponId());
        List<Integer> uidList = parseIdList(request.getUid());
        if (uidList.isEmpty()) {
            throw new IllegalArgumentException("请选择用户！");
        }
        if (Boolean.TRUE.equals(coupon.getIsLimited()) && coupon.getLastTotal() < uidList.size()) {
            throw new IllegalArgumentException("当前剩余数量不够领取！");
        }
        filterReceiveUserInUid(coupon.getId(), uidList);
        if (uidList.isEmpty()) {
            throw new IllegalArgumentException("当前用户已经领取过此优惠券了！");
        }

        LocalDateTime startTime = coupon.getUseStartTime();
        LocalDateTime endTime = coupon.getUseEndTime();
        if (!Boolean.TRUE.equals(coupon.getIsFixedTime())) {
            startTime = LocalDateTime.now();
            endTime = startTime.plusDays(coupon.getDay() == null ? 0 : coupon.getDay());
        }

        for (Integer uid : uidList) {
            StoreCouponUser couponUser = new StoreCouponUser();
            couponUser.setCouponId(coupon.getId());
            couponUser.setCid(0);
            couponUser.setUid(uid);
            couponUser.setName(coupon.getName());
            couponUser.setMoney(coupon.getMoney());
            couponUser.setMinPrice(coupon.getMinPrice());
            couponUser.setType("send");
            couponUser.setStatus(0);
            couponUser.setStartTime(startTime);
            couponUser.setEndTime(endTime);
            couponUser.setUseType(coupon.getUseType());
            if (coupon.getUseType() != null && coupon.getUseType() > 1) {
                couponUser.setPrimaryKey(coupon.getPrimaryKey());
            }
            storeCouponUserMapper.insert(couponUser);
        }

        StoreCoupon update = new StoreCoupon();
        update.setId(coupon.getId());
        update.setLastTotal(coupon.getLastTotal() - uidList.size());
        update.setUpdateTime(LocalDateTime.now());
        return storeCouponMapper.updateById(update) > 0;
    }

    private QueryWrapper<StoreCoupon> buildListQuery(StoreCouponSearchRequest request) {
        QueryWrapper<StoreCoupon> query = new QueryWrapper<>();
        query.eq("is_del", 0);
        if (request.getType() != null) {
            query.eq("type", request.getType());
        }
        if (request.getUseType() != null) {
            query.eq("use_type", request.getUseType());
        }
        if (request.getStatus() != null) {
            query.eq("status", request.getStatus() ? 1 : 0);
        }
        String name = StringUtils.hasText(request.getName()) ? request.getName() : request.getKeywords();
        if (StringUtils.hasText(name)) {
            query.like("name", name.trim());
        }
        query.orderByDesc("sort").orderByDesc("id");
        return query;
    }

    private StoreCoupon getReceiveCoupon(Integer id) {
        StoreCoupon coupon = storeCouponMapper.selectById(id);
        if (coupon == null || Boolean.TRUE.equals(coupon.getIsDel()) || !Boolean.TRUE.equals(coupon.getStatus())) {
            throw new IllegalArgumentException("优惠券信息不存在或者已失效！");
        }
        if (coupon.getReceiveEndTime() != null && coupon.getReceiveEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("已超过优惠券领取最后期限！");
        }
        if (Boolean.TRUE.equals(coupon.getIsLimited()) && coupon.getLastTotal() < 1) {
            throw new IllegalArgumentException("此优惠券已经被领完了！");
        }
        return coupon;
    }

    private void filterReceiveUserInUid(Integer couponId, List<Integer> uidList) {
        QueryWrapper<StoreCouponUser> query = new QueryWrapper<>();
        query.eq("coupon_id", couponId);
        query.in("uid", uidList);
        Set<Integer> receivedUidSet = storeCouponUserMapper.selectList(query).stream()
                .map(StoreCouponUser::getUid)
                .collect(Collectors.toCollection(HashSet::new));
        uidList.removeIf(receivedUidSet::contains);
    }

    private StoreCouponSaveRequest toSaveRequest(StoreCoupon coupon) {
        StoreCouponSaveRequest request = new StoreCouponSaveRequest();
        request.setId(coupon.getId());
        request.setName(coupon.getName());
        request.setMoney(coupon.getMoney());
        request.setIsLimited(coupon.getIsLimited());
        request.setTotal(coupon.getTotal());
        request.setUseType(coupon.getUseType());
        request.setPrimaryKey(coupon.getPrimaryKey());
        request.setMinPrice(coupon.getMinPrice());
        request.setIsForever(coupon.getReceiveEndTime() != null);
        request.setReceiveStartTime(formatDateTime(coupon.getReceiveStartTime()));
        request.setReceiveEndTime(formatDateTime(coupon.getReceiveEndTime()));
        request.setIsFixedTime(coupon.getIsFixedTime());
        request.setUseStartTime(formatDateTime(coupon.getUseStartTime()));
        request.setUseEndTime(formatDateTime(coupon.getUseEndTime()));
        request.setDay(coupon.getDay());
        request.setType(coupon.getType());
        request.setSort(coupon.getSort());
        request.setStatus(coupon.getStatus());
        return request;
    }

    private StoreCoupon buildCouponForSave(StoreCouponSaveRequest request, StoreCoupon existing) {
        StoreCoupon coupon = new StoreCoupon();
        coupon.setName(request.getName().trim());
        coupon.setMoney(request.getMoney());
        coupon.setIsLimited(Boolean.TRUE.equals(request.getIsLimited()));
        coupon.setTotal(Boolean.TRUE.equals(request.getIsLimited()) ? request.getTotal() : 0);
        coupon.setLastTotal(nextLastTotal(request, existing));
        coupon.setUseType(request.getUseType());
        coupon.setPrimaryKey(StringUtils.hasText(request.getPrimaryKey()) ? request.getPrimaryKey().trim() : "");
        coupon.setMinPrice(request.getMinPrice() == null ? BigDecimal.ZERO : request.getMinPrice());
        coupon.setIsFixedTime(Boolean.TRUE.equals(request.getIsFixedTime()));
        coupon.setType(request.getType());
        coupon.setSort(request.getSort());
        coupon.setStatus(Boolean.TRUE.equals(request.getStatus()));
        coupon.setIsDel(false);

        if (Boolean.TRUE.equals(request.getIsForever())) {
            coupon.setReceiveStartTime(parseDateTime(request.getReceiveStartTime(), "请选择领取时间范围！"));
            coupon.setReceiveEndTime(parseDateTime(request.getReceiveEndTime(), "请选择领取时间范围！"));
            if (!coupon.getReceiveStartTime().isBefore(coupon.getReceiveEndTime())) {
                throw new IllegalArgumentException("请选择正确的领取时间范围！");
            }
        } else {
            coupon.setReceiveStartTime(existing == null || existing.getReceiveStartTime() == null ? LocalDateTime.now() : existing.getReceiveStartTime());
            coupon.setReceiveEndTime(null);
        }

        if (Boolean.TRUE.equals(request.getIsFixedTime())) {
            coupon.setUseStartTime(parseDateTime(request.getUseStartTime(), "请选择使用有效期限"));
            coupon.setUseEndTime(parseDateTime(request.getUseEndTime(), "请选择使用有效期限"));
            if (!coupon.getUseStartTime().isBefore(coupon.getUseEndTime())) {
                throw new IllegalArgumentException("请选择正确的使用有效期限！");
            }
            coupon.setDay(null);
        } else {
            coupon.setDay(request.getDay());
            coupon.setUseStartTime(null);
            coupon.setUseEndTime(null);
        }
        return coupon;
    }

    private Integer nextLastTotal(StoreCouponSaveRequest request, StoreCoupon existing) {
        if (!Boolean.TRUE.equals(request.getIsLimited())) {
            return 0;
        }
        int total = request.getTotal() == null ? 0 : request.getTotal();
        if (existing == null || !Boolean.TRUE.equals(existing.getIsLimited())) {
            return total;
        }
        int oldTotal = existing.getTotal() == null ? 0 : existing.getTotal();
        int oldLastTotal = existing.getLastTotal() == null ? 0 : existing.getLastTotal();
        int used = Math.max(0, oldTotal - oldLastTotal);
        return Math.max(0, total - used);
    }

    private void validateSaveRequest(StoreCouponSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请填写优惠券名称");
        }
        if (request.getName().trim().length() > 64) {
            throw new IllegalArgumentException("优惠券名称长度不能超过64个字符");
        }
        if (request.getMoney() == null || request.getMoney().compareTo(BigDecimal.ZERO) <= 0
                || request.getMoney().compareTo(new BigDecimal("99999.99")) > 0) {
            throw new IllegalArgumentException("优惠券面值不能大于99999.99");
        }
        if (request.getUseType() == null || request.getUseType() < 1 || request.getUseType() > 3) {
            throw new IllegalArgumentException("请选择优惠券使用类型");
        }
        if (request.getUseType() > 1 && !StringUtils.hasText(request.getPrimaryKey())) {
            throw new IllegalArgumentException("请选择商品/分类！");
        }
        if (request.getMinPrice() != null && request.getMinPrice().compareTo(new BigDecimal("99999.99")) > 0) {
            throw new IllegalArgumentException("最低消费 面值不能大于99999.99");
        }
        if (request.getIsLimited() == null) {
            throw new IllegalArgumentException("请设置是否限量");
        }
        if (Boolean.TRUE.equals(request.getIsLimited()) && (request.getTotal() == null || request.getTotal() <= 0)) {
            throw new IllegalArgumentException("请输入数量！");
        }
        if (request.getIsForever() == null) {
            throw new IllegalArgumentException("请选择领取是否限时");
        }
        if (request.getIsFixedTime() == null) {
            throw new IllegalArgumentException("请设置是否固定使用时间");
        }
        if (!Boolean.TRUE.equals(request.getIsFixedTime()) && (request.getDay() == null || request.getDay() <= 0)) {
            throw new IllegalArgumentException("请输入天数！");
        }
        if (request.getDay() != null && request.getDay() > 999) {
            throw new IllegalArgumentException("天数不能超过999天");
        }
        if (request.getType() == null || request.getType() < 1 || request.getType() > 3) {
            throw new IllegalArgumentException("请选择优惠券领取方式");
        }
        if (request.getSort() == null || request.getSort() < 0) {
            throw new IllegalArgumentException("排序不能为空");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("优惠券状态不能为空");
        }
    }

    private List<Integer> parseIdList(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>();
        for (String item : value.split(",")) {
            if (StringUtils.hasText(item)) {
                try {
                    ids.add(Integer.valueOf(item.trim()));
                } catch (NumberFormatException ignored) {
                    // Old data can contain loose comma values; invalid fragments are ignored like empty fragments.
                }
            }
        }
        return ids;
    }

    private LocalDateTime parseDateTime(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        String normalized = value.trim().replace('T', ' ');
        if (normalized.length() > 19) {
            normalized = normalized.substring(0, 19);
        }
        try {
            return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(message);
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    private QueryWrapper<StoreCoupon> buildSendQuery(StoreCouponSearchRequest request) {
        QueryWrapper<StoreCoupon> query = new QueryWrapper<>();
        query.select("id", "name", "money", "min_price", "use_start_time", "use_end_time",
                "is_fixed_time", "day", "is_limited", "last_total", "type", "status", "is_del");
        query.eq("is_del", 0);
        if (request.getType() != null) {
            query.eq("type", request.getType());
        }
        query.eq("status", 1);
        if (StringUtils.hasText(request.getKeywords())) {
            query.like("name", request.getKeywords().trim());
        }
        query.and(wrapper -> wrapper.eq("is_limited", 0).or().ge("last_total", 0));
        query.and(wrapper -> wrapper.isNull("receive_end_time").or().gt("receive_end_time", LocalDateTime.now()));
        query.orderByDesc("sort").orderByDesc("id");
        return query;
    }

    private QueryWrapper<StoreCouponUser> buildUserQuery(StoreCouponUserSearchRequest request) {
        QueryWrapper<StoreCouponUser> query = new QueryWrapper<>();
        if (StringUtils.hasText(request.getName())) {
            query.like("name", request.getName().trim());
        }
        if (request.getUid() != null && request.getUid() > 0) {
            query.eq("uid", request.getUid());
        }
        if (request.getStatus() != null) {
            query.eq("status", request.getStatus());
        }
        if (request.getCouponId() != null) {
            query.eq("coupon_id", request.getCouponId());
        }
        if (request.getMinPrice() != null) {
            query.eq("min_price", request.getMinPrice());
        }
        query.orderByDesc("id");
        return query;
    }

    private Map<Integer, User> loadUsers(List<StoreCouponUser> records) {
        List<Integer> uids = records.stream()
                .map(StoreCouponUser::getUid)
                .filter(uid -> uid != null && uid > 0)
                .distinct()
                .toList();
        if (uids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(uids).stream()
                .collect(Collectors.toMap(User::getUid, Function.identity(), (left, right) -> left));
    }

    private StoreCouponUserResponse toCouponUserResponse(StoreCouponUser record, User user) {
        StoreCouponUserResponse response = new StoreCouponUserResponse();
        response.setId(record.getId());
        response.setCouponId(record.getCouponId());
        response.setCid(record.getCid());
        response.setUid(record.getUid());
        response.setName(record.getName());
        response.setMoney(record.getMoney());
        response.setMinPrice(record.getMinPrice());
        response.setType(record.getType());
        response.setStatus(record.getStatus());
        response.setCreateTime(record.getCreateTime());
        response.setUpdateTime(record.getUpdateTime());
        response.setStartTime(record.getStartTime());
        response.setEndTime(record.getEndTime());
        response.setUseTime(record.getUseTime());
        response.setUseType(record.getUseType());
        response.setPrimaryKey(record.getPrimaryKey());
        response.setUseStartTimeStr(formatDate(record.getStartTime()));
        response.setUseEndTimeStr(formatDate(record.getEndTime()));
        response.setIsValid(Integer.valueOf(0).equals(record.getStatus()));
        response.setValidStr(toValidStr(record));
        if (user != null) {
            response.setNickname(user.getNickname());
            response.setAvatar(user.getAvatar());
        }
        return response;
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "" : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String toValidStr(StoreCouponUser record) {
        if (Integer.valueOf(1).equals(record.getStatus())) {
            return "unusable";
        }
        if (Integer.valueOf(2).equals(record.getStatus())) {
            return "overdue";
        }
        LocalDateTime now = LocalDateTime.now();
        if (record.getStartTime() != null && record.getStartTime().isAfter(now)) {
            return "notStart";
        }
        if (record.getEndTime() != null && record.getEndTime().isBefore(now)) {
            return "overdue";
        }
        return "usable";
    }
}
