package com.jsy.crmeb.modern.service.front;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.config.CrmebRuntimeProperties;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.admin.mapper.LegacyLoginUiMapper;
import com.jsy.crmeb.modern.service.coupon.entity.StoreCoupon;
import com.jsy.crmeb.modern.service.coupon.entity.StoreCouponUser;
import com.jsy.crmeb.modern.service.coupon.mapper.StoreCouponMapper;
import com.jsy.crmeb.modern.service.coupon.mapper.StoreCouponUserMapper;
import com.jsy.crmeb.modern.service.product.ProductCatalogService;
import com.jsy.crmeb.modern.service.product.dto.ProductAttrValueResponse;
import com.jsy.crmeb.modern.service.product.dto.CategoryTreeResponse;
import com.jsy.crmeb.modern.service.product.entity.StoreProduct;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductAttrMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductAttrValueMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductCouponMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductDescriptionMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductRelationMapper;
import com.jsy.crmeb.modern.service.front.mapper.FrontProductReplyMapper;
import com.jsy.crmeb.modern.service.front.mapper.FrontUserCenterMapper;
import com.jsy.crmeb.modern.service.system.entity.SystemGroupData;
import com.jsy.crmeb.modern.service.system.mapper.SystemGroupDataMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FrontProductService {
    private static final int GID_INDEX_BANNER = 48;
    private static final int GID_INDEX_TABLE = 70;

    private final StoreProductMapper productMapper;
    private final ProductCatalogService catalogService;
    private final StoreProductAttrMapper attrMapper;
    private final StoreProductAttrValueMapper attrValueMapper;
    private final StoreProductDescriptionMapper descriptionMapper;
    private final StoreProductRelationMapper relationMapper;
    private final StoreProductCouponMapper productCouponMapper;
    private final StoreCouponMapper couponMapper;
    private final StoreCouponUserMapper couponUserMapper;
    private final LegacyLoginUiMapper configMapper;
    private final CrmebRuntimeProperties runtimeProperties;
    private final FrontProductReplyMapper replyMapper;
    private final FrontUserCenterMapper userCenterMapper;
    private final SystemGroupDataMapper groupDataMapper;
    private final ObjectMapper objectMapper;

    public FrontProductService(
            StoreProductMapper productMapper,
            ProductCatalogService catalogService,
            StoreProductAttrMapper attrMapper,
            StoreProductAttrValueMapper attrValueMapper,
            StoreProductDescriptionMapper descriptionMapper,
            StoreProductRelationMapper relationMapper,
            StoreProductCouponMapper productCouponMapper,
            StoreCouponMapper couponMapper,
            StoreCouponUserMapper couponUserMapper,
            LegacyLoginUiMapper configMapper,
            CrmebRuntimeProperties runtimeProperties,
            FrontProductReplyMapper replyMapper,
            FrontUserCenterMapper userCenterMapper,
            SystemGroupDataMapper groupDataMapper,
            ObjectMapper objectMapper) {
        this.productMapper = productMapper;
        this.catalogService = catalogService;
        this.attrMapper = attrMapper;
        this.attrValueMapper = attrValueMapper;
        this.descriptionMapper = descriptionMapper;
        this.relationMapper = relationMapper;
        this.productCouponMapper = productCouponMapper;
        this.couponMapper = couponMapper;
        this.couponUserMapper = couponUserMapper;
        this.configMapper = configMapper;
        this.runtimeProperties = runtimeProperties;
        this.replyMapper = replyMapper;
        this.userCenterMapper = userCenterMapper;
        this.groupDataMapper = groupDataMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> index() {
        List<Map<String, Object>> hotProducts = productPage(1, 8, 2, null, null, null, null, null).getList();
        List<Map<String, Object>> bestProducts = productPage(1, 8, 1, null, null, null, null, null).getList();
        List<Map<String, Object>> indexTable = indexTableItems();
        List<Map<String, Object>> banner = indexBannerItems();

        Map<String, Object> response = new HashMap<>();
        response.put("banner", banner);
        response.put("menus", menuItems());
        response.put("roll", List.of());
        response.put("logoUrl", normalizeAsset(configMapper.selectConfigValue("site_logo")));
        response.put("subscribe", false);
        response.put("indexTable", indexTable);
        response.put("explosiveMoney", indexTable.isEmpty() ? hotProducts : indexTable);
        response.put("bastBanner", bestProducts);
        response.put("yzfUrl", configMapper.selectConfigValue("yzf_url"));
        response.put("categoryPageConfig", defaultString(configMapper.selectConfigValue("category_page_config"), "1"));
        response.put("isShowCategory", defaultString(configMapper.selectConfigValue("is_show_category"), "1"));
        response.put("consumerHotline", normalizeCustomerHotline(configMapper.selectConfigValue("consumer_hotline")));
        response.put("telephoneServiceSwitch", defaultString(configMapper.selectConfigValue("telephone_service_switch"), "0"));
        response.put("homePageSaleListStyle", defaultString(
                configMapper.selectConfigValue("homePageSaleListStyle"),
                defaultString(configMapper.selectConfigValue("home_page_sale_list_style"), "1")));
        response.put("wxChatIndependent", defaultString(configMapper.selectConfigValue("wx_chat_independent"), "0"));
        response.put("recommend", productPage(1, 12, null, null, null, null, null, null).getList());
        return response;
    }

    public Map<String, Object> customerServiceConfig() {
        Map<String, Object> response = new HashMap<>();
        String chatUrl = defaultString(configMapper.selectConfigValue("yzf_url"), "");
        String consumerHotline = normalizeCustomerHotline(configMapper.selectConfigValue("consumer_hotline"));
        String telephoneServiceSwitch = defaultString(configMapper.selectConfigValue("telephone_service_switch"), "0");
        String wxChatIndependent = defaultString(configMapper.selectConfigValue("wx_chat_independent"), "0");
        response.put("yzfUrl", chatUrl);
        response.put("chatUrl", chatUrl);
        response.put("consumerHotline", consumerHotline);
        response.put("telephoneServiceSwitch", telephoneServiceSwitch);
        response.put("telephone_service_switch", telephoneServiceSwitch);
        response.put("wxChatIndependent", wxChatIndependent);
        response.put("wx_chat_independent", wxChatIndependent);
        response.put("serviceMode", isOpen(telephoneServiceSwitch) ? "phone" : "online");
        response.put("safeExternal", true);
        return response;
    }

    private String normalizeCustomerHotline(String value) {
        String phone = defaultString(value, "").trim();
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 10 || digits.matches("(\\d)\\1+") || digits.matches("1{6,}")) {
            return "";
        }
        return phone;
    }

    public PageResponse<Map<String, Object>> indexProduct(Integer type, int page, int limit) {
        return productPage(page, limit, type, null, null, null, null, null);
    }

    public List<CategoryTreeResponse> category() {
        return catalogService.getTree(1, 1, null);
    }

    public PageResponse<Map<String, Object>> products(
            int page,
            int limit,
            String keyword,
            String cid,
            String cateId,
            String priceOrder,
            String salesOrder,
            Boolean news) {
        return productPage(page, limit, null, keyword, defaultString(cid, cateId), priceOrder, salesOrder, news);
    }

    public Map<String, Object> detail(Integer id) {
        return detail(id, null);
    }

    @Transactional
    public Map<String, Object> detail(Integer id, Integer uid) {
        StoreProduct product = productMapper.selectById(id);
        if (product == null
                || !Integer.valueOf(1).equals(product.getIsShow())
                || Integer.valueOf(1).equals(product.getIsDel())
                || Integer.valueOf(1).equals(product.getIsRecycle())) {
            throw new IllegalArgumentException("商品不存在或已下架");
        }
        productMapper.incrementBrowse(id);
        if (uid != null && uid > 0) {
            userCenterMapper.insertVisitRecord(LocalDate.now().toString(), uid, 2);
        }

        Map<String, Object> productInfo = productMap(product, true);
        productInfo.put("browse", product.getBrowse() == null ? 1 : product.getBrowse() + 1);
        productInfo.put("sliderImage", sliderImages(product.getSliderImage(), product.getImage()));
        productInfo.put("content", normalizeHtmlAssets(descriptionMapper.selectDescription(id, 0)));
        productInfo.put("cateValues", catalogService.getCategoryNames(product.getCateId()));
        productInfo.putAll(replyConfig(id));

        List<ProductAttrValueResponse> attrValues = ensureNormalAttrValues(product);
        Map<String, Object> productValue = new HashMap<>();
        for (ProductAttrValueResponse value : attrValues) {
            value.setImage(normalizeAsset(value.getImage()));
            productValue.put(value.getSuk(), value);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("productAttr", attrMapper.selectByProductIdAndType(id, 0));
        response.put("productValue", productValue);
        response.put("priceName", "");
        response.put("activityAllH5", activityItems(product));
        response.put("productInfo", productInfo);
        response.put("userCollect", uid != null && isCollected(uid, id));
        List<Map<String, Object>> coupons = productCoupons(id, uid);
        response.put("defaultCoupon", coupons);
        response.put("couponList", coupons);
        response.put("coupons", coupons);
        List<Map<String, Object>> goodList = recommendProducts(id);
        response.put("goodList", goodList);
        response.put("good_list", goodList);
        response.put("recommend", goodList);
        List<Map<String, Object>> firstReplies = firstReplies(id);
        response.put("reply", firstReplies);
        response.put("replyList", firstReplies);
        return response;
    }

    private List<ProductAttrValueResponse> ensureNormalAttrValues(StoreProduct product) {
        List<ProductAttrValueResponse> attrValues = attrValueMapper.selectByProductIdAndType(product.getId(), 0);
        if (!attrValues.isEmpty()) {
            return attrValues;
        }
        attrValueMapper.insertAttrValue(
                product.getId(),
                "默认",
                Math.max(0, product.getStock() == null ? 0 : product.getStock()),
                product.getPrice(),
                product.getImage(),
                "p" + product.getId(),
                product.getCost(),
                product.getBarCode(),
                product.getOtPrice(),
                0,
                "[{\"attr\":\"默认\",\"value\":\"默认\"}]");
        return attrValueMapper.selectByProductIdAndType(product.getId(), 0);
    }

    public PageResponse<Map<String, Object>> replyList(Integer productId, Integer type, int page, int limit) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        List<Map<String, Object>> list = replyMapper.selectReplyList(productId, type, safeLimit, offset).stream()
                .map(this::normalizeReply)
                .toList();
        return new PageResponse<>(safePage, safeLimit, replyMapper.countReplyList(productId, type), list);
    }

    public Map<String, Object> replyConfig(Integer productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        Map<String, Object> raw = replyMapper.countReplyConfig(productId);
        long sumCount = longValue(raw == null ? null : raw.get("sumCount"));
        long goodCount = longValue(raw == null ? null : raw.get("goodCount"));
        long inCount = longValue(raw == null ? null : raw.get("inCount"));
        long poorCount = longValue(raw == null ? null : raw.get("poorCount"));
        double scoreSum = doubleValue(raw == null ? null : raw.get("scoreSum"));
        double replyChance = sumCount == 0 ? 1D : (double) goodCount / (double) sumCount;
        double replyStar = sumCount == 0 ? 5D : scoreSum / (double) sumCount / 2D;
        Map<String, Object> response = new HashMap<>();
        response.put("sumCount", sumCount);
        response.put("goodCount", goodCount);
        response.put("inCount", inCount);
        response.put("poorCount", poorCount);
        response.put("replyChance", replyChance);
        response.put("replyStar", Math.round(replyStar * 10D) / 10D);
        response.put("replyNum", sumCount);
        response.put("positiveRatio", Math.round(replyChance * 100D));
        return response;
    }

    public PageResponse<Map<String, Object>> collectList(Integer uid, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        int offset = (safePage - 1) * safeLimit;
        List<Map<String, Object>> list = relationMapper.selectUserCollectList(uid, offset, safeLimit).stream()
                .map(this::normalizeCollectItem)
                .toList();
        return new PageResponse<>(safePage, safeLimit, relationMapper.countUserCollectList(uid), list);
    }

    @Transactional
    public void addCollect(Integer uid, Map<String, Object> body) {
        Integer productId = intValue(body, "id", "productId", "product_id");
        String category = defaultString(stringValue(body, "category"), "product");
        saveCollect(uid, productId, category);
    }

    @Transactional
    public void addCollectAll(Integer uid, Map<String, Object> body) {
        List<Integer> productIds = intList(body == null ? null : body.getOrDefault("id", body.get("productId")));
        if (productIds.isEmpty()) {
            throw new IllegalArgumentException("请选择产品");
        }
        String category = defaultString(stringValue(body, "category"), "product");
        for (Integer productId : productIds) {
            saveCollect(uid, productId, category);
        }
    }

    public void deleteCollect(Integer uid, Map<String, Object> body) {
        List<Integer> ids = intList(body == null ? null : body.get("ids"));
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("收藏id不能为空");
        }
        if (relationMapper.deleteByUidAndIds(uid, ids) <= 0) {
            throw new IllegalArgumentException("收藏记录不存在");
        }
    }

    public void cancelCollectByProduct(Integer uid, Integer productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("请选择产品");
        }
        if (relationMapper.deleteByUidAndProductIdAndType(uid, productId, "collect") <= 0) {
            throw new IllegalArgumentException("收藏记录不存在");
        }
    }

    public String imageDomain() {
        if (StringUtils.hasText(runtimeProperties.getSiteUrl())) {
            return runtimeProperties.getSiteUrl().replaceAll("/+$", "") + "/";
        }
        return "/";
    }

    public List<Map<String, Object>> hotKeywords() {
        return List.of(
                keyword("新品"),
                keyword("热卖"),
                keyword("香水"),
                keyword("运动"),
                keyword("连衣裙"));
    }

    private PageResponse<Map<String, Object>> productPage(
            int page,
            int limit,
            Integer type,
            String keyword,
            String cid,
            String priceOrder,
            String salesOrder,
            Boolean news) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        QueryWrapper<StoreProduct> query = visibleQuery();
        applyType(query, type);
        if (Boolean.TRUE.equals(news)) {
            query.eq("is_new", 1);
        }
        if (StringUtils.hasText(keyword)) {
            String cleanKeyword = keyword.trim();
            query.and(wrapper -> wrapper
                    .eq("id", cleanKeyword)
                    .or().like("store_name", cleanKeyword)
                    .or().like("keyword", cleanKeyword));
        }
        if (StringUtils.hasText(cid)) {
            List<String> categoryIds = Arrays.stream(cid.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();
            if (!categoryIds.isEmpty()) {
                query.and(wrapper -> {
                    boolean[] first = {true};
                    for (String categoryId : categoryIds) {
                        if (first[0]) {
                            wrapper.apply("FIND_IN_SET({0}, cate_id)", categoryId);
                            first[0] = false;
                        } else {
                            wrapper.or().apply("FIND_IN_SET({0}, cate_id)", categoryId);
                        }
                    }
                });
            }
        }
        applyOrder(query, priceOrder, salesOrder);
        Page<StoreProduct> productPage = productMapper.selectPage(new Page<>(safePage, safeLimit), query);
        List<Map<String, Object>> list = productPage.getRecords().stream().map(product -> productMap(product, false)).toList();
        return new PageResponse<>(safePage, safeLimit, productPage.getTotal(), list);
    }

    private QueryWrapper<StoreProduct> visibleQuery() {
        return new QueryWrapper<StoreProduct>()
                .eq("is_show", 1)
                .eq("is_recycle", 0)
                .eq("is_del", 0);
    }

    private List<Map<String, Object>> recommendProducts(Integer productId) {
        return productPage(1, 13, null, null, null, null, "desc", null)
                .getList()
                .stream()
                .filter(item -> !String.valueOf(productId).equals(String.valueOf(item.get("id"))))
                .limit(12)
                .toList();
    }

    private List<Map<String, Object>> firstReplies(Integer productId) {
        return replyMapper.selectReplyList(productId, 0, 1, 0).stream()
                .map(this::normalizeReply)
                .toList();
    }

    private List<Map<String, Object>> indexTableItems() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SystemGroupData data : groupDataMapper.selectByGid(GID_INDEX_TABLE)) {
            if (!Boolean.TRUE.equals(data.getStatus())) {
                continue;
            }
            Map<String, Object> item = groupDataItem(data);
            if (!item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    private List<Map<String, Object>> indexBannerItems() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SystemGroupData data : groupDataMapper.selectByGid(GID_INDEX_BANNER)) {
            if (!Boolean.TRUE.equals(data.getStatus())) {
                continue;
            }
            Map<String, Object> item = groupDataItem(data);
            String image = stringValue(item.get("pic"));
            if (!StringUtils.hasText(image)) {
                image = stringValue(item.get("image"));
            }
            if (!StringUtils.hasText(image)) {
                continue;
            }
            item.put("pic", image);
            item.put("image", image);
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> groupDataItem(SystemGroupData data) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", data.getId());
        item.put("gid", data.getGid());
        item.put("sort", data.getSort());
        item.put("status", Boolean.TRUE.equals(data.getStatus()));
        try {
            JsonNode root = objectMapper.readTree(data.getValue());
            if (!root.path("id").isMissingNode()) {
                item.put("tempid", root.path("id").asInt());
            }
            JsonNode fields = root.path("fields");
            if (fields.isArray()) {
                for (JsonNode field : fields) {
                    String name = field.path("name").asText("");
                    if (StringUtils.hasText(name)) {
                        item.put(name, normalizeAsset(field.path("value").asText("")));
                    }
                }
            }
        } catch (JsonProcessingException ignored) {
            return Map.of();
        }
        return item;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private void applyType(QueryWrapper<StoreProduct> query, Integer type) {
        if (type == null) {
            return;
        }
        switch (type) {
            case 1 -> query.eq("is_best", 1);
            case 2 -> query.eq("is_hot", 1);
            case 3 -> query.eq("is_new", 1);
            case 4 -> query.eq("is_benefit", 1);
            default -> {
            }
        }
    }

    private void applyOrder(QueryWrapper<StoreProduct> query, String priceOrder, String salesOrder) {
        if ("desc".equalsIgnoreCase(salesOrder)) {
            query.last("order by (sales + ficti) desc, sort desc, id desc");
        } else if ("asc".equalsIgnoreCase(salesOrder)) {
            query.last("order by (sales + ficti) asc, sort asc, id asc");
        } else if ("desc".equalsIgnoreCase(priceOrder)) {
            query.orderByDesc("price").orderByDesc("sort").orderByDesc("id");
        } else if ("asc".equalsIgnoreCase(priceOrder)) {
            query.orderByAsc("price").orderByDesc("sort").orderByDesc("id");
        } else {
            query.orderByDesc("sort").orderByDesc("id");
        }
    }

    private Map<String, Object> productMap(StoreProduct product, boolean detail) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", product.getId());
        item.put("image", normalizeAsset(product.getImage()));
        item.put("storeName", product.getStoreName());
        item.put("price", product.getPrice());
        item.put("otPrice", product.getOtPrice());
        item.put("sales", product.getSales());
        item.put("ficti", product.getFicti());
        item.put("positiveRatio", "100");
        item.put("replyNum", 0);
        item.put("unitName", product.getUnitName());
        item.put("activity", defaultString(product.getActivity(), "0"));
        item.put("activityH5", activityItem("默认", 0));
        item.put("cartNum", 0);
        item.put("stock", product.getStock());
        item.put("flatPattern", normalizeAsset(product.getFlatPattern()));
        item.put("activityStyle", "");
        if (detail) {
            item.put("sliderImage", sliderImages(product.getSliderImage(), product.getImage()));
            item.put("store_name", product.getStoreName());
            item.put("cateId", product.getCateId());
            item.put("isPostage", Integer.valueOf(1).equals(product.getIsPostage()));
            item.put("browse", product.getBrowse());
            item.put("vipPrice", product.getVipPrice());
        }
        return item;
    }

    private void saveCollect(Integer uid, Integer productId, String category) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("请选择产品");
        }
        StoreProduct product = productMapper.selectById(productId);
        if (product == null
                || !Integer.valueOf(1).equals(product.getIsShow())
                || Integer.valueOf(1).equals(product.getIsDel())
                || Integer.valueOf(1).equals(product.getIsRecycle())) {
            throw new IllegalArgumentException("商品不存在或已下架");
        }
        relationMapper.deleteByUidAndProductIdsAndType(uid, List.of(productId), "collect", category);
        relationMapper.insertRelation(uid, productId, "collect", category);
    }

    private boolean isCollected(Integer uid, Integer productId) {
        Integer count = relationMapper.countByUidAndProductIdAndType(uid, productId, "collect", "product");
        if (count != null && count > 0) {
            return true;
        }
        Integer legacyCount = relationMapper.countByUidAndProductIdAndType(uid, productId, "collect", "store");
        return legacyCount != null && legacyCount > 0;
    }

    private Map<String, Object> normalizeCollectItem(Map<String, Object> item) {
        Map<String, Object> normalized = new HashMap<>(item);
        normalized.put("image", normalizeAsset(String.valueOf(item.getOrDefault("image", ""))));
        return normalized;
    }

    private Map<String, Object> normalizeReply(Map<String, Object> item) {
        Map<String, Object> normalized = new HashMap<>(item);
        normalized.put("avatar", normalizeAsset(String.valueOf(item.getOrDefault("avatar", ""))));
        normalized.put("pics", splitPics(item.get("pics")));
        return normalized;
    }

    private List<Map<String, Object>> productCoupons(Integer productId, Integer uid) {
        List<Integer> couponIds = productCouponMapper.selectCouponIds(productId);
        if (couponIds.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<StoreCoupon> query = new QueryWrapper<>();
        query.in("id", couponIds);
        query.eq("is_del", 0);
        query.eq("status", 1);
        query.eq("type", 1);
        query.and(wrapper -> wrapper.eq("is_limited", 0).or().gt("last_total", 0));
        query.and(wrapper -> wrapper.isNull("receive_start_time").or().le("receive_start_time", now));
        query.and(wrapper -> wrapper.isNull("receive_end_time").or().gt("receive_end_time", now));
        List<StoreCoupon> coupons = couponMapper.selectList(query);
        if (coupons.isEmpty()) {
            return List.of();
        }
        Set<Integer> receivedIds = receivedCouponIds(uid, coupons);
        Map<Integer, Integer> orderMap = new HashMap<>();
        for (int index = 0; index < couponIds.size(); index++) {
            orderMap.put(couponIds.get(index), index);
        }
        return coupons.stream()
                .sorted((left, right) -> Integer.compare(
                        orderMap.getOrDefault(left.getId(), Integer.MAX_VALUE),
                        orderMap.getOrDefault(right.getId(), Integer.MAX_VALUE)))
                .map(coupon -> couponMap(coupon, receivedIds.contains(coupon.getId())))
                .toList();
    }

    private Set<Integer> receivedCouponIds(Integer uid, List<StoreCoupon> coupons) {
        if (uid == null || coupons.isEmpty()) {
            return Set.of();
        }
        QueryWrapper<StoreCouponUser> query = new QueryWrapper<>();
        query.select("coupon_id");
        query.eq("uid", uid);
        query.in("coupon_id", coupons.stream().map(StoreCoupon::getId).toList());
        Set<Integer> ids = new LinkedHashSet<>();
        for (StoreCouponUser couponUser : couponUserMapper.selectList(query)) {
            if (couponUser.getCouponId() != null) {
                ids.add(couponUser.getCouponId());
            }
        }
        return ids;
    }

    private Map<String, Object> couponMap(StoreCoupon coupon, boolean received) {
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

    private String formatCouponDate(LocalDateTime value) {
        return value == null ? "" : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private List<String> splitPics(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return List.of();
        }
        String clean = String.valueOf(value).trim();
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        List<String> pics = new ArrayList<>();
        for (String raw : clean.split(",")) {
            String pic = raw.trim();
            if ((pic.startsWith("\"") && pic.endsWith("\"")) || (pic.startsWith("'") && pic.endsWith("'"))) {
                pic = pic.substring(1, pic.length() - 1);
            }
            if (StringUtils.hasText(pic)) {
                pics.add(normalizeAsset(pic));
            }
        }
        return pics;
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

    private double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            return Double.parseDouble(String.valueOf(value));
        }
        return 0D;
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
                return Integer.valueOf(String.valueOf(value).trim());
            }
        }
        return null;
    }

    private List<Integer> intList(Object value) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                addInt(ids, item);
            }
        } else if (value instanceof Object[] array) {
            for (Object item : array) {
                addInt(ids, item);
            }
        } else if (value != null) {
            for (String item : String.valueOf(value).split(",")) {
                addInt(ids, item);
            }
        }
        return new ArrayList<>(ids);
    }

    private void addInt(Set<Integer> ids, Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return;
        }
        Integer id = value instanceof Number number
                ? number.intValue()
                : Integer.valueOf(String.valueOf(value).trim());
        if (id > 0) {
            ids.add(id);
        }
    }

    private String stringValue(Map<String, Object> body, String name) {
        if (body == null) {
            return "";
        }
        Object value = body.get(name);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private List<Map<String, Object>> activityItems(StoreProduct product) {
        return List.of(activityItem("默认", 0));
    }

    private Map<String, Object> activityItem(String title, int type) {
        Map<String, Object> item = new HashMap<>();
        item.put("type", type);
        item.put("title", title);
        item.put("name", title);
        return item;
    }

    private List<Map<String, Object>> menuItems() {
        List<Map<String, Object>> menus = new ArrayList<>();
        String[] names = {"商品分类", "优惠券", "我的订单", "会员中心", "积分商城", "联系客服", "收藏夹", "推广中心"};
        String[] urls = {
                "/pages/goods_cate/goods_cate",
                "/pages/users/user_coupon/index",
                "/pages/users/order_list/index",
                "/pages/user/index",
                "/pages/users/user_integral/index",
                "/pages/users/kefu/index",
                "/pages/users/user_goods_collection/index",
                "/pages/promoter/user_spread_code/index"
        };
        for (int i = 0; i < names.length; i++) {
            Map<String, Object> menu = new HashMap<>();
            menu.put("id", i + 1);
            menu.put("name", names[i]);
            menu.put("pic", "");
            menu.put("url", urls[i]);
            menus.add(menu);
        }
        return menus;
    }

    private Map<String, Object> keyword(String text) {
        Map<String, Object> item = new HashMap<>();
        item.put("keyword", text);
        item.put("name", text);
        return item;
    }

    private List<String> sliderImages(String sliderImage, String fallback) {
        List<String> images = new ArrayList<>();
        if (StringUtils.hasText(sliderImage)) {
            String clean = sliderImage.trim();
            if (clean.startsWith("[") && clean.endsWith("]")) {
                clean = clean.substring(1, clean.length() - 1);
            }
            for (String raw : clean.split(",")) {
                String image = raw.trim();
                if ((image.startsWith("\"") && image.endsWith("\"")) || (image.startsWith("'") && image.endsWith("'"))) {
                    image = image.substring(1, image.length() - 1);
                }
                if (StringUtils.hasText(image)) {
                    images.add(normalizeAsset(image));
                }
            }
        }
        if (images.isEmpty() && StringUtils.hasText(fallback)) {
            images.add(normalizeAsset(fallback));
        }
        return images;
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

    private String normalizeHtmlAssets(String html) {
        if (!StringUtils.hasText(html)) {
            return html;
        }
        return html
                .replace("src=\"crmebimage/", "src=\"/crmebimage/")
                .replace("src='crmebimage/", "src='/crmebimage/")
                .replace("href=\"crmebimage/", "href=\"/crmebimage/")
                .replace("href='crmebimage/", "href='/crmebimage/")
                .replace("src=\"public/", "src=\"/public/")
                .replace("src='public/", "src='/public/")
                .replace("href=\"public/", "href=\"/public/")
                .replace("href='public/", "href='/public/");
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private boolean isOpen(String value) {
        return "open".equalsIgnoreCase(value) || "1".equals(value) || "true".equalsIgnoreCase(value);
    }
}
